package com.drodo.gbsokoban.validation;

import static com.drodo.gbsokoban.model.SourceText.wasWritten;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.PaletteDef;
import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.model.GameTextures;
import com.drodo.gbsokoban.util.CSymbols;
import com.drodo.gbsokoban.util.ScreenText;
import com.drodo.gbsokoban.util.Speeds;
import com.drodo.gbsokoban.util.TileKind;

public class GameValidator extends AbstractDeclarativeValidator {

	/** The engine counts levels with a uint8_t. */
	private static final int MAX_LEVELS = 0xFF;

	private static final String ENGINE = "the engine";

	/** The TILE_ and CELL_ names common.h and level.h already spend. */
	private static final Set<String> ENGINE_MACROS = Set.of(
			"CELL_PX", "CELL_SHIFT",
			"TILE_BECOMES", "TILE_CELL", "TILE_CONVEYOR_DIR", "TILE_IS_CONVEYOR",
			"TILE_IS_CRUMBLE", "TILE_IS_FILLABLE", "TILE_IS_ICE", "TILE_IS_PASSABLE",
			"TILE_KILLS");

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	@Check
	public void checkFullScreenText(Game game) {
		reportScreenText(game.getTitle(), GBSokobanPackage.Literals.GAME__TITLE);
		reportScreenText(game.getAuthor(), GBSokobanPackage.Literals.GAME__AUTHOR);
		reportScreenText(game.getEnding(), GBSokobanPackage.Literals.GAME__ENDING);

		int title = ScreenText.titleRows(game.getTitle(), game.getAuthor()).size();
		if (title > ScreenText.TITLE_ROWS)
			error("The title screen needs " + title + " rows and only " + ScreenText.TITLE_ROWS
					+ " fit above PUSH START. Shorten the title or the author",
					ScreenText.titleRows(game.getTitle(), null).size() >= ScreenText.titleRows(null, game.getAuthor()).size()
							? GBSokobanPackage.Literals.GAME__TITLE
							: GBSokobanPackage.Literals.GAME__AUTHOR);
		int ending = ScreenText.wrap(game.getEnding()).size();
		if (ending > ScreenText.ROWS)
			error("The ending needs " + ending + " rows and only " + ScreenText.ROWS + " fit on the screen",
					GBSokobanPackage.Literals.GAME__ENDING);
	}

	private void reportScreenText(String text, EStructuralFeature feature) {
		if (ScreenText.losesCharacters(text))
			warning((ScreenText.printable(text).isEmpty() ? "None of this will show"
					: "This will show as '" + ScreenText.printable(text) + "'")
					+ ". The font has only A-Z, 0-9 and spaces", feature);
		for (String row : ScreenText.wrap(text))
			if (row.length() > ScreenText.COLUMNS)
				error("'" + row + "' is " + row.length() + " characters long and only "
						+ ScreenText.COLUMNS + " fit on a row", feature);
	}

	@Check
	public void checkLevelCountWithinEngineLimit(Game game) {
		if (game.getLevels().size() > MAX_LEVELS)
			error("A game can have at most " + MAX_LEVELS + " levels. This one has " + game.getLevels().size(),
					game.getLevels().get(MAX_LEVELS), GBSokobanPackage.Literals.LEVEL__ROWS, 0);
	}

	@Check
	public void checkSpeedRanges(Game game) {
		int fastest = Speeds.maxMoveSpeed(GameTextures.cellPx(game));
		if (wasWritten(game, GBSokobanPackage.Literals.GAME__MOVE_SPEED)
				&& (game.getMoveSpeed() < 1 || game.getMoveSpeed() > fastest))
			error("MOVE_SPEED must be between 1 and " + fastest,
					GBSokobanPackage.Literals.GAME__MOVE_SPEED);
		if (wasWritten(game, GBSokobanPackage.Literals.GAME__ANIM_SPEED)
				&& (game.getAnimSpeed() < 1 || game.getAnimSpeed() > Speeds.MAX_ANIM_SPEED))
			error("ANIM_SPEED must be between 1 and " + Speeds.MAX_ANIM_SPEED,
					GBSokobanPackage.Literals.GAME__ANIM_SPEED);
	}

	@Check
	public void checkUniqueNames(Game game) {
		Set<String> palettes = new HashSet<>();
		for (PaletteDef palette : game.getPalettes())
			reportDuplicateName(palette, palette.getName(), palettes,
					GBSokobanPackage.Literals.PALETTE_DEF__NAME);

		Set<String> textures = new HashSet<>();
		for (Texture texture : game.getTextures())
			reportDuplicateName(texture, texture.getName(), textures, GBSokobanPackage.Literals.TEXTURE__NAME);

		Set<String> entities = new HashSet<>();
		for (TileDef tile : game.getTiles())
			reportDuplicateName(tile, tile.getName(), entities, GBSokobanPackage.Literals.ENTITY__NAME);
		for (ObjectDef object : game.getObjects())
			reportDuplicateName(object, object.getName(), entities, GBSokobanPackage.Literals.ENTITY__NAME);
	}

	private void reportDuplicateName(EObject host, String name, Set<String> used, EStructuralFeature feature) {
		if (name != null && !used.add(name.toLowerCase(Locale.ROOT)))
			error("The name '" + name + "' is already used. Names ignore capitals",
					host, feature, -1);
	}

	private static Set<String> reservedCSymbols() {
		Set<String> reserved = new HashSet<>();
		for (TileKind kind : TileKind.values())
			reserved.add(kind.symbol());
		reserved.add(CSymbols.tile(CSymbols.OUTSIDE));
		reserved.add(CSymbols.cell(CSymbols.OUTSIDE));
		// A tile called 'cell' would emit TILE_CELL, which is already a macro in level.h.
		reserved.addAll(ENGINE_MACROS);
		return reserved;
	}

	/**
	 * Two names that differ can still make one C symbol: {@code crate} with ON_GOAL art and a
	 * tile {@code crate_on_goal} are both CELL_CRATE_ON_GOAL.
	 */
	@Check
	public void checkEmittedSymbolsUnique(Game game) {
		Map<String, String> owner = new HashMap<>();
		for (String reserved : reservedCSymbols())
			owner.put(reserved, ENGINE);

		for (TileDef tile : game.getTiles()) {
			claimSymbol(tile, CSymbols.tile(tile.getName()), "'" + tile.getName() + "'", owner);
			claimSymbol(tile, CSymbols.cell(tile.getName()), "'" + tile.getName() + "'", owner);
		}
		for (ObjectDef object : game.getObjects()) {
			claimSymbol(object, CSymbols.cell(object.getName()), "'" + object.getName() + "'", owner);
			if (object.getGoalTexture() != null)
				claimSymbol(object, CSymbols.onGoalCell(object.getName()),
						"the ON_GOAL texture of '" + object.getName() + "'", owner);
		}
	}

	private void claimSymbol(EObject host, String symbol, String who, Map<String, String> owner) {
		String previous = owner.putIfAbsent(symbol, who);
		if (previous == null || previous.equals(who))
			return;
		error(previous.equals(ENGINE)
				? "The game already uses this name internally. Use another one"
				: "This ends up with the same name as " + previous + ". Rename one of them",
				host, GBSokobanPackage.Literals.ENTITY__NAME, -1);
	}

	@Check
	public void checkUniqueSymbols(Game game) {
		Map<String, String> owner = new HashMap<>();
		for (TileDef tile : game.getTiles())
			reportDuplicateSymbol(tile, tile.getSymbol(), "'" + tile.getName() + "'",
					GBSokobanPackage.Literals.TILE_DEF__SYMBOL, owner);
		for (LegendEntry entry : game.getLegend())
			reportDuplicateSymbol(entry, entry.getSymbol(), "a legend entry",
					GBSokobanPackage.Literals.LEGEND_ENTRY__SYMBOL, owner);
	}

	private void reportDuplicateSymbol(EObject host, String symbol, String who,
			EStructuralFeature feature, Map<String, String> owner) {
		if (symbol == null)
			return;
		String previous = owner.put(symbol, who);
		if (previous != null)
			error("'" + symbol + "' is already used by " + previous + ". Use another character",
					host, feature, -1, GBSokobanValidator.ISSUE_DUPLICATE_SYMBOL);
	}
}
