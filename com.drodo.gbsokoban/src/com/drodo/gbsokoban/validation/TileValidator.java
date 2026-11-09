package com.drodo.gbsokoban.validation;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.drodo.gbsokoban.gBSokoban.Crumble;
import com.drodo.gbsokoban.gBSokoban.Fillable;
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.Level;
import com.drodo.gbsokoban.gBSokoban.NoEntity;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.gBSokoban.WinCondition;
import com.drodo.gbsokoban.model.GoalTiles;
import com.drodo.gbsokoban.util.Messages;

public class TileValidator extends AbstractDeclarativeValidator {

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	@Check
	public void checkTileSymbolLength(TileDef tile) {
		if (tile.getSymbol() != null && tile.getSymbol().length() != 1)
			error(Messages.get("tile.symbolLength"),
					tile, GBSokobanPackage.Literals.TILE_DEF__SYMBOL, -1, GBSokobanValidator.ISSUE_SYMBOL_LENGTH);
	}

	@Check
	public void checkLegendSymbolLength(LegendEntry entry) {
		if (entry.getSymbol() != null && entry.getSymbol().length() != 1)
			error(Messages.get("tile.symbolLength"),
					entry, GBSokobanPackage.Literals.LEGEND_ENTRY__SYMBOL, -1, GBSokobanValidator.ISSUE_SYMBOL_LENGTH);
	}

	@Check
	public void checkOnGoalArtHasGoals(ObjectDef object) {
		if (object.getGoalTexture() == null)
			return;
		Game game = EcoreUtil2.getContainerOfType(object, Game.class);
		if (game == null || GoalTiles.owners(game).containsValue(object)
				|| GBSokobanValidator.hasUnresolvedReferences(game))
			return;
		warning(Messages.get("tile.onGoalNoGoal"),
				GBSokobanPackage.Literals.OBJECT_DEF__GOAL_TEXTURE);
	}

	@Check
	public void checkTileIsUsed(Game game) {
		for (TileDef tile : game.getTiles()) {
			if (isReachable(tile, game))
				continue;
			warning(Messages.get("tile.unused"),
					tile, GBSokobanPackage.Literals.ENTITY__NAME, -1);
		}
	}

	private static boolean isReachable(TileDef tile, Game game) {
		for (LegendEntry entry : game.getLegend())
			if (entry.getTile() == tile)
				return true;
		for (TileDef other : game.getTiles()) {
			if (other.getBehaviour() instanceof Crumble
					&& ((Crumble) other.getBehaviour()).getTarget() == tile)
				return true;
			if (other.getBehaviour() instanceof Fillable
					&& ((Fillable) other.getBehaviour()).getTarget() == tile)
				return true;
		}
		for (WinCondition condition : game.getWin())
			if (GoalTiles.tileOf(condition) == tile
					|| (condition instanceof NoEntity && ((NoEntity) condition).getEntity() == tile))
				return true;
		if (tile.getSymbol() == null)
			return false;
		for (Level level : game.getLevels())
			for (String row : level.getRows())
				if (row.contains(tile.getSymbol()))
					return true;
		return false;
	}

	@Check
	public void checkTileTransformNotIntoSelf(TileDef tile) {
		if (tile.getBehaviour() instanceof Crumble && ((Crumble) tile.getBehaviour()).getTarget() == tile)
			error(Messages.get("tile.intoSelf"), GBSokobanPackage.Literals.TILE_DEF__BEHAVIOUR);
		if (tile.getBehaviour() instanceof Fillable && ((Fillable) tile.getBehaviour()).getTarget() == tile)
			error(Messages.get("tile.intoSelf"), GBSokobanPackage.Literals.TILE_DEF__BEHAVIOUR);
	}
}
