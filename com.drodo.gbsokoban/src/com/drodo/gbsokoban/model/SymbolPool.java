package com.drodo.gbsokoban.model;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.TileDef;

public final class SymbolPool {

	public static final String SUGGESTIONS = "abcdefghijklmnopqrstuvwxyz#$@*+.-~%!&<>^,:;?=/";

	private SymbolPool() {
	}

	public static Set<String> usedIn(Game game) {
		Set<String> used = new HashSet<>();
		for (TileDef tile : game.getTiles())
			add(used, tile.getSymbol());
		for (LegendEntry entry : game.getLegend())
			add(used, entry.getSymbol());
		return used;
	}

	/** A letter of the name when one is free, otherwise the first free suggestion. */
	public static String firstUnusedFor(String name, Set<String> used) {
		String pool = (name == null ? "" : name) + SUGGESTIONS;
		for (char c : pool.toLowerCase(Locale.ROOT).toCharArray()) {
			String candidate = String.valueOf(c);
			if (!used.contains(candidate))
				return candidate;
		}
		return null;
	}

	private static void add(Set<String> used, String symbol) {
		if (symbol != null)
			used.add(symbol);
	}
}
