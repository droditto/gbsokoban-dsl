package com.drodo.gbsokoban.generator.emit

import com.drodo.gbsokoban.generator.plan.GamePlan
import com.drodo.gbsokoban.util.Feature

class BoxDataEmitter {

	def String boxDataH(GamePlan plan) '''
	#ifndef BOX_DATA_H
	#define BOX_DATA_H

	// Indexed by Box.group, which is the object index a level spawn carries.

	const BoxTypeProps box_type_props[NUM_BOX_TYPES] = {
	    «FOR object : plan.objects SEPARATOR ','»
	    {.cell = «object.cellSymbol»«IF plan.has(Feature.ON_GOAL)», .on_goal_cell = «object.onGoalCellSymbol»«ENDIF», .metasprite_idx = «plan.sprites.metaspriteIndexOf(object.textureName)», .oam_props = «object.oamProps»}
	    «ENDFOR»
	};

	#endif // BOX_DATA_H
	'''
}
