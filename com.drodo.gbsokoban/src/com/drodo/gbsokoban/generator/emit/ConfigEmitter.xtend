package com.drodo.gbsokoban.generator.emit

import com.drodo.gbsokoban.generator.plan.GamePlan
import com.drodo.gbsokoban.util.Feature
import com.drodo.gbsokoban.util.ScreenText

class ConfigEmitter {

	def String configH(GamePlan plan) '''
	#ifndef CONFIG_H
	#define CONFIG_H

	«IF plan.has(Feature.TITLE_SCREEN)»
	#define TITLE_SCREEN_TEXT "«cString(ScreenText.titleRows(plan.name, plan.author).join("\n"))»"
	«ENDIF»
	«IF plan.has(Feature.END_SCREEN)»
	#define ALL_LEVELS_COMPLETE_TEXT "«cString(ScreenText.wrap(plan.ending).join("\n"))»"
	«ENDIF»

	#endif // CONFIG_H
	'''

	private def String cString(String text) {
		text.replace("\n", "\\n")
	}
}
