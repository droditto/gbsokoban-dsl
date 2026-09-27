package com.drodo.gbsokoban.util;

public final class Speeds {

	public static final int MAX_ANIM_SPEED = 1 << CellGeometry.SUBPIXEL_SHIFT;

	/** What is left of that counter once a frame's worth of {@link #MAX_ANIM_SPEED} is reserved. */
	public static final int MAX_ANIM_FRAMES = (256 - MAX_ANIM_SPEED) >> CellGeometry.SUBPIXEL_SHIFT;

	private Speeds() {
	}

	public static int maxMoveSpeed(int cellPx) {
		return new CellGeometry(cellPx).subPixelsPerCell();
	}

	/** One cell every 16 frames, the same pace at 8x8 and at 16x16. */
	public static int defaultMoveSpeed(int cellPx) {
		return cellPx > 0 ? cellPx : CellGeometry.DEFAULT_PX;
	}

	public static int animSpeedFor(int frames, int moveSpeed, int cellPx) {
		if (frames < 1 || cellPx < 1)
			return 1;
		return Math.clamp(Math.round((float) frames * moveSpeed / cellPx), 1, MAX_ANIM_SPEED);
	}
}
