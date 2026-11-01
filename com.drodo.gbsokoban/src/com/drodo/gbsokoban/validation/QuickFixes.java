package com.drodo.gbsokoban.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;

import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.Level;
import com.drodo.gbsokoban.gBSokoban.PlayerRef;
import com.drodo.gbsokoban.gBSokoban.Solid;
import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.model.GameTextures;
import com.drodo.gbsokoban.model.SymbolPool;
import com.drodo.gbsokoban.util.CellGeometry;
import com.drodo.gbsokoban.util.Palettes;

public final class QuickFixes {

	public static final String CAN_PULL_LINE = "PLAYER_CAN_PULL\n";

	private QuickFixes() {
	}

	public static String freeSymbol(EObject host) {
		Game game = EcoreUtil2.getContainerOfType(host, Game.class);
		if (game == null)
			return null;
		String name = host instanceof TileDef ? ((TileDef) host).getName() : null;
		String free = SymbolPool.firstUnusedFor(name, SymbolPool.usedIn(game));
		return free == null ? null : quoted(free);
	}

	public static String firstCharacter(String symbol) {
		String inner = symbol.replace("\"", "");
		return inner.isEmpty() ? null : quoted(inner.substring(0, 1));
	}

	public static String playerLegendLine(EObject host) {
		Game game = EcoreUtil2.getContainerOfType(host, Game.class);
		if (game == null)
			return null;
		TileDef tile = plainTileOf(game);
		String symbol = SymbolPool.firstUnusedFor(null, SymbolPool.usedIn(game));
		if (tile == null || symbol == null)
			return null;
		return quoted(symbol) + " = PLAYER ON " + tile.getName() + "\n";
	}

	public static String keepFirstPlayer(Level level, String text) {
		Game game = EcoreUtil2.getContainerOfType(level, Game.class);
		if (game == null)
			return null;
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
	}

	public static String fitTexture(Texture texture, String text) {
		int side = sideFor(texture, EcoreUtil2.getContainerOfType(texture, Game.class));
		List<String> lines = new ArrayList<>(List.of(text.split("\n", -1)));
		int header = 0;
		while (header < lines.size() && !lines.get(header).contains("USES"))
			header++;
		int split = Math.min(header + 1, lines.size());
		List<String> head = new ArrayList<>(lines.subList(0, split));
		List<String> rows = new ArrayList<>(lines.subList(split, lines.size()));
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
	}

	public static String fitPalette(String text) {
		List<String> parts = new ArrayList<>(List.of(text.trim().split("\\s+")));
		String name = parts.remove(0);
		if (parts.isEmpty())
			return null;
		while (parts.size() > Palettes.ENTRIES)
			parts.remove(parts.size() - 1);
		while (parts.size() < Palettes.ENTRIES)
			parts.add(parts.get(parts.size() - 1));
		return name + " " + String.join(" ", parts);
	}

	private static String indentOf(String line) {
		return line.substring(0, line.length() - line.stripLeading().length());
	}

	private static String unquoted(String line) {
		return line.trim().replace("\"", "");
	}

	private static String quoted(String text) {
		return "\"" + text + "\"";
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
		for (TileDef tile : game.getTiles())
			if (tile.getBehaviour() == null)
				return tile;
		for (TileDef tile : game.getTiles())
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
