package com.drodo.gbsokoban.generator.plan;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.drodo.gbsokoban.util.CellGeometry;
import com.drodo.gbsokoban.util.Feature;

public record GamePlan(
		String name,
		String author,
		String ending,
		int cellPx,
		CameraMode cameraMode,
		Set<Feature> features,
		CapacityPlan capacities,
		PalettePlan palette,
		TilesetPlan background,
		SpritesetPlan sprites,
		List<TilePlan> tiles,
		List<ObjectPlan> objects,
		List<LevelPlan> levels,
		List<WinConditionPlan> winConditions,
		List<AnimPlan> animations,
		List<SoundPlan> sounds,
		int moveSpeed,
		int animSpeed,
		int animFrames,
		List<String> metaspriteProps) {
	public Set<Integer> usedMirrors() {
		Set<Integer> mirrors = new LinkedHashSet<>();
		for (AnimPlan anim : animations)
			mirrors.addAll(anim.mirrors());
		return mirrors;
	}

	public boolean has(Feature feature) {
		return features.contains(feature);
	}

	public boolean needsSave() {
		return has(Feature.MULTI_LEVEL);
	}

	public boolean declaresSound(String role) {
		for (SoundPlan sound : sounds)
			if (sound.role().equals(role))
				return true;
		return false;
	}

	private List<ObjectPlan> objectsWithGoalArt() {
		List<ObjectPlan> out = new ArrayList<>();
		for (ObjectPlan object : objects)
			if (object.hasOwnGoalArt())
				out.add(object);
		return out;
	}

	public List<CellPlan> cells() {
		List<CellPlan> out = new ArrayList<>();
		for (TilePlan tile : tiles)
			out.add(new CellPlan(tile.cellSymbol(), tile.textureName()));
		for (ObjectPlan object : objects)
			out.add(new CellPlan(object.cellSymbol(), object.textureName()));
		for (ObjectPlan object : objectsWithGoalArt())
			out.add(new CellPlan(object.onGoalCellSymbol(), object.goalTextureName()));
		return out;
	}

	public CellGeometry geometry() {
		return new CellGeometry(cellPx);
	}

	public String plainTitle() {
		String firstLine = name == null ? "" : name.split("\n", 2)[0];
		return firstLine.replaceAll("[^A-Za-z0-9]+", "");
	}
}
