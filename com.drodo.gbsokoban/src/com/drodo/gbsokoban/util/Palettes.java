package com.drodo.gbsokoban.util;

import java.util.List;

public final class Palettes {

	/** A palette register holds four entries, whatever a texture names. */
	public static final int ENTRIES = 4;

	public static final int DMG_BKG_PALETTES = 1;

	/** OBP0 and OBP1; a Color reads the index from OAM instead. */
	public static final int DMG_OBJECT_PALETTES = 2;

	public static final int CGB_BKG_PALETTES = 8;

	public static final int CGB_OBJECT_PALETTES = 8;

	public static final List<String> DEFAULT_SHADES =
			List.of("DMG_WHITE", "DMG_LITE_GRAY", "DMG_DARK_GRAY", "DMG_BLACK");

	private Palettes() {
	}
}
