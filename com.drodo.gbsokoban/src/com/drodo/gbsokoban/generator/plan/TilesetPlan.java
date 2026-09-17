package com.drodo.gbsokoban.generator.plan;

import java.util.List;
import java.util.Map;

import com.drodo.gbsokoban.util.CellGeometry;

public record TilesetPlan(int cellPx, List<HardwareTile> tiles, Map<String, List<Integer>> tilesOf) {

	public int tilesPerCell() {
		return new CellGeometry(cellPx).tilesPerCell();
	}
}
