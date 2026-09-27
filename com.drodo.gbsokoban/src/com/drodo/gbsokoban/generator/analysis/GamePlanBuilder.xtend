package com.drodo.gbsokoban.generator.analysis

import com.drodo.gbsokoban.gBSokoban.AllOn
import com.drodo.gbsokoban.gBSokoban.AnimKind
import com.drodo.gbsokoban.gBSokoban.Conveyor
import com.drodo.gbsokoban.gBSokoban.Crumble
import com.drodo.gbsokoban.gBSokoban.Deadly
import com.drodo.gbsokoban.gBSokoban.Direction
import com.drodo.gbsokoban.gBSokoban.Fillable
import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.gBSokoban.Ice
import com.drodo.gbsokoban.gBSokoban.NoEntity
import com.drodo.gbsokoban.gBSokoban.ObjectDef
import com.drodo.gbsokoban.gBSokoban.ObjectRef
import com.drodo.gbsokoban.gBSokoban.PlayerRef
import com.drodo.gbsokoban.gBSokoban.Solid
import com.drodo.gbsokoban.gBSokoban.SomeOn
import com.drodo.gbsokoban.gBSokoban.SoundChannel
import com.drodo.gbsokoban.gBSokoban.SoundEvent
import com.drodo.gbsokoban.gBSokoban.Texture
import com.drodo.gbsokoban.gBSokoban.TileDef
import com.drodo.gbsokoban.gBSokoban.WinCondition
import com.drodo.gbsokoban.generator.plan.AnimPlan
import com.drodo.gbsokoban.generator.plan.CameraMode
import com.drodo.gbsokoban.generator.plan.CapacityPlan
import com.drodo.gbsokoban.generator.plan.GamePlan
import com.drodo.gbsokoban.generator.plan.LevelPlan
import com.drodo.gbsokoban.generator.plan.ObjectPlan
import com.drodo.gbsokoban.generator.plan.SoundPlan
import com.drodo.gbsokoban.generator.plan.TilePlan
import com.drodo.gbsokoban.generator.plan.WinConditionPlan
import com.drodo.gbsokoban.model.AnimationChain
import com.drodo.gbsokoban.model.Directions
import com.drodo.gbsokoban.model.GameTextures
import com.drodo.gbsokoban.model.GoalTiles
import com.drodo.gbsokoban.util.CSymbols
import com.drodo.gbsokoban.util.Feature
import com.drodo.gbsokoban.util.ScreenText
import com.drodo.gbsokoban.util.Speeds
import com.drodo.gbsokoban.util.TileKind
import com.drodo.gbsokoban.util.WinKind
import java.util.EnumSet
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Set

class GamePlanBuilder {

	def GamePlan build(Game game) {
		val cellPx = GameTextures.cellPx(game)
		val goalMap = GoalTiles.owners(game)
		val compiler = new LevelCompiler(game, goalMap)
		val levels = game.levels.map[compiler.compile(it)].toList
		val background = GameTextures.background(game)
		val sprites = GameTextures.sprites(game)
		val palettes = new PaletteResolver(background, sprites)
		val winPlans = winConditions(game)
		val features = computeFeatures(game, goalMap, winPlans)
		val moveSpeed = if (game.moveSpeed > 0) game.moveSpeed else Speeds.defaultMoveSpeed(cellPx)
		val animFrames = AnimationChain.frameCount(game.player)

		new GamePlan(
			game.title,
			game.author,
			game.ending,
			cellPx,
			cameraMode(levels, cellPx),
			features,
			capacities(game, levels),
			palettes.plan,
			TilesetAllocator.allocateBackground(background, cellPx),
			TilesetAllocator.allocateSprites(sprites, cellPx),
			tiles(game),
			objects(game, palettes),
			levels,
			winPlans,
			animations(game, features),
			sounds(game, features),
			moveSpeed,
			if (game.animSpeed > 0) game.animSpeed else Speeds.animSpeedFor(animFrames, moveSpeed, cellPx),
			animFrames,
			metaspriteProps(sprites, palettes))
	}

	private def CameraMode cameraMode(List<LevelPlan> levels, int cellPx) {
		if (levels.exists[width * cellPx > ScreenText.PIXEL_WIDTH || height * cellPx > ScreenText.PLAY_PIXEL_HEIGHT])
			CameraMode.SCROLL
		else
			CameraMode.CENTER
	}

	private def CapacityPlan capacities(Game game, List<LevelPlan> levels) {
		new CapacityPlan(
			levels.size,
			game.objects.size,
			Math.max(levels.map[boxes.size].max, 1),
			Math.max(levels.map[goals.size].max, 1),
			levels.map[width].max,
			levels.map[height].max)
	}

	private def List<String> metaspriteProps(List<Texture> sprites, PaletteResolver palettes) {
		sprites.map[palettes.oamProps(it)].toList
	}

	private def List<TilePlan> tiles(Game game) {
		val entries = game.tiles
		val out = newArrayList
		for (var i = 0; i < entries.size; i++) {
			val tile = entries.get(i)
			out.add(new TilePlan(
				tile.name,
				i,
				tile.texture.name,
				tileKind(tile.behaviour),
				becomes(tile)))
		}
		out.add(new TilePlan(
			CSymbols.OUTSIDE,
			LevelCompiler.outsideIndex(game),
			entries.head.texture.name,
			TileKind.SOLID,
			null))
		out
	}

	private def String becomes(TileDef tile) {
		val behaviour = tile.behaviour
		switch behaviour {
			Crumble: CSymbols.tile(behaviour.target.name)
			Fillable: CSymbols.tile(behaviour.target.name)
			Conveyor: Directions.symbol(behaviour.direction)
			default: null
		}
	}

	private def TileKind tileKind(Object behaviour) {
		switch (behaviour) {
			Solid: TileKind.SOLID
			Deadly: TileKind.DEADLY
			Ice: TileKind.ICE
			Crumble: TileKind.CRUMBLE
			Fillable: TileKind.FILLABLE
			Conveyor: TileKind.CONVEYOR
			default: TileKind.FLOOR
		}
	}

	private def List<ObjectPlan> objects(Game game, PaletteResolver palettes) {
		game.objects.map[new ObjectPlan(name, texture.name, goalTexture?.name,
			palettes.oamProps(texture))].toList
	}

	private def List<WinConditionPlan> winConditions(Game game) {
		game.win.map[condition | winPlan(condition, game)].toList
	}

	private def WinConditionPlan winPlan(WinCondition condition, Game game) {
		switch condition {
			AllOn case condition.subject instanceof ObjectRef:
				new WinConditionPlan(WinKind.ALL_ON, game.objects.indexOf((condition.subject as ObjectRef).object))
			SomeOn case condition.subject instanceof ObjectRef:
				new WinConditionPlan(WinKind.SOME_ON, game.objects.indexOf((condition.subject as ObjectRef).object))
			AllOn case condition.subject instanceof PlayerRef:
				new WinConditionPlan(WinKind.PLAYER_ON, game.tiles.indexOf(condition.tile))
			SomeOn case condition.subject instanceof PlayerRef:
				new WinConditionPlan(WinKind.PLAYER_ON, game.tiles.indexOf(condition.tile))
			NoEntity case condition.entity instanceof ObjectDef:
				new WinConditionPlan(WinKind.NO_OBJECT, game.objects.indexOf(condition.entity as ObjectDef))
			NoEntity case condition.entity instanceof TileDef:
				new WinConditionPlan(WinKind.NO_TILE, game.tiles.indexOf(condition.entity as TileDef))
			default:
				throw new IllegalStateException("Unhandled win condition: " + condition?.eClass?.name)
		}
	}

	private def List<AnimPlan> animations(Game game, Set<Feature> features) {
		val out = newArrayList
		out.add(animPlan(game, AnimKind.WALK))
		if (features.contains(Feature.PUSH_ANIM)) out.add(animPlan(game, AnimKind.PUSH))
		if (features.contains(Feature.PULL_ANIM)) out.add(animPlan(game, AnimKind.PULL))
		out
	}

	private def AnimPlan animPlan(Game game, AnimKind kind) {
		val frames = <List<String>>newArrayList
		val mirrors = <Integer>newArrayList
		for (dir : Direction.values) {
			val resolved = AnimationChain.follow(game.player, kind, dir)
			frames.add(if (resolved.source === null) <String>newArrayList
				else resolved.source.frames.textures.map[name].toList)
			mirrors.add(resolved.flips)
		}
		new AnimPlan(kind.literal.toLowerCase(Locale.ROOT), frames, mirrors)
	}

	private def List<SoundPlan> sounds(Game game, Set<Feature> features) {
		if (!features.contains(Feature.SOUND)) return newArrayList
		game.sounds.map[sound | new SoundPlan(sfxRole(sound.event), channelId(sound.channel),
			#[sound.r0, sound.r1, sound.r2, sound.r3, sound.r4])].toList
	}

	private def String sfxRole(SoundEvent event) {
		switch event {
			case MOVE: "player_move"
			case BLOCKED: "player_blocked"
			case PUSH: "box_push"
			case ON_GOAL: SoundPlan.BOX_ON_GOAL
			case DESTROY: "box_destroyed"
			case COMPLETE: "level_complete"
			case RESTART: "level_restart"
			case COLLAPSE: "crumble"
			case MENU_MOVE: "menu_move"
			case MENU_OK: "menu_select"
			default: throw new IllegalStateException("Unhandled sound event: " + event)
		}
	}

	private def String channelId(SoundChannel channel) {
		switch channel {
			case NR1: "SND_NR1"
			case NR2: "SND_NR2"
			case NR3: "SND_NR3"
			case NR4: "SND_NR4"
			default: throw new IllegalStateException("Unhandled sound channel: " + channel)
		}
	}

	private def Set<Feature> computeFeatures(Game game, Map<TileDef, ObjectDef> goalMap,
			List<WinConditionPlan> winPlans) {
		val features = EnumSet.noneOf(Feature)
		addMechanicFeatures(game, goalMap, features)
		addWinFeatures(winPlans, features)
		addPresentationFeatures(game, features)
		features
	}

	private def void addMechanicFeatures(Game game, Map<TileDef, ObjectDef> goalMap, Set<Feature> features) {
		val anims = game.player.anims
		if (anims.exists[kind == AnimKind.PUSH]) features.add(Feature.PUSH_ANIM)
		if (game.canPull) features.add(Feature.PULL)
		if (anims.exists[kind == AnimKind.PULL]) features.add(Feature.PULL_ANIM)
		if (!goalMap.empty) features.add(Feature.HAS_GOALS)
		if (!goalMap.empty && game.objects.exists[goalTexture !== null]) features.add(Feature.ON_GOAL)
		if (game.tiles.exists[behaviour instanceof Ice]) features.add(Feature.SLIDING)
		if (game.tiles.exists[behaviour instanceof Crumble]) features.add(Feature.CRUMBLE)
		if (game.tiles.exists[behaviour instanceof Fillable]) features.add(Feature.FILLABLE)
		if (game.tiles.exists[behaviour instanceof Conveyor]) features.add(Feature.CONVEYOR)
		if (game.tiles.exists[behaviour instanceof Deadly || behaviour instanceof Fillable])
			features.add(Feature.DEADLY)
		if (!game.objects.empty) features.add(Feature.BOXES)
	}

	private def void addWinFeatures(List<WinConditionPlan> winPlans, Set<Feature> features) {
		for (condition : winPlans)
			features.add(condition.kind.feature)

		if (features.contains(Feature.DEADLY) || features.contains(Feature.NO_OBJECT))
			features.add(Feature.DESTROYABLE)
	}

	private def void addPresentationFeatures(Game game, Set<Feature> features) {
		if (!ScreenText.printable(game.title).empty || !ScreenText.printable(game.author).empty)
			features.add(Feature.TITLE_SCREEN)
		if (!ScreenText.printable(game.ending).empty) features.add(Feature.END_SCREEN)
		if (game.levels.size > 1) features.add(Feature.MULTI_LEVEL)
		if (game.levels.size > 1 && !game.noLevelSelect) features.add(Feature.LEVEL_SELECT)
		if (!game.sounds.empty) features.add(Feature.SOUND)
	}
}
