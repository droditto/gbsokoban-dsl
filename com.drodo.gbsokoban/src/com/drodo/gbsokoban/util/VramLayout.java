package com.drodo.gbsokoban.util;

public final class VramLayout {

	private static final int BKG_TILES = 256;

	/** The background map is this many tiles each way, whatever the screen shows of it. */
	public static final int BKG_MAP_TILES = 32;

	/** Sprite slots 128..255 share $8800 with background ones, so sprites stop here. */
	public static final int SPRITE_TILES = 128;

	/** font_min: a space, ten digits and twenty-six capitals. */
	public static final int FONT_TILES = 37;

	private static final int CURSOR_TILES = 1;

	public static final int TILESET_BASE = FONT_TILES + CURSOR_TILES;

	public static final int BKG_TILE_BUDGET = BKG_TILES - TILESET_BASE;

	private VramLayout() {
	}
}
