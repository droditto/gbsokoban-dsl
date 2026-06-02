package com.drodo.gbsokoban.validation;

import static com.drodo.gbsokoban.util.ModelHelpers.hasExplicit;
import static com.drodo.gbsokoban.util.ModelHelpers.objectsOf;
import static com.drodo.gbsokoban.util.ModelHelpers.tilesOf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.Check;

import com.drodo.gbsokoban.gBSokoban.AllOn;
import com.drodo.gbsokoban.gBSokoban.AnimBlock;
import com.drodo.gbsokoban.gBSokoban.CrumbleTile;
import com.drodo.gbsokoban.gBSokoban.DeadlyTile;
import com.drodo.gbsokoban.gBSokoban.DirAnim;
import com.drodo.gbsokoban.gBSokoban.Entity;
import com.drodo.gbsokoban.gBSokoban.FillableTile;
import com.drodo.gbsokoban.gBSokoban.FlipAnim;
import com.drodo.gbsokoban.gBSokoban.FrameList;
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.Level;
import com.drodo.gbsokoban.gBSokoban.LevelsBlock;
import com.drodo.gbsokoban.gBSokoban.NoEntity;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.ObjectRef;
import com.drodo.gbsokoban.gBSokoban.PaletteBlock;
import com.drodo.gbsokoban.gBSokoban.PaletteEntry;
import com.drodo.gbsokoban.gBSokoban.PaletteRegister;
import com.drodo.gbsokoban.gBSokoban.PlayerDef;
import com.drodo.gbsokoban.gBSokoban.PlayerRef;
import com.drodo.gbsokoban.gBSokoban.SolidTile;
import com.drodo.gbsokoban.gBSokoban.SomeOn;
import com.drodo.gbsokoban.gBSokoban.SoundSpec;
import com.drodo.gbsokoban.gBSokoban.Subject;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.gBSokoban.WinCondition;
import com.drodo.gbsokoban.util.CharPool;

public class GBSokobanValidator extends AbstractGBSokobanValidator {

	public static final String ISSUE_TILE_SHEET_NO_PNG = "issue.tileSheetNoPng";
	public static final String ISSUE_SPRITE_SHEET_NO_PNG = "issue.spriteSheetNoPng";
	public static final String ISSUE_TITLE_SCREEN_NO_PNG = "issue.titleScreenNoPng";
	public static final String ISSUE_ENDING_SCREEN_NO_PNG = "issue.endingScreenNoPng";
	public static final String ISSUE_LEGEND_NO_PLAYER = "issue.legendNoPlayer";
	public static final String ISSUE_LEVEL_NOT_RECTANGULAR = "issue.levelNotRectangular";
	public static final String ISSUE_LEVEL_TOO_MANY_PLAYERS = "issue.levelTooManyPlayers";
	public static final String ISSUE_DUPLICATE_CHAR = "issue.duplicateChar";
	public static final String ISSUE_CHAR_LENGTH = "issue.charLength";
	public static final String ISSUE_PULL_REQUIRES_CAN_PULL = "issue.pullRequiresCanPull";
	public static final String ISSUE_ON_GOAL_HAS_NO_REFERENCE = "issue.onGoalHasNoReference";

	private static final int MAX_LEVELS = 255;

	private static final Object[][] PNG_ASSETS = {
		{GBSokobanPackage.Literals.GAME__TILE_SHEET,    "tileSheet",    ISSUE_TILE_SHEET_NO_PNG},
		{GBSokobanPackage.Literals.GAME__SPRITE_SHEET,  "spriteSheet",  ISSUE_SPRITE_SHEET_NO_PNG},
		{GBSokobanPackage.Literals.GAME__TITLE_SCREEN,  "titleScreen",  ISSUE_TITLE_SCREEN_NO_PNG},
		{GBSokobanPackage.Literals.GAME__ENDING_SCREEN, "endingScreen", ISSUE_ENDING_SCREEN_NO_PNG},
	};

	private static final EAttribute[] SOUND_REGS = {
		GBSokobanPackage.Literals.SOUND_SPEC__R0,
		GBSokobanPackage.Literals.SOUND_SPEC__R1,
		GBSokobanPackage.Literals.SOUND_SPEC__R2,
		GBSokobanPackage.Literals.SOUND_SPEC__R3,
		GBSokobanPackage.Literals.SOUND_SPEC__R4,
	};

	// ==== Game ====

	@Check
	public void checkLevelCountWithinEngineLimit(LevelsBlock block) {
		int levelCount = block.getLevels().size();
		if (levelCount > MAX_LEVELS)
			error("Maximum " + MAX_LEVELS + " levels supported, found " + levelCount,
					block, null);
	}

	@Check
	public void checkMovementSpeedRange(Game game) {
		// Only flag explicit values. Omitted properties default to 0 and need no error.
		if (hasExplicit(game, GBSokobanPackage.Literals.GAME__MOVEMENT_SPEED)
				&& (game.getMovementSpeed() < 1 || game.getMovementSpeed() > 32))
			error("movementSpeed must be between 1 and 32",
					GBSokobanPackage.Literals.GAME__MOVEMENT_SPEED);
	}

	@Check
	public void checkAnimationSpeedRange(Game game) {
		if (hasExplicit(game, GBSokobanPackage.Literals.GAME__ANIMATION_SPEED)
				&& (game.getAnimationSpeed() < 1 || game.getAnimationSpeed() > 8))
			error("animationSpeed must be between 1 and 8",
					GBSokobanPackage.Literals.GAME__ANIMATION_SPEED);
	}

	@Check
	public void checkUniqueNames(Game game) {
		Set<String> usedNames = new HashSet<>();
		for (TileDef tile : tilesOf(game)) {
			if (tile.getName() != null && !usedNames.add(tile.getName()))
				error("Name '" + tile.getName() + "' is already used",
						tile, GBSokobanPackage.Literals.ENTITY__NAME, -1);
		}
		for (ObjectDef object : objectsOf(game)) {
			if (object.getName() != null && !usedNames.add(object.getName()))
				error("Name '" + object.getName() + "' is already used",
						object, GBSokobanPackage.Literals.ENTITY__NAME, -1);
		}
	}

	@Check
	public void checkUniqueChars(Game game) {
		Map<String, String> charOwner = new HashMap<>();
		for (TileDef tile : tilesOf(game))
			reportDuplicateChar(tile, tile.getChar(), "tile " + tile.getName(),
					GBSokobanPackage.Literals.ENTITY__CHAR, charOwner);
		for (ObjectDef object : objectsOf(game))
			reportDuplicateChar(object, object.getChar(), "object " + object.getName(),
					GBSokobanPackage.Literals.ENTITY__CHAR, charOwner);
		if (game.getLegend() != null)
			for (LegendEntry entry : game.getLegend().getEntries())
				reportDuplicateChar(entry, entry.getChar(), "legend",
						GBSokobanPackage.Literals.LEGEND_ENTRY__CHAR, charOwner);
	}

	private void reportDuplicateChar(EObject host, String character, String owner,
			EStructuralFeature feature, Map<String, String> charOwner) {
		if (character == null) return;
		String previousOwner = charOwner.put(character, owner);
		if (previousOwner != null)
			error("Character '" + character + "' is already used by " + previousOwner,
					host, feature, -1, ISSUE_DUPLICATE_CHAR);
	}

	@Check
	public void checkLegendHasPlayer(Game game) {
		if (game.getLegend() == null) return;
		for (LegendEntry entry : game.getLegend().getEntries()) {
			if (entry.getEntity() instanceof PlayerRef)
				return;
		}
		error("Legend must bind a character to Player",
				GBSokobanPackage.Literals.GAME__LEGEND,
				ISSUE_LEGEND_NO_PLAYER);
	}

	@Check
	public void checkPaletteRegistersUnique(PaletteBlock palette) {
		Set<PaletteRegister> seenRegisters = new HashSet<>();
		for (PaletteEntry entry : palette.getEntries()) {
			if (!seenRegisters.add(entry.getRegister()))
				error("Palette register " + entry.getRegister().getLiteral() + " is already defined",
						entry, GBSokobanPackage.Literals.PALETTE_ENTRY__REGISTER, -1);
		}
	}

	@Check
	public void checkAssetPaths(Game game) {
		for (Object[] asset : PNG_ASSETS) {
			EAttribute feature = (EAttribute) asset[0];
			String path = (String) game.eGet(feature);
			if (path != null && !path.endsWith(".png"))
				warning(asset[1] + " must be a .png file", feature, (String) asset[2]);
		}
	}

	// ==== Tiles and objects ====

	@Check
	public void checkEntityCharLength(Entity entity) {
		if (entity.getChar() != null && entity.getChar().length() != 1)
			error("Character must be exactly one character",
					entity, GBSokobanPackage.Literals.ENTITY__CHAR, -1, ISSUE_CHAR_LENGTH);
	}

	@Check
	public void checkEntityTileIdxRange(Entity entity) {
		if (entity.getTileIdx() > 63)
			error("tileIdx must be between 0 and 63 (background metatile budget)",
					GBSokobanPackage.Literals.ENTITY__TILE_IDX);
	}

	@Check
	public void checkTileTransformNotIntoSelf(TileDef tile) {
		if (tile.getType() instanceof CrumbleTile) {
			CrumbleTile crumble = (CrumbleTile) tile.getType();
			if (crumble.getCollapseTile() == tile)
				error("Tile cannot crumble into itself",
						GBSokobanPackage.Literals.TILE_DEF__TYPE);
		}
		if (tile.getType() instanceof FillableTile) {
			FillableTile fillable = (FillableTile) tile.getType();
			if (fillable.getFilledTile() == tile)
				error("Tile cannot fill into itself",
						GBSokobanPackage.Literals.TILE_DEF__TYPE);
		}
	}

	@Check
	public void checkObjectIndicesRange(ObjectDef object) {
		if (object.getSpriteIdx() > 31)
			error("spriteIdx must be between 0 and 31 (sprite metasprite budget)",
					GBSokobanPackage.Literals.OBJECT_DEF__SPRITE_IDX);
		if (object.getOnGoalTileIdx() > 63)
			error("onGoal must be between 0 and 63",
					GBSokobanPackage.Literals.OBJECT_DEF__ON_GOAL_TILE_IDX);
	}

	@Check
	public void checkOnGoalTileIdxNeedsGoalReference(ObjectDef object) {
		if (!hasExplicit(object, GBSokobanPackage.Literals.OBJECT_DEF__ON_GOAL_TILE_IDX))
			return;
		Game game = EcoreUtil2.getContainerOfType(object, Game.class);
		if (game == null || game.getWin() == null) return;
		for (WinCondition condition : game.getWin().getConditions()) {
			if (condition instanceof AllOn && referencesObject(((AllOn) condition).getSubject(), object)) return;
			if (condition instanceof SomeOn && referencesObject(((SomeOn) condition).getSubject(), object)) return;
		}
		warning("onGoal will never show because no win condition places this object on a tile",
				object, GBSokobanPackage.Literals.OBJECT_DEF__ON_GOAL_TILE_IDX, -1,
				ISSUE_ON_GOAL_HAS_NO_REFERENCE);
	}

	private static boolean referencesObject(Subject subject, ObjectDef object) {
		return subject instanceof ObjectRef && ((ObjectRef) subject).getRef() == object;
	}

	// ==== Player ====

	@Check
	public void checkPullAnimRequiresCanPull(PlayerDef player) {
		if (player.getPull() != null && !player.isCanPull())
			error("'pull' animation requires 'canPull' on the player",
					player, GBSokobanPackage.Literals.PLAYER_DEF__PULL, -1,
					ISSUE_PULL_REQUIRES_CAN_PULL);
	}

	@Check
	public void checkFlipAnimCycle(PlayerDef player) {
		checkFlipCycleFor(player.getWalk(), GBSokobanPackage.Literals.PLAYER_DEF__WALK);
		checkFlipCycleFor(player.getPush(), GBSokobanPackage.Literals.PLAYER_DEF__PUSH);
		checkFlipCycleFor(player.getPull(), GBSokobanPackage.Literals.PLAYER_DEF__PULL);
	}

	private void checkFlipCycleFor(AnimBlock block, EStructuralFeature feature) {
		if (block == null) return;
		for (DirAnim direction : new DirAnim[]{block.getDown(), block.getUp(), block.getLeft(), block.getRight()}) {
			if (direction instanceof FlipAnim && hasFlipCycle(direction, block, 0)) {
				error("Flip chain never reaches a frame list: at least one direction must use explicit frame indices",
						feature);
				return;
			}
		}
	}

	// Four directions. Once depth exceeds that, any non-terminating chain is a cycle.
	private static boolean hasFlipCycle(DirAnim direction, AnimBlock block, int depth) {
		if (depth > 4) return true;
		if (direction instanceof FrameList) return false;
		FlipAnim flip = (FlipAnim) direction;
		DirAnim source;
		switch (flip.getSource()) {
			case DOWN: source = block.getDown(); break;
			case UP: source = block.getUp(); break;
			case LEFT: source = block.getLeft(); break;
			default: source = block.getRight(); break;
		}
		return hasFlipCycle(source, block, depth + 1);
	}

	@Check
	public void checkFrameIndicesRange(FrameList frames) {
		for (int i = 0; i < frames.getFrames().size(); i++) {
			if (frames.getFrames().get(i) > 31)
				error("Frame index must be between 0 and 31",
						frames, GBSokobanPackage.Literals.FRAME_LIST__FRAMES, i);
		}
	}

	@Check
	public void checkFrameListsSameLength(PlayerDef player) {
		// The engine uses a single PLAYER_ANIM_FRAMES count for all directions.
		int referenceLength = -1;
		for (AnimBlock block : new AnimBlock[]{player.getWalk(), player.getPush(), player.getPull()}) {
			if (block == null) continue;
			for (DirAnim direction : new DirAnim[]{block.getDown(), block.getUp(), block.getLeft(), block.getRight()}) {
				if (!(direction instanceof FrameList)) continue;
				int length = ((FrameList) direction).getFrames().size();
				if (referenceLength == -1) referenceLength = length;
				else if (length != referenceLength) {
					error("All frame lists must have the same length, expected " + referenceLength + " frames",
							direction, GBSokobanPackage.Literals.FRAME_LIST__FRAMES, -1);
				}
			}
		}
	}

	// ==== Legend ====

	@Check
	public void checkLegendCharLength(LegendEntry entry) {
		if (entry.getChar() != null && entry.getChar().length() != 1)
			error("Character must be exactly one character",
					entry, GBSokobanPackage.Literals.LEGEND_ENTRY__CHAR, -1, ISSUE_CHAR_LENGTH);
	}

	@Check
	public void checkLegendPlayerTilePlayable(LegendEntry entry) {
		if (!(entry.getEntity() instanceof PlayerRef) || entry.getTile() == null) return;
		Object type = entry.getTile().getType();
		if (type instanceof CrumbleTile || type instanceof FillableTile)
			warning("Player on a tile that disappears under it would leave the player nowhere to stand",
					GBSokobanPackage.Literals.LEGEND_ENTRY__TILE);
		else if (type instanceof DeadlyTile)
			warning("Player on a deadly tile starts the level on a death cell",
					GBSokobanPackage.Literals.LEGEND_ENTRY__TILE);
		else if (type instanceof SolidTile)
			warning("Player on a solid tile starts the level on a blocked cell",
					GBSokobanPackage.Literals.LEGEND_ENTRY__TILE);
	}

	// ==== Win ====

	@Check
	public void checkNoEntityFeasible(NoEntity condition) {
		EObject entity = condition.getEntity();
		if (entity instanceof ObjectDef) {
			Game game = EcoreUtil2.getContainerOfType(condition, Game.class);
			if (game == null) return;
			for (TileDef tile : tilesOf(game)) {
				if (tile.getType() instanceof DeadlyTile || tile.getType() instanceof FillableTile)
					return;
			}
			warning("'no' condition is unreachable because no tile destroys objects",
					GBSokobanPackage.Literals.NO_ENTITY__ENTITY);
		} else if (entity instanceof TileDef) {
			Object type = ((TileDef) entity).getType();
			if (!(type instanceof CrumbleTile) && !(type instanceof FillableTile))
				warning("'no' condition is unreachable because this tile never disappears",
						GBSokobanPackage.Literals.NO_ENTITY__ENTITY);
		}
	}

	@Check
	public void checkAllOnTilePlayable(AllOn condition) {
		warnIfTileBlocksObjects(condition.getTile(), GBSokobanPackage.Literals.ALL_ON__TILE);
	}

	@Check
	public void checkSomeOnTilePlayable(SomeOn condition) {
		warnIfTileBlocksObjects(condition.getTile(), GBSokobanPackage.Literals.SOME_ON__TILE);
	}

	private void warnIfTileBlocksObjects(TileDef tile, EStructuralFeature feature) {
		if (tile == null) return;
		Object type = tile.getType();
		if (type instanceof SolidTile)
			warning("Win condition unreachable: objects cannot land on a solid tile", feature);
		else if (type instanceof DeadlyTile || type instanceof FillableTile)
			warning("Win condition unreachable: objects are destroyed on this tile, not held", feature);
	}

	// ==== Sounds ====

	@Check
	public void checkSoundSpec(SoundSpec spec) {
		for (EAttribute reg : SOUND_REGS)
			if (((Integer) spec.eGet(reg)) > 255)
				error(reg.getName() + " must be between 0 and 255", reg);

		if (spec.getChannel() == null) return;
		switch (spec.getChannel()) {
			case NR2:
			case NR4:
				if (spec.getR0() != 0)
					warning("This channel has no sweep; r0 is ignored",
							GBSokobanPackage.Literals.SOUND_SPEC__R0);
				break;
			case NR3:
				if (spec.getR0() != 0 && spec.getR0() != 128)
					warning("NR30 r0 only uses bit 7 (DAC enable); use 0 or 128",
							GBSokobanPackage.Literals.SOUND_SPEC__R0);
				if ((spec.getR2() & ~0x60) != 0)
					warning("NR32 r2 only uses bits 6-5 (volume); use 0, 32, 64 or 96",
							GBSokobanPackage.Literals.SOUND_SPEC__R2);
				break;
			default:
				break;
		}
	}

	// ==== Levels ====

	@Check
	public void checkLevelRowsRectangular(Level level) {
		int expectedWidth = level.getRows().get(0).length();
		for (int i = 1; i < level.getRows().size(); i++) {
			int actualWidth = level.getRows().get(i).length();
			if (actualWidth != expectedWidth) {
				error("Row width " + actualWidth + ", expected " + expectedWidth,
						level, GBSokobanPackage.Literals.LEVEL__ROWS, i,
						ISSUE_LEVEL_NOT_RECTANGULAR);
				return;
			}
		}
	}

	@Check
	public void checkLevelDimensions(Level level) {
		int width = level.getRows().get(0).length();
		int height = level.getRows().size();
		if (width > 16)
			error("Level width " + width + " exceeds the 16 cell limit",
					level, GBSokobanPackage.Literals.LEVEL__ROWS, 0);
		if (height > 16)
			error("Level height " + height + " exceeds the 16 cell limit",
					level, GBSokobanPackage.Literals.LEVEL__ROWS, 16);
	}

	@Check
	public void checkLevelHasExactlyOnePlayer(Level level) {
		Game game = EcoreUtil2.getContainerOfType(level, Game.class);
		if (game == null || game.getLegend() == null) return;

		Set<String> playerChars = new HashSet<>();
		for (LegendEntry entry : game.getLegend().getEntries()) {
			if (entry.getEntity() instanceof PlayerRef && entry.getChar() != null)
				playerChars.add(entry.getChar());
		}
		if (playerChars.isEmpty()) return;

		int playerCount = 0;
		int extraPlayerRow = -1;
		for (int i = 0; i < level.getRows().size(); i++) {
			for (int j = 0; j < level.getRows().get(i).length(); j++) {
				if (playerChars.contains(String.valueOf(level.getRows().get(i).charAt(j)))) {
					playerCount++;
					if (playerCount == 2) extraPlayerRow = i;
				}
			}
		}

		if (playerCount == 0)
			error("Level must contain exactly one player position", level, null);
		else if (playerCount > 1)
			error("Level has " + playerCount + " player positions, expected exactly one",
					level, GBSokobanPackage.Literals.LEVEL__ROWS, extraPlayerRow,
					ISSUE_LEVEL_TOO_MANY_PLAYERS);
	}

	@Check
	public void checkLevelCharsValid(Level level) {
		Game game = EcoreUtil2.getContainerOfType(level, Game.class);
		if (game == null) return;

		Set<String> validChars = CharPool.usedIn(game);

		// One warning per distinct unknown character, anchored at its first occurrence.
		Set<String> reported = new HashSet<>();
		for (int i = 0; i < level.getRows().size(); i++) {
			String row = level.getRows().get(i);
			for (int j = 0; j < row.length(); j++) {
				String character = String.valueOf(row.charAt(j));
				if (!validChars.contains(character) && reported.add(character)) {
					warning("Character '" + character + "' is not declared in tiles, objects or legend",
							level, GBSokobanPackage.Literals.LEVEL__ROWS, i);
				}
			}
		}
	}

}
