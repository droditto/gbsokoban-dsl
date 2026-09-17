package com.drodo.gbsokoban.generator.emit

import com.drodo.gbsokoban.generator.plan.AnimPlan
import com.drodo.gbsokoban.generator.plan.GamePlan

class PlayerDataEmitter {

	def String playerDataH(GamePlan plan) '''
	#ifndef PLAYER_DATA_H
	#define PLAYER_DATA_H

	// Both arrays are indexed by direction, in DIR_* order. Frames index metasprites[];
	// mirror is bit 0 = flip X, bit 1 = flip Y.

	«FOR anim : plan.animations»
	«animArrays(plan, anim)»
	«ENDFOR»

	#endif // PLAYER_DATA_H
	'''

	private def String animArrays(GamePlan plan, AnimPlan anim) '''
	static const uint8_t «anim.name»_frames[4][PLAYER_ANIM_FRAMES] = {
	    «FOR frames : anim.frames SEPARATOR ','»
	    {«frames.map[plan.sprites.metaspriteIndexOf(it)].join(", ")»}
	    «ENDFOR»
	};
	static const uint8_t «anim.name»_mirror[4] = {«anim.mirrors.join(", ")»};
	'''
}
