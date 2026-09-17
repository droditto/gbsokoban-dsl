package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.generator.analysis.GamePlanBuilder
import com.drodo.gbsokoban.generator.emit.AssetEmitter
import com.drodo.gbsokoban.generator.emit.BoxDataEmitter
import com.drodo.gbsokoban.generator.emit.ConfigEmitter
import com.drodo.gbsokoban.generator.emit.ConstantsEmitter
import com.drodo.gbsokoban.generator.emit.LevelDataEmitter
import com.drodo.gbsokoban.generator.emit.MakefileEmitter
import com.drodo.gbsokoban.generator.emit.PlayerDataEmitter
import com.drodo.gbsokoban.generator.emit.SfxEmitter
import com.drodo.gbsokoban.generator.emit.WinDataEmitter
import com.drodo.gbsokoban.generator.plan.GamePlan
import com.drodo.gbsokoban.util.Feature
import com.google.inject.Inject
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.generator.AbstractGenerator
import org.eclipse.xtext.generator.IFileSystemAccess2
import org.eclipse.xtext.generator.IGeneratorContext

class GBSokobanGenerator extends AbstractGenerator {

	@Inject BuildTrigger buildTrigger

	val planBuilder = new GamePlanBuilder
	val config = new ConfigEmitter
	val constants = new ConstantsEmitter
	val assets = new AssetEmitter
	val levelData = new LevelDataEmitter
	val winData = new WinDataEmitter
	val playerData = new PlayerDataEmitter
	val boxData = new BoxDataEmitter
	val sfx = new SfxEmitter
	val makefile = new MakefileEmitter

	override void doGenerate(Resource resource, IFileSystemAccess2 fsa, IGeneratorContext ctx) {
		val game = resource.contents.head as Game
		if (game === null) return

		val plan = planBuilder.build(game)
		val project = projectName(resource)
		val base = project + "/"

		val flags = EngineFlags.definedBy(plan)
		val macroValues = EngineFlags.macroValues(plan)
		val sources = newArrayList("assets.c")
		for (name : EngineSources.FILES)
			if (needsEngineFile(name, plan)) {
				fsa.generateFile(base + "src/" + name,
					EnginePreprocessor.resolve(EngineSources.read(name), flags, macroValues))
				if (name.endsWith(".c")) sources.add(name)
			}

		fsa.generateFile(base + "src/config.h", config.configH(plan))
		fsa.generateFile(base + "src/common.h", constants.commonH(plan))

		fsa.generateFile(base + "src/assets.h", assets.assetsH(plan))
		fsa.generateFile(base + "src/assets.c", assets.assetsC(plan))
		fsa.generateFile(base + "src/level_data.h", levelData.levelDataH(plan))
		fsa.generateFile(base + "src/win_data.h", winData.winDataH(plan))
		fsa.generateFile(base + "src/player_data.h", playerData.playerDataH(plan))
		if (plan.has(Feature.BOXES))
			fsa.generateFile(base + "src/box_data.h", boxData.boxDataH(plan))
		if (plan.has(Feature.SOUND)) {
			fsa.generateFile(base + "src/sfx.h", sfx.sfxH(plan))
			fsa.generateFile(base + "src/sfx_data.h", sfx.sfxDataH(plan))
		}

		fsa.generateFile(base + "Makefile", makefile.makefile(plan, project, sources))

		buildTrigger.afterGenerate(fsa, base + "Makefile")
	}

	private def String projectName(Resource resource) {
		val stem = resource.URI.trimFileExtension.lastSegment
		val cleaned = if (stem === null) "" else stem.replaceAll("[^A-Za-z0-9_]+", "")
		if (cleaned.empty) "game" else cleaned
	}

	private def boolean needsEngineFile(String name, GamePlan plan) {
		if (name.startsWith("box.")) return plan.has(Feature.BOXES)
		if (name.startsWith("landing.")) return plan.has(Feature.BOXES) || plan.has(Feature.CRUMBLE)
			|| plan.has(Feature.DEADLY)
		if (name.startsWith("sound.")) return plan.has(Feature.SOUND)
		if (name.startsWith("save.")) return plan.needsSave
		true
	}
}
