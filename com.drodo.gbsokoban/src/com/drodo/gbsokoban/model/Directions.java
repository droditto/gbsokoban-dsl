package com.drodo.gbsokoban.model;

import com.drodo.gbsokoban.gBSokoban.Direction;

public final class Directions {

	private static final int[] STEP_X = { 0, 0, -1, 1 };

	private static final int[] STEP_Y = { 1, -1, 0, 0 };

	private Directions() {
	}

	public static int stepX(Direction direction) {
		return STEP_X[direction.ordinal()];
	}

	public static int stepY(Direction direction) {
		return STEP_Y[direction.ordinal()];
	}

	public static Direction opposite(Direction direction) {
		switch (direction) {
			case DOWN:  return Direction.UP;
			case UP:    return Direction.DOWN;
			case LEFT:  return Direction.RIGHT;
			default:    return Direction.LEFT;
		}
	}

	public static String symbol(Direction direction) {
		return "DIR_" + direction.getLiteral();
	}
}
