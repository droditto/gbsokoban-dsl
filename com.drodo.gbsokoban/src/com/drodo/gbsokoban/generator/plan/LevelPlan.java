package com.drodo.gbsokoban.generator.plan;

import java.util.List;

public record LevelPlan(int width, int height, List<Integer> map,
		List<GroupedPos> boxes, List<GroupedPos> goals, int playerX, int playerY) {
}
