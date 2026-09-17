package com.drodo.gbsokoban.util;

/** The ways a level can be cleared. The ordinal is the value emitted for WIN_*. */
public enum WinKind {

	ALL_ON(Feature.ALL_ON),
	SOME_ON(Feature.SOME_ON),
	NO_OBJECT(Feature.NO_OBJECT),
	NO_TILE(Feature.NO_TILE),
	PLAYER_ON(Feature.PLAYER_ON);

	private final Feature feature;

	WinKind(Feature feature) {
		this.feature = feature;
	}

	public Feature feature() {
		return feature;
	}

	public boolean indexesTile() {
		return this == NO_TILE || this == PLAYER_ON;
	}

	public String symbol() {
		return "WIN_" + name();
	}
}
