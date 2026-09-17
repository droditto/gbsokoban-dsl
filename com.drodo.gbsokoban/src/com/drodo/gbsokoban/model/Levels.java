package com.drodo.gbsokoban.model;

import com.drodo.gbsokoban.gBSokoban.Level;

public final class Levels {

	private Levels() {
	}

	public static int width(Level level) {
		int width = 0;
		for (String row : level.getRows())
			if (row.length() > width)
				width = row.length();
		return width;
	}
}
