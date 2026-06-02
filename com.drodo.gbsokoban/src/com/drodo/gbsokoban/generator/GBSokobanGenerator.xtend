package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.AllOn
import com.drodo.gbsokoban.gBSokoban.ConveyorTile
import com.drodo.gbsokoban.gBSokoban.CrumbleTile
import com.drodo.gbsokoban.gBSokoban.DeadlyTile
import com.drodo.gbsokoban.gBSokoban.FillableTile
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage
import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.gBSokoban.IceTile
import com.drodo.gbsokoban.gBSokoban.NoEntity
import com.drodo.gbsokoban.gBSokoban.ObjectDef
import com.drodo.gbsokoban.gBSokoban.ObjectRef
import com.drodo.gbsokoban.gBSokoban.PlayerRef
import com.drodo.gbsokoban.gBSokoban.SomeOn
import com.drodo.gbsokoban.gBSokoban.TileDef
import com.drodo.gbsokoban.gBSokoban.WinCondition
import com.drodo.gbsokoban.util.Feature
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.EnumSet
import java.util.LinkedHashMap
import java.util.Map
import java.util.Set
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.generator.AbstractGenerator
import org.eclipse.xtext.generator.IFileSystemAccess2
import org.eclipse.xtext.generator.IGeneratorContext

import static extension com.drodo.gbsokoban.generator.GenUtils.*
import static extension com.drodo.gbsokoban.util.ModelHelpers.*

/** Top-level generator: runs each sub-generator and writes everything under <GameName>/. */
class GBSokobanGenerator extends AbstractGenerator {

	static val SCREEN_W = 160
	static val SCREEN_H = 144
	static val CELL_PX = 16

	override void doGenerate(Resource resource, IFileSystemAccess2 fsa, IGeneratorContext ctx) {
		val game = resource.contents.head as Game
		if (game === null) return

		val base = game.name + "/"
		val goalMap = collectGoalTiles(game)
		val features = computeFeatures(game, goalMap)
		val levels = game.levels.levels.map[new LevelParser(game, it, goalMap)].toList

		// Camera mode: NONE (all screen-sized), CENTER (some smaller), SCROLL (any larger).
		val cameraMode = if (levels.exists[width * CELL_PX > SCREEN_W || height * CELL_PX > SCREEN_H])
				CameraGen.Mode.SCROLL
			else if (levels.exists[width * CELL_PX < SCREEN_W || height * CELL_PX < SCREEN_H])
				CameraGen.Mode.CENTER
			else
				CameraGen.Mode.NONE

		val box = new BoxGen
		val camera = new CameraGen
		val common = new CommonGen
		val gameGen = new GameGen
		val level = new LevelGen
		val main = new MainGen
		val menus = new MenusGen
		val player = new PlayerGen
		val save = new SaveGen
		val sound = new SoundGen
		val ui = new UiGen
		val makefile = new MakefileGen

		// src/
		fsa.generateFile(base + "src/common.h", common.commonH(game, levels, features))
		fsa.generateFile(base + "src/main.c", main.mainC(features))
		fsa.generateFile(base + "src/game.h", gameGen.gameH())
		fsa.generateFile(base + "src/game.c", gameGen.gameC(game, features))
		fsa.generateFile(base + "src/menus.h", menus.menusH(features))
		fsa.generateFile(base + "src/menus.c", menus.menusC(game, features))
		fsa.generateFile(base + "src/level.h", level.levelH(game, levels, features))
		fsa.generateFile(base + "src/level.c", level.levelC(game, levels, features))
		if (features.contains(Feature.BOXES)) {
			fsa.generateFile(base + "src/box.h", box.boxH(features))
			fsa.generateFile(base + "src/box.c", box.boxC(game, features))
		}
		fsa.generateFile(base + "src/player.h", player.playerH(features))
		fsa.generateFile(base + "src/player.c", player.playerC(game, features))
		fsa.generateFile(base + "src/camera.h", camera.cameraH(cameraMode))
		if (cameraMode != CameraGen.Mode.NONE) {
			fsa.generateFile(base + "src/camera.c", camera.cameraC(cameraMode))
		}
		if (features.needsSave) {
			fsa.generateFile(base + "src/save.h", save.saveH(features))
			fsa.generateFile(base + "src/save.c", save.saveC(features))
		}
		if (features.contains(Feature.SOUND)) {
			fsa.generateFile(base + "src/sound.h", sound.soundH(game, features))
			fsa.generateFile(base + "src/sound.c", sound.soundC(game, features))
		}
		fsa.generateFile(base + "src/ui.h", ui.uiH(features))
		fsa.generateFile(base + "src/ui.c", ui.uiC(game, features))

		// Makefile
		fsa.generateFile(base + "Makefile", makefile.makefile(game, features))

		// res/
		copyAsset(game.tileSheet, fsa, base + "res/background.png")
		copyAsset(game.spriteSheet, fsa, base + "res/sprites.png")
		if (game.titleScreen !== null) copyAsset(game.titleScreen, fsa, base + "res/title_screen.png")
		if (game.endingScreen !== null) copyAsset(game.endingScreen, fsa, base + "res/ending_screen.png")

		// build
		runMake(fsa, base + "Makefile")
	}

	private def Set<Feature> computeFeatures(Game game, Map<TileDef, ObjectDef> goalMap) {
		val features = EnumSet.noneOf(Feature)
		if (game.player.push !== null) features.add(Feature.PUSH_ANIM)
		if (game.player.canPull) features.add(Feature.PULL)
		if (game.player.pull !== null) features.add(Feature.PULL_ANIM)
		if (!goalMap.empty) features.add(Feature.HAS_GOALS)
		if (!goalMap.empty &&
			objectsOf(game).exists[hasExplicit(GBSokobanPackage.Literals.OBJECT_DEF__ON_GOAL_TILE_IDX)])
			features.add(Feature.ON_GOAL)
		if (game.tiles.entries.exists[type instanceof IceTile])
			features.add(Feature.SLIDING)
		if (game.tiles.entries.exists[type instanceof CrumbleTile])
			features.add(Feature.CRUMBLE)
		if (game.tiles.entries.exists[type instanceof FillableTile])
			features.add(Feature.FILLABLE)
		if (game.tiles.entries.exists[type instanceof ConveyorTile])
			features.add(Feature.CONVEYOR)
		val hasObjectKillingTile = game.tiles.entries.exists[type instanceof DeadlyTile || type instanceof FillableTile]
		if (hasObjectKillingTile ||
			game.win.conditions.exists[it instanceof NoEntity && (it as NoEntity).entity instanceof ObjectDef])
			features.add(Feature.DESTROYABLE)
		if (hasObjectKillingTile) features.add(Feature.DEADLY)
		if (game.titleScreen !== null) features.add(Feature.TITLE_SCREEN)
		if (game.endingScreen !== null) features.add(Feature.END_SCREEN)
		// Level select shows automatically for multi-level games unless the user opts out.
		if (game.levels.levels.size > 1 && !game.disableLevelSelect) features.add(Feature.LEVEL_SELECT)
		if (game.levels.levels.size > 1) features.add(Feature.MULTI_LEVEL)
		if (!objectsOf(game).empty) features.add(Feature.BOXES)
		if (game.sounds !== null && !game.sounds.eContents.empty) features.add(Feature.SOUND)
		val winConditions = game.win.conditions
		if (winConditions.exists[it instanceof AllOn && (it as AllOn).subject instanceof ObjectRef])
			features.add(Feature.ALL_ON)
		if (winConditions.exists[it instanceof SomeOn && (it as SomeOn).subject instanceof ObjectRef])
			features.add(Feature.SOME_ON)
		if (winConditions.exists[it instanceof NoEntity && (it as NoEntity).entity instanceof ObjectDef])
			features.add(Feature.NO_OBJECT)
		if (winConditions.exists[it instanceof NoEntity && (it as NoEntity).entity instanceof TileDef])
			features.add(Feature.NO_TILE)
		if (winConditions.exists[
				(it instanceof AllOn && (it as AllOn).subject instanceof PlayerRef) ||
				(it instanceof SomeOn && (it as SomeOn).subject instanceof PlayerRef)])
			features.add(Feature.PLAYER_ON)
		features
	}

	/** Maps each tile used by an object-targeting AllOn/SomeOn to its target object. */
	private def Map<TileDef, ObjectDef> collectGoalTiles(Game game) {
		val tileToObject = new LinkedHashMap<TileDef, ObjectDef>
		for (condition : game.win.conditions) {
			val tile = condition.subjectTile
			val obj = condition.referencedObject
			if (tile !== null && obj !== null) tileToObject.put(tile, obj)
		}
		tileToObject
	}

	/** The tile slot of an AllOn/SomeOn (null otherwise). */
	private def static TileDef subjectTile(WinCondition wc) {
		switch wc { AllOn: wc.tile SomeOn: wc.tile default: null }
	}

	/** The ObjectDef referenced by an AllOn/SomeOn whose subject is an object (null otherwise). */
	private def static ObjectDef referencedObject(WinCondition wc) {
		val subject = switch wc { AllOn: wc.subject SomeOn: wc.subject default: null }
		if (subject instanceof ObjectRef) subject.ref else null
	}

	private def void copyAsset(String assetPath, IFileSystemAccess2 fsa, String outputPath) {
		try {
			val source = Paths.get(assetPath)
			if (!Files.exists(source)) {
				System.err.println("GBSokoban generator: asset not found, skipping: " + source.toAbsolutePath)
				return
			}
			fsa.generateFile(outputPath, new FileInputStream(source.toFile))
		} catch (Exception e) {
			System.err.println("GBSokoban generator: failed to copy " + assetPath + ": " + e.message)
		}
	}

	private def void runMake(IFileSystemAccess2 fsa, String makefilePath) {
		try {
			val uri = fsa.getURI(makefilePath)
			var Path dir = null
			if (uri.isFile) {
				dir = Paths.get(uri.toFileString).parent
			} else if (uri.isPlatformResource) {
				val wsFile = ResourcesPlugin.workspace.root
					.getFile(new org.eclipse.core.runtime.Path(uri.toPlatformString(true)))
				val loc = wsFile?.rawLocation
				if (loc !== null) dir = Paths.get(loc.toOSString).parent
			}
			if (dir !== null && Files.exists(dir))
				new ProcessBuilder("make")
					.directory(dir.toFile)
					.inheritIO
					.start
		} catch (Exception e) {
			System.err.println("GBSokoban generator: failed to launch make for " + makefilePath + ": " + e.message)
		}
	}
}
