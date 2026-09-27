package com.drodo.gbsokoban.ide.quickfix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.ide.editor.quickfix.AbstractDeclarativeIdeQuickfixProvider;
import org.eclipse.xtext.ide.editor.quickfix.DiagnosticResolutionAcceptor;
import org.eclipse.xtext.ide.editor.quickfix.ITextModification;
import org.eclipse.xtext.ide.editor.quickfix.QuickFix;
import org.eclipse.xtext.ide.server.Document;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.Level;
import com.drodo.gbsokoban.gBSokoban.PaletteDef;
import com.drodo.gbsokoban.gBSokoban.PlayerRef;
import com.drodo.gbsokoban.gBSokoban.Solid;
import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.model.GameTextures;
import com.drodo.gbsokoban.model.SymbolPool;
import com.drodo.gbsokoban.util.CellGeometry;
import com.drodo.gbsokoban.util.Palettes;
import com.drodo.gbsokoban.validation.GBSokobanValidator;

/** The same nine fixes as the Eclipse provider, as LSP code actions. */
public class GBSokobanIdeQuickfixProvider extends AbstractDeclarativeIdeQuickfixProvider {

	@QuickFix(GBSokobanValidator.ISSUE_DUPLICATE_SYMBOL)
	public void replaceDuplicateSymbol(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Use a free character", (ITextModification) (diagnostic, object, document) -> {
			Game game = gameOf(object);
			if (game == null)
				return none();
			String name = object instanceof TileDef ? ((TileDef) object).getName() : null;
			String free = SymbolPool.firstUnusedFor(name, SymbolPool.usedIn(game));
			return free == null ? none() : one(diagnostic.getRange(), "\"" + free + "\"");
		});
	}

	@QuickFix(GBSokobanValidator.ISSUE_SYMBOL_LENGTH)
	public void trimSymbol(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Keep only the first character", (ITextModification) (diagnostic, object, document) -> {
			String inner = document.getSubstring(diagnostic.getRange()).replace("\"", "");
			return inner.isEmpty() ? none() : one(diagnostic.getRange(), "\"" + inner.charAt(0) + "\"");
		});
	}

	@QuickFix(GBSokobanValidator.ISSUE_PULL_REQUIRES_CAN_PULL)
	public void allowPulling(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Add PLAYER_CAN_PULL", (ITextModification) (diagnostic, object, document) -> {
			Position start = new Position(0, 0);
			return one(new Range(start, start), "PLAYER_CAN_PULL\n");
		});
	}

	@QuickFix(GBSokobanValidator.ISSUE_LEGEND_NO_PLAYER)
	public void addPlayerToLegend(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Add a character for the player", (ITextModification) (diagnostic, object, document) -> {
			Game game = gameOf(object);
			if (game == null)
				return none();
			TileDef tile = plainTileOf(game);
			String symbol = SymbolPool.firstUnusedFor(null, SymbolPool.usedIn(game));
			if (tile == null || symbol == null)
				return none();
			Position start = diagnostic.getRange().getStart();
			return one(new Range(start, start), "\"" + symbol + "\" = PLAYER ON " + tile.getName() + "\n");
		});
	}

	@QuickFix(GBSokobanValidator.ISSUE_LEVEL_TOO_MANY_PLAYERS)
	public void keepFirstPlayer(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Keep only the first player", rewrite(Level.class, (host, text) -> {
			Game game = gameOf(host);
			Set<String> players = playerSymbolsOf(game);
			TileDef floor = plainTileOf(game);
			if (players.isEmpty() || floor == null || floor.getSymbol() == null)
				return null;
			char fill = floor.getSymbol().charAt(0);
			StringBuilder out = new StringBuilder(text);
			boolean quoted = false;
			boolean kept = false;
			for (int i = 0; i < out.length(); i++) {
				if (out.charAt(i) == '"') {
					quoted = !quoted;
				} else if (quoted && players.contains(String.valueOf(out.charAt(i)))) {
					if (kept)
						out.setCharAt(i, fill);
					kept = true;
				}
			}
			return out.toString();
		}));
	}

	@QuickFix(GBSokobanValidator.ISSUE_TEXTURE_SIZE)
	public void fitTextureToSize(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Resize the texture", rewrite(Texture.class, (host, text) -> {
			int side = sideFor(host, gameOf(host));
			List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
			int header = 0;
			while (header < lines.size() && !lines.get(header).contains("USES"))
				header++;
			List<String> head = new ArrayList<>(lines.subList(0, Math.min(header + 1, lines.size())));
			List<String> rows = new ArrayList<>(lines.subList(Math.min(header + 1, lines.size()), lines.size()));
			rows.removeIf(String::isBlank);
			String indent = rows.isEmpty() ? "" : indentOf(rows.get(0));
			while (rows.size() > side)
				rows.remove(rows.size() - 1);
			for (int y = 0; y < rows.size(); y++)
				rows.set(y, indent + quoted(fitRow(unquoted(rows.get(y)), side)));
			while (rows.size() < side)
				rows.add(indent + quoted(fitRow("", side)));
			head.addAll(rows);
			return String.join("\n", head);
		}));
	}

	@QuickFix(GBSokobanValidator.ISSUE_PALETTE_SIZE)
	public void fitPaletteToRegister(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Resize the palette to 4 colors", rewrite(PaletteDef.class, (host, text) -> {
			List<String> parts = new ArrayList<>(List.of(text.trim().split("\\s+")));
			String name = parts.remove(0);
			if (parts.isEmpty())
				return null;
			while (parts.size() > Palettes.ENTRIES)
				parts.remove(parts.size() - 1);
			while (parts.size() < Palettes.ENTRIES)
				parts.add(parts.get(parts.size() - 1));
			return name + " " + String.join(" ", parts);
		}));
	}

	@QuickFix(GBSokobanValidator.ISSUE_PULL_WITHOUT_OBJECTS)
	public void stopPulling(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Remove PLAYER_CAN_PULL",
				(ITextModification) (diagnostic, object, document) -> deleteLine(document,
						diagnostic.getRange().getStart().getLine()));
	}

	@QuickFix(GBSokobanValidator.ISSUE_SOUND_NEVER_PLAYS)
	public void removeUnreachableSound(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Remove the sound",
				(ITextModification) (diagnostic, object, document) -> deleteLine(document,
						diagnostic.getRange().getStart().getLine()));
	}

	/** Rewrites the host's text; getText() would include the comments above it. */
	private static <T extends EObject> ITextModification rewrite(Class<T> hostType,
			BiFunction<T, String, String> edit) {
		return (diagnostic, object, document) -> {
			T host = EcoreUtil2.getContainerOfType(object, hostType);
			ICompositeNode node = host == null ? null : NodeModelUtils.findActualNodeFor(host);
			if (node == null)
				return none();
			String updated = edit.apply(host, document.getContents().substring(node.getOffset(), node.getEndOffset()));
			if (updated == null)
				return none();
			Range range = new Range(document.getPosition(node.getOffset()), document.getPosition(node.getEndOffset()));
			return one(range, updated);
		};
	}

	private static List<TextEdit> deleteLine(Document document, int line) {
		Position start = new Position(line, 0);
		Position end = line + 1 < document.getLineCount() ? new Position(line + 1, 0)
				: new Position(line, document.getLineContent(line).length());
		return one(new Range(start, end), "");
	}

	private static List<TextEdit> one(Range range, String text) {
		return Collections.singletonList(new TextEdit(range, text));
	}

	private static List<TextEdit> none() {
		return Collections.emptyList();
	}

	private static Game gameOf(EObject object) {
		return object == null ? null : EcoreUtil2.getContainerOfType(object, Game.class);
	}

	private static String indentOf(String line) {
		return line.substring(0, line.length() - line.stripLeading().length());
	}

	private static String unquoted(String line) {
		return line.trim().replace("\"", "");
	}

	private static String quoted(String row) {
		return "\"" + row + "\"";
	}

	private static int sideFor(Texture texture, Game game) {
		int stated = game == null ? 0 : GameTextures.cellPx(game);
		if (CellGeometry.SUPPORTED_PX.contains(stated))
			return stated;
		return CellGeometry.nearestSupportedPx(texture.getRows().size());
	}

	private static String fitRow(String row, int side) {
		StringBuilder out = new StringBuilder(row.length() > side ? row.substring(0, side) : row);
		while (out.length() < side)
			out.append('.');
		return out.toString();
	}

	private static TileDef plainTileOf(Game game) {
		List<TileDef> tiles = game.getTiles();
		for (TileDef tile : tiles)
			if (tile.getBehaviour() == null)
				return tile;
		for (TileDef tile : tiles)
			if (!(tile.getBehaviour() instanceof Solid))
				return tile;
		return null;
	}

	private static Set<String> playerSymbolsOf(Game game) {
		Set<String> symbols = new HashSet<>();
		if (game == null)
			return symbols;
		for (LegendEntry entry : game.getLegend())
			if (entry.getSubject() instanceof PlayerRef && entry.getSymbol() != null)
				symbols.add(entry.getSymbol());
		return symbols;
	}
}
