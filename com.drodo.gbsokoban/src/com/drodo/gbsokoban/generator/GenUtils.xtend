package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage
import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.gBSokoban.ObjectDef
import com.drodo.gbsokoban.gBSokoban.ObjectPalette
import com.drodo.gbsokoban.gBSokoban.TileDef
import com.drodo.gbsokoban.util.Feature
import java.util.Set

import static extension com.drodo.gbsokoban.util.ModelHelpers.*

/** Shared helpers used across the per-file generators. */
class GenUtils {

	static def String cname(TileDef tile) { tile.name.toUpperCase }
	static def String cname(ObjectDef object) { object.name.toUpperCase }

	static def String oamProps(ObjectPalette palette) {
		if (palette == ObjectPalette.OBP1) "OAMF_PAL1" else "0"
	}

	/** Number of BG metatile slots: max tileIdx among tiles, objects and onGoals plus 1. */
	static def int mtCount(Game game) {
		val onGoalIds = objectsOf(game)
						.filter[hasExplicit(GBSokobanPackage.Literals.OBJECT_DEF__ON_GOAL_TILE_IDX)]
						.map[onGoalTileIdx]
		val bgIds = (game.tiles.entries.map[tileIdx]
						+ objectsOf(game).map[tileIdx]
						+ onGoalIds).toList
		bgIds.max + 1
	}

	static def int objectIndex(Game game, ObjectDef object) { objectsOf(game).indexOf(object) }
	static def int tileIndex(Game game, TileDef tile) { game.tiles.entries.indexOf(tile) }

	/** Save module is emitted only for multi-level games. Single-level games have nothing to persist. */
	static def boolean needsSave(Set<Feature> features) {
		features.contains(Feature.MULTI_LEVEL)
	}

	/** MT_<obj> or MT_<obj>_ON_GOAL depending on whether the object has an on-goal sprite. */
	static def String mtName(ObjectDef object, boolean onGoal) {
		if (onGoal && object.hasExplicit(GBSokobanPackage.Literals.OBJECT_DEF__ON_GOAL_TILE_IDX))
			"MT_" + cname(object) + "_ON_GOAL"
		else
			"MT_" + cname(object)
	}
}
