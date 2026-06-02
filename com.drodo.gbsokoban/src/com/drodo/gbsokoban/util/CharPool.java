package com.drodo.gbsokoban.util;

import java.util.HashSet;
import java.util.Set;

import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.TileDef;

/** Single-character strings already taken by the game's tiles, objects, and legend entries. */
public final class CharPool {

	private CharPool() {}

	public static Set<String> usedIn(Game game) {
		Set<String> used = new HashSet<>();
		for (TileDef tile : ModelHelpers.tilesOf(game))
			if (tile.getChar() != null) used.add(tile.getChar());
		for (ObjectDef object : ModelHelpers.objectsOf(game))
			if (object.getChar() != null) used.add(object.getChar());
		for (LegendEntry entry : ModelHelpers.legendEntriesOf(game))
			if (entry.getChar() != null) used.add(entry.getChar());
		return used;
	}

	/** First character of {@code pool} not already in {@code used}, or null if all are used. */
	public static String firstUnusedFrom(Set<String> used, String pool) {
		for (int i = 0; i < pool.length(); i++) {
			String candidate = String.valueOf(pool.charAt(i));
			if (!used.contains(candidate)) return candidate;
		}
		return null;
	}
}
