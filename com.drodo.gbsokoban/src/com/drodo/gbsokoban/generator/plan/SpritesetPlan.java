package com.drodo.gbsokoban.generator.plan;

import java.util.List;
import java.util.Map;

import com.drodo.gbsokoban.util.CellGeometry;

/**
 * The sprite half of VRAM. Never shared: 8x16 pairs tiles N and N+1, so an index carries
 * meaning.
 */
public record SpritesetPlan(int cellPx, List<HardwareTile> tiles, Map<String, Integer> firstTileOf) {

	public boolean usesTallSprites() {
		return new CellGeometry(cellPx).usesTallSprites();
	}

	public int metaspriteIndexOf(String textureName) {
		return firstTileOf.get(textureName) / new CellGeometry(cellPx).tilesPerCell();
	}
}
