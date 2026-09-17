package com.drodo.gbsokoban.generator.emit

import com.drodo.gbsokoban.generator.plan.GamePlan
import com.drodo.gbsokoban.generator.plan.WinConditionPlan

class WinDataEmitter {

	def String winDataH(GamePlan plan) '''
	#ifndef WIN_DATA_H
	#define WIN_DATA_H

	// A level is clear when every one of these holds. The second field is an object index,
	// or a tile id for WIN_NO_TILE and WIN_PLAYER_ON.
	const WinCondition win_conditions[] = {
	    «FOR condition : plan.winConditions»
	    «conditionEntry(plan, condition)»
	    «ENDFOR»
	};
	const uint8_t num_win_conditions = «plan.winConditions.size»;

	#endif // WIN_DATA_H
	'''

	private def String conditionEntry(GamePlan plan, WinConditionPlan condition) {
		val kind = String.format("%-15s", condition.kind.symbol + ",")
		if (condition.kind.indexesTile)
			return "{" + kind + plan.tiles.get(condition.group).tileSymbol + "},"
		"{" + kind + condition.group + "},  // " + plan.objects.get(condition.group).name
	}
}
