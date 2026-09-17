package com.drodo.gbsokoban.generator.emit

import com.drodo.gbsokoban.generator.plan.GamePlan
import com.drodo.gbsokoban.util.Feature
import com.drodo.gbsokoban.util.ScreenText

class ConfigEmitter {

	static val TITLE_TEXT_ROWS = ScreenText.ROWS - 4

	def String configH(GamePlan plan) '''
	#ifndef CONFIG_H
	#define CONFIG_H

	«IF plan.has(Feature.TITLE_SCREEN)»
	#define TITLE_SCREEN_TEXT "«cString(titleScreenText(plan))»"
	«ENDIF»
	«IF plan.has(Feature.END_SCREEN)»
	#define ALL_LEVELS_COMPLETE_TEXT "«cString(ScreenText.fitBlock(plan.ending, ScreenText.ROWS))»"
	«ENDIF»

	#endif // CONFIG_H
	'''

	private def String titleScreenText(GamePlan plan) {
		val name = ScreenText.printable(plan.name)
		val author = ScreenText.printable(plan.author)
		val text = if (name.empty) "BY " + author
			else if (author.empty) name
			else name + "\n\n" + "BY " + author
		ScreenText.fitBlock(text, TITLE_TEXT_ROWS)
	}

	private def String cString(String text) {
		text.replace("\n", "\\n")
	}
}
