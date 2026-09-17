package com.drodo.gbsokoban.util;

import java.util.List;

public record CellGeometry(int px) {

	/** The cell sides the hardware can draw: one tile, or a 2x2 block of them. */
	public static final List<Integer> SUPPORTED_PX = List.of(8, 16);

	public static final int DEFAULT_PX = 16;

	/** Positions are carried in sub-pixels so a move can advance by less than a pixel a frame. */
	public static final int SUBPIXEL_SHIFT = 4;

	public int subPixelsPerCell() {
		return px << SUBPIXEL_SHIFT;
	}

	public int tilesPerSide() {
		return px / 8;
	}

	public int tilesPerCell() {
		return tilesPerSide() * tilesPerSide();
	}

	public int pixelShift() {
		return Integer.numberOfTrailingZeros(px);
	}

	/** The metasprite pivot sits at the cell center, so a flip mirrors around it. */
	public int halfPx() {
		return px / 2;
	}

	public static int nearestSupportedPx(int rows) {
		int nearest = SUPPORTED_PX.get(0);
		for (int px : SUPPORTED_PX)
			if (Math.abs(px - rows) < Math.abs(nearest - rows))
				nearest = px;
		return nearest;
	}

	public static String supportedPxAsText() {
		StringBuilder out = new StringBuilder();
		for (int px : SUPPORTED_PX) {
			if (out.length() > 0)
				out.append(" or ");
			out.append(px);
		}
		return out.toString();
	}

	/** 8x16 mode pairs tiles by index, so a 16px cell must use the taller sprite. */
	public boolean usesTallSprites() {
		return px == 16;
	}

	/** Hardware sprite origin (8,16), plus half a cell for the centered pivot. */
	public int spriteOffsetX() {
		return 8 + halfPx();
	}

	public int spriteOffsetY() {
		return 16 + halfPx();
	}
}
