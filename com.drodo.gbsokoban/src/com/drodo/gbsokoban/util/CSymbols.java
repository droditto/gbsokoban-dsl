package com.drodo.gbsokoban.util;

import java.util.Locale;

public final class CSymbols {

	/** The cell outside a level. Reserved, so no declaration can take the name back. */
	public static final String OUTSIDE = "OUTSIDE";

	private CSymbols() {
	}

	public static String tile(String name) {
		return "TILE_" + cname(name);
	}

	public static String cell(String name) {
		return "CELL_" + cname(name);
	}

	public static String onGoalCell(String name) {
		return cell(name) + "_ON_GOAL";
	}

	private static String cname(String name) {
		return name == null ? "" : name.toUpperCase(Locale.ROOT);
	}
}
