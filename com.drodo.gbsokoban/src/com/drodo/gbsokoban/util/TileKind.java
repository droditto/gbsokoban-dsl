package com.drodo.gbsokoban.util;

/** What a tile does to whatever stands on it. The ordinal is the value emitted for TILE_KIND_*. */
public enum TileKind {

	SOLID,
	FLOOR,
	DEADLY,
	ICE,
	CRUMBLE,
	FILLABLE,
	CONVEYOR;

	public String symbol() {
		return "TILE_KIND_" + name();
	}
}
