package com.drodo.gbsokoban.generator.plan;

import java.util.List;
import java.util.Locale;

public record SoundPlan(String role, String channel, List<Integer> registers) {

	public static final String BOX_ON_GOAL = "box_on_goal";

	public String flag() {
		return role.toUpperCase(Locale.ROOT);
	}
}
