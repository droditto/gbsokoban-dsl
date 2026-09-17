package com.drodo.gbsokoban.generator.emit

import com.drodo.gbsokoban.generator.plan.GroupedPos
import com.drodo.gbsokoban.generator.plan.GamePlan
import com.drodo.gbsokoban.generator.plan.LevelPlan
import com.drodo.gbsokoban.generator.plan.TilePlan
import com.drodo.gbsokoban.util.Feature
import java.util.List

class LevelDataEmitter {

	def String levelDataH(GamePlan plan) '''
	#ifndef LEVEL_DATA_H
	#define LEVEL_DATA_H

	// How each tile id behaves and which cell draws it, indexed by the TILE_* constants.
	const TileProperties tile_props[] = {
	    «FOR tile : plan.tiles SEPARATOR ','»
	    [«tile.tileSymbol»] = «tileEntry(tile)»
	    «ENDFOR»
	};

	// Row-major, one grid row per line, so the array reads the way the level looks.
	«FOR level : plan.levels.indexed»
	«levelArrays(plan, level.value, level.key + 1)»

	«ENDFOR»
	const LevelDef levels[] = {
	    «FOR level : plan.levels.indexed SEPARATOR ','»
	    «levelEntry(plan, level.value, level.key + 1)»
	    «ENDFOR»
	};

	#endif // LEVEL_DATA_H
	'''

	private def String levelArrays(GamePlan plan, LevelPlan level, int number) '''
	static const uint8_t level_«number»_map[] = {
	«mapRows(plan, level)»
	};
	«IF plan.has(Feature.BOXES) && !level.boxes.empty»
	// {x, y, object kind}
	static const GroupedPos level_«number»_boxes[] = {«positions(level.boxes)»};
	«ENDIF»
	«IF plan.has(Feature.HAS_GOALS) && !level.goals.empty»
	// {x, y, object kind the goal accepts}
	static const GroupedPos level_«number»_goals[] = {«positions(level.goals)»};
	«ENDIF»
	'''

	private def String levelEntry(GamePlan plan, LevelPlan level, int number) {
		'''{.width = «level.width», .height = «level.height», .map = level_«number»_map, .player_x = «level.playerX», .player_y = «level.playerY»«entityFields(plan, level, number)»}'''
			.toString
	}

	private def String positions(List<GroupedPos> entries) {
		entries.map['''{«x», «y», «group»}'''].join(", ")
	}

	private def String mapRows(GamePlan plan, LevelPlan level) {
		val rows = newArrayList
		for (y : 0 ..< level.height) {
			val symbols = (0 ..< level.width).map[x | plan.tiles.get(level.map.get(y * level.width + x)).tileSymbol]
			rows.add("    " + symbols.join(", ") + ",")
		}
		rows.join("\n")
	}

	private def String tileEntry(TilePlan tile) {
		if (tile.becomes === null)
			'''{.kind = «tile.kind.symbol», .cell = «tile.cellSymbol»}'''.toString
		else
			'''{.kind = «tile.kind.symbol», .cell = «tile.cellSymbol», .becomes = «tile.becomes»}'''.toString
	}

	private def String entityFields(GamePlan plan, LevelPlan level, int number) {
		val fields = new StringBuilder
		if (plan.has(Feature.BOXES))
			fields.append(spawnTable("boxes", number, level.boxes.size))
		if (plan.has(Feature.HAS_GOALS))
			fields.append(spawnTable("goals", number, level.goals.size))
		fields.toString
	}

	/** A level with none of a kind points nowhere rather than at an empty array, which C rejects. */
	private def String spawnTable(String name, int number, int size) {
		if (size == 0)
			''', .«name» = 0, .num_«name» = 0'''.toString
		else
			''', .«name» = level_«number»_«name», .num_«name» = «size»'''.toString
	}
}
