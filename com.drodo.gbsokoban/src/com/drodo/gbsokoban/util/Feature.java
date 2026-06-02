package com.drodo.gbsokoban.util;

/**
 * Feature flags computed once per generation pass and consulted by every
 * sub-generator to gate optional sections of the emitted C code.
 */
public enum Feature {
	PUSH_ANIM,
	PULL,
	PULL_ANIM,
	HAS_GOALS,
	ON_GOAL,
	SLIDING,
	CRUMBLE,
	FILLABLE,
	CONVEYOR,
	DESTROYABLE,
	DEADLY,
	TITLE_SCREEN,
	END_SCREEN,
	LEVEL_SELECT,
	MULTI_LEVEL,
	BOXES,
	SOUND,
	ALL_ON,
	SOME_ON,
	NO_OBJECT,
	NO_TILE,
	PLAYER_ON
}
