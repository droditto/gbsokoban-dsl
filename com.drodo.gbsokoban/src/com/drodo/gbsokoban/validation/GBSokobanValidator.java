package com.drodo.gbsokoban.validation;

import org.eclipse.xtext.validation.ComposedChecks;

import com.drodo.gbsokoban.gBSokoban.Deadly;
import com.drodo.gbsokoban.gBSokoban.Fillable;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.TileDef;

@ComposedChecks(validators = {
		GameValidator.class,
		PaletteValidator.class,
		TextureValidator.class,
		TileValidator.class,
		PlayerValidator.class,
		LegendValidator.class,
		WinValidator.class,
		SoundValidator.class,
		LevelValidator.class })
public class GBSokobanValidator extends AbstractGBSokobanValidator {

	public static final String ISSUE_DUPLICATE_SYMBOL = "issue.duplicateSymbol";
	public static final String ISSUE_SYMBOL_LENGTH = "issue.symbolLength";
	public static final String ISSUE_PULL_REQUIRES_CAN_PULL = "issue.pullRequiresCanPull";
	public static final String ISSUE_LEGEND_NO_PLAYER = "issue.legendNoPlayer";
	public static final String ISSUE_LEVEL_TOO_MANY_PLAYERS = "issue.levelTooManyPlayers";
	public static final String ISSUE_TEXTURE_SIZE = "issue.textureSize";
	public static final String ISSUE_PALETTE_SIZE = "issue.paletteSize";
	public static final String ISSUE_PULL_WITHOUT_OBJECTS = "issue.pullWithoutObjects";
	public static final String ISSUE_SOUND_NEVER_PLAYS = "issue.soundNeverPlays";

	static boolean destroysObjects(Game game) {
		for (TileDef tile : game.getTiles())
			if (tile.getBehaviour() instanceof Deadly || tile.getBehaviour() instanceof Fillable)
				return true;
		return false;
	}
}
