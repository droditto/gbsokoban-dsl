package com.drodo.gbsokoban.generator.emit

import com.drodo.gbsokoban.generator.plan.GamePlan
import com.drodo.gbsokoban.generator.plan.SoundPlan

/**
 * Emits the SFX bank; engine/sound.c plays it. The definitions go in a header, because an
 * extra translation unit shifts the link layout.
 */
class SfxEmitter {

	def String sfxH(GamePlan plan) '''
	#ifndef SFX_H
	#define SFX_H

	«FOR sound : plan.sounds»
	extern const SoundDef sfx_«sound.role»;
	«ENDFOR»

	#endif // SFX_H
	'''

	def String sfxDataH(GamePlan plan) '''
	#ifndef SFX_DATA_H
	#define SFX_DATA_H

	// {channel, NRx0, NRx1, NRx2, NRx3, NRx4}.

	«FOR sound : plan.sounds»
	const SoundDef sfx_«sound.role» = {«sound.channel», «registerBytes(sound)»};
	«ENDFOR»

	#endif // SFX_DATA_H
	'''

	private def String registerBytes(SoundPlan sound) {
		sound.registers.map[String.format("0x%02X", it)].join(", ")
	}
}
