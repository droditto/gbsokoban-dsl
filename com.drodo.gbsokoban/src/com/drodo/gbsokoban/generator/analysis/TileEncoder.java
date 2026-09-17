package com.drodo.gbsokoban.generator.analysis;

import java.util.ArrayList;
import java.util.List;

import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.generator.plan.HardwareTile;

final class TileEncoder {

	private TileEncoder() {
	}

	/**
	 * Rows as palette indices. '.' reads as 0. Missing or short rows read as 0; the validator
	 * reports those.
	 */
	static int[][] pixelsOf(Texture texture, int cellPx) {
		int[][] pixels = new int[cellPx][cellPx];
		List<String> rows = texture.getRows();
		for (int y = 0; y < cellPx; y++) {
			String row = y < rows.size() ? rows.get(y) : "";
			for (int x = 0; x < cellPx; x++) {
				char c = x < row.length() ? row.charAt(x) : '0';
				pixels[y][x] = (c == '.') ? 0 : (c - '0');
			}
		}
		return pixels;
	}

	/** 2bpp: per row a byte of low bits then one of high bits, most significant leftmost. */
	static HardwareTile encode(int[][] pixels, int originX, int originY) {
		List<Integer> bytes = new ArrayList<>();
		for (int y = 0; y < 8; y++) {
			int low = 0;
			int high = 0;
			for (int x = 0; x < 8; x++) {
				int value = pixels[originY + y][originX + x];
				int bit = 7 - x;
				low |= (value & 1) << bit;
				high |= ((value >> 1) & 1) << bit;
			}
			bytes.add(low);
			bytes.add(high);
		}
		return new HardwareTile(bytes);
	}
}
