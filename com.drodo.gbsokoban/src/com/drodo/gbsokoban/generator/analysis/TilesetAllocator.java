package com.drodo.gbsokoban.generator.analysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.generator.plan.HardwareTile;
import com.drodo.gbsokoban.generator.plan.SpritesetPlan;
import com.drodo.gbsokoban.generator.plan.TilesetPlan;
import com.drodo.gbsokoban.util.CellGeometry;

public final class TilesetAllocator {

	private TilesetAllocator() {
	}

	public static TilesetPlan allocateBackground(List<Texture> textures, int cellPx) {
		int tilesPerSide = new CellGeometry(cellPx).tilesPerSide();
		List<HardwareTile> tiles = new ArrayList<>();
		Map<HardwareTile, Integer> slotOf = new LinkedHashMap<>();
		Map<String, List<Integer>> tilesOf = new LinkedHashMap<>();

		for (Texture texture : textures) {
			int[][] pixels = TileEncoder.pixelsOf(texture, cellPx);
			List<Integer> cellTiles = new ArrayList<>();
			for (int ty = 0; ty < tilesPerSide; ty++) {
				for (int tx = 0; tx < tilesPerSide; tx++) {
					HardwareTile tile = TileEncoder.encode(pixels, tx * 8, ty * 8);
					Integer slot = slotOf.get(tile);
					if (slot == null) {
						slot = tiles.size();
						tiles.add(tile);
						slotOf.put(tile, slot);
					}
					cellTiles.add(slot);
				}
			}
			tilesOf.put(texture.getName(), cellTiles);
		}
		return new TilesetPlan(cellPx, tiles, tilesOf);
	}

	/** Column-first, so each even pair forms one 8x16 hardware sprite. No sharing. */
	public static SpritesetPlan allocateSprites(List<Texture> textures, int cellPx) {
		int tilesPerSide = new CellGeometry(cellPx).tilesPerSide();
		List<HardwareTile> tiles = new ArrayList<>();
		Map<String, Integer> firstTileOf = new LinkedHashMap<>();

		for (Texture texture : textures) {
			int[][] pixels = TileEncoder.pixelsOf(texture, cellPx);
			firstTileOf.put(texture.getName(), tiles.size());
			// Top-left, bottom-left, top-right, bottom-right: 4k and 4k+1 are the
			// left 8x16 sprite, 4k+2 and 4k+3 the right.
			for (int tx = 0; tx < tilesPerSide; tx++)
				for (int ty = 0; ty < tilesPerSide; ty++)
					tiles.add(TileEncoder.encode(pixels, tx * 8, ty * 8));
		}
		return new SpritesetPlan(cellPx, tiles, firstTileOf);
	}
}
