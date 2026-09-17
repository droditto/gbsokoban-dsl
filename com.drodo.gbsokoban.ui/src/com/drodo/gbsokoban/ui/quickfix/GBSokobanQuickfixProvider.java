package com.drodo.gbsokoban.ui.quickfix;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.emf.ecore.EObject;
import java.util.ArrayList;
import java.util.function.BiFunction;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.ui.editor.model.edit.IModificationContext;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.editor.model.edit.IModification;
import org.eclipse.xtext.ui.editor.quickfix.DefaultQuickfixProvider;
import org.eclipse.xtext.ui.editor.quickfix.Fix;
import org.eclipse.xtext.ui.editor.quickfix.IssueResolutionAcceptor;
import org.eclipse.xtext.validation.Issue;

import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.PlayerRef;
import com.drodo.gbsokoban.gBSokoban.Solid;
import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.model.GameTextures;
import com.drodo.gbsokoban.model.SymbolPool;
import com.drodo.gbsokoban.util.CellGeometry;
import com.drodo.gbsokoban.util.Palettes;
import com.drodo.gbsokoban.validation.GBSokobanValidator;

public class GBSokobanQuickfixProvider extends DefaultQuickfixProvider {

	@Fix(GBSokobanValidator.ISSUE_DUPLICATE_SYMBOL)
	public void replaceDuplicateSymbol(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Use an available symbol", "Pick a character nothing else uses.", null,
				(IModification) context -> {
					IXtextDocument doc = context.getXtextDocument();
					String free = doc.readOnly(resource -> {
						EObject host = resource.getEObject(issue.getUriToProblem().fragment());
						String name = host instanceof TileDef ? ((TileDef) host).getName() : "";
						return SymbolPool.firstUnusedFor(name,
								SymbolPool.usedIn((Game) resource.getContents().get(0)));
					});
					if (free != null)
						doc.replace(issue.getOffset(), issue.getLength(), "\"" + free + "\"");
				});
	}

	@Fix(GBSokobanValidator.ISSUE_SYMBOL_LENGTH)
	public void trimSymbol(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Keep the first character", "A symbol is a single character.", null,
				(IModification) context -> {
					IXtextDocument doc = context.getXtextDocument();
					String inner = doc.get(issue.getOffset(), issue.getLength()).replace("\"", "");
					if (!inner.isEmpty())
						doc.replace(issue.getOffset(), issue.getLength(), "\"" + inner.charAt(0) + "\"");
				});
	}

	@Fix(GBSokobanValidator.ISSUE_PULL_REQUIRES_CAN_PULL)
	public void allowPulling(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Enable player pull", "Add PLAYER_CAN_PULL to the game options.", null,
				(IModification) context -> context.getXtextDocument().replace(0, 0, "PLAYER_CAN_PULL\n"));
	}

	@Fix(GBSokobanValidator.ISSUE_LEGEND_NO_PLAYER)
	public void addPlayerToLegend(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Place the player", "Add a legend entry that puts the player on a plain tile.",
				null, (IModification) context -> {
					IXtextDocument doc = context.getXtextDocument();
					String line = doc.readOnly(resource -> {
						Game game = (Game) resource.getContents().get(0);
						TileDef tile = plainTileOf(game);
						String symbol = SymbolPool.firstUnusedFor(null, SymbolPool.usedIn(game));
						return tile == null || symbol == null ? null
								: "\"" + symbol + "\" = PLAYER ON " + tile.getName() + "\n";
					});
					if (line != null)
						doc.replace(issue.getOffset(), 0, line);
				});
	}

	@Fix(GBSokobanValidator.ISSUE_LEVEL_TOO_MANY_PLAYERS)
	public void keepFirstPlayer(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Keep only the first player",
				"Replace the extra players with a tile they can stand on.", null,
				(IModification) context -> rewrite(issue, context, (host, text) -> {
					Game game = EcoreUtil2.getContainerOfType(host, Game.class);
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

	@Fix(GBSokobanValidator.ISSUE_TEXTURE_SIZE)
	public void fitTextureToSize(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Fix the texture size", "Pad short rows with transparent and drop the extra ones.",
				null, (IModification) context -> rewrite(issue, context, (host, text) -> {
					if (!(host instanceof Texture))
						return null;
					int side = sideFor((Texture) host, EcoreUtil2.getContainerOfType(host, Game.class));
					List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
					int header = 0;
					while (header < lines.size() && !lines.get(header).contains("USES"))
						header++;
					List<String> head = new ArrayList<>(lines.subList(0, header + 1));
					List<String> rows = new ArrayList<>(lines.subList(header + 1, lines.size()));
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

	/** Replaces the text of the object the issue points at, computed from what is there now. */
	private static void rewrite(Issue issue, IModificationContext context,
			BiFunction<EObject, String, String> edit) throws Exception {
		IXtextDocument doc = context.getXtextDocument();
		int[] region = new int[2];
		String updated = doc.readOnly(resource -> {
			EObject host = resource.getEObject(issue.getUriToProblem().fragment());
			ICompositeNode node = host == null ? null : NodeModelUtils.findActualNodeFor(host);
			if (node == null)
				return null;
			region[0] = node.getTotalOffset();
			region[1] = node.getTotalLength();
			return edit.apply(host, node.getText());
		});
		if (updated != null)
			doc.replace(region[0], region[1], updated);
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

	@Fix(GBSokobanValidator.ISSUE_PALETTE_SIZE)
	public void fitPaletteToRegister(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Fix the palette size", "Drop the extras or repeat the last one.", null,
				(IModification) context -> rewrite(issue, context, (host, text) -> {
					List<String> parts = new ArrayList<>(List.of(text.trim().split("\\s+")));
					String name = parts.remove(0);
					if (parts.isEmpty())
						return null;
					while (parts.size() > Palettes.ENTRIES)
						parts.remove(parts.size() - 1);
					while (parts.size() < Palettes.ENTRIES)
						parts.add(parts.get(parts.size() - 1));
					return indentOf(text) + name + " " + String.join(" ", parts);
				}));
	}

	@Fix(GBSokobanValidator.ISSUE_PULL_WITHOUT_OBJECTS)
	public void stopPulling(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Drop PLAYER_CAN_PULL", "Nothing in this game can be pulled.", null,
				(IModification) context -> {
					IXtextDocument doc = context.getXtextDocument();
					deleteLineAt(doc, doc.get().indexOf("PLAYER_CAN_PULL"));
				});
	}

	@Fix(GBSokobanValidator.ISSUE_SOUND_NEVER_PLAYS)
	public void removeUnreachableSound(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Remove the sound", "Nothing in this game can trigger this event.", null,
				(IModification) context -> deleteLineAt(context.getXtextDocument(), issue.getOffset()));
	}

	/** Removes the whole line the offset falls on, line break included. */
	private static void deleteLineAt(IXtextDocument doc, int offset) throws BadLocationException {
		if (offset < 0)
			return;
		int line = doc.getLineOfOffset(offset);
		doc.replace(doc.getLineOffset(line), doc.getLineLength(line), "");
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
		for (LegendEntry entry : game.getLegend())
			if (entry.getSubject() instanceof PlayerRef && entry.getSymbol() != null)
				symbols.add(entry.getSymbol());
		return symbols;
	}

}
