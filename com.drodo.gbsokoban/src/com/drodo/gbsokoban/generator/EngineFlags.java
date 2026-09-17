package com.drodo.gbsokoban.generator;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.drodo.gbsokoban.generator.plan.CameraMode;
import com.drodo.gbsokoban.generator.plan.GamePlan;
import com.drodo.gbsokoban.generator.plan.SoundPlan;
import com.drodo.gbsokoban.model.AnimationChain;
import com.drodo.gbsokoban.util.Feature;

public final class EngineFlags {

	private EngineFlags() {
	}

	public static Set<String> definedBy(GamePlan plan) {
		Set<String> flags = new LinkedHashSet<>();
		for (Feature feature : Feature.values())
			if (plan.has(feature))
				flags.add("FEAT_" + feature.name());

		if (plan.needsSave())
			flags.add("FEAT_SAVE");
		if (plan.has(Feature.HAS_GOALS)
				&& (plan.has(Feature.ON_GOAL) || plan.declaresSound(SoundPlan.BOX_ON_GOAL)))
			flags.add("FEAT_GOAL_LANDING");
		if (plan.has(Feature.END_SCREEN)
				|| (plan.has(Feature.TITLE_SCREEN) && plan.has(Feature.MULTI_LEVEL)))
			flags.add("FEAT_SAVE_ALL_BEATEN");
		if (plan.palette().hasColor())
			flags.add("FEAT_COLOR");
		if (plan.has(Feature.TITLE_SCREEN) || plan.has(Feature.END_SCREEN))
			flags.add("FEAT_TEXT_SCREEN");
		if (plan.sprites().usesTallSprites())
			flags.add("FEAT_TALL_SPRITES");

		Set<Integer> mirrors = plan.usedMirrors();
		for (int mirror : mirrors)
			if (mirror != 0) {
				flags.add("FEAT_MIRROR_ANY");
				break;
			}
		if (mirrors.contains(AnimationChain.FLIP_X))
			flags.add("FEAT_MIRROR_X");
		if (mirrors.contains(AnimationChain.FLIP_Y))
			flags.add("FEAT_MIRROR_Y");

		for (SoundPlan sound : plan.sounds())
			flags.add("FEAT_SND_" + sound.flag());

		return flags;
	}

	public static Map<String, String> macroValues(GamePlan plan) {
		return Map.of(
				"CAMERA_MODE", plan.cameraMode() == CameraMode.CENTER ? "0" : "1",
				"CAMERA_MODE_CENTER", "0",
				"CAMERA_MODE_SCROLL", "1",
				"TILES_PER_SIDE", String.valueOf(plan.geometry().tilesPerSide()));
	}
}
