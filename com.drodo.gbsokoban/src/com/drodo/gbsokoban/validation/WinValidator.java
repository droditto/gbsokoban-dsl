package com.drodo.gbsokoban.validation;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.drodo.gbsokoban.gBSokoban.AllOn;
import com.drodo.gbsokoban.gBSokoban.Crumble;
import com.drodo.gbsokoban.gBSokoban.Deadly;
import com.drodo.gbsokoban.gBSokoban.Fillable;
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.NoEntity;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.PlayerRef;
import com.drodo.gbsokoban.gBSokoban.Solid;
import com.drodo.gbsokoban.gBSokoban.SomeOn;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.gBSokoban.WinCondition;
import com.drodo.gbsokoban.model.GoalTiles;
import com.drodo.gbsokoban.util.Messages;

public class WinValidator extends AbstractDeclarativeValidator {

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	@Check
	public void checkNoEntityFeasible(NoEntity condition) {
		EObject entity = condition.getEntity();
		if (entity instanceof ObjectDef) {
			Game game = EcoreUtil2.getContainerOfType(condition, Game.class);
			if (game == null || GBSokobanValidator.destroysObjects(game))
				return;
			warning(Messages.get("win.noDestroyer"),
					GBSokobanPackage.Literals.NO_ENTITY__ENTITY);
		} else if (entity instanceof TileDef) {
			TileDef tile = (TileDef) entity;
			if (!(tile.getBehaviour() instanceof Crumble) && !(tile.getBehaviour() instanceof Fillable))
				warning(Messages.get("win.tileStays"),
						GBSokobanPackage.Literals.NO_ENTITY__ENTITY);
		}
	}

	@Check
	public void checkAllOnTileHolds(AllOn condition) {
		warnIfTileRejectsObjects(condition.getTile(), GBSokobanPackage.Literals.ALL_ON__TILE);
	}

	@Check
	public void checkAllOnPlayerIsRedundant(AllOn condition) {
		if (condition.getSubject() instanceof PlayerRef)
			warning(Messages.get("win.allPlayer"),
					GBSokobanPackage.Literals.ALL_ON__SUBJECT);
	}

	@Check
	public void checkGoalTileServesOneObject(Game game) {
		Map<TileDef, ObjectDef> claimed = new HashMap<>();
		for (WinCondition condition : game.getWin()) {
			TileDef tile = GoalTiles.tileOf(condition);
			ObjectDef object = GoalTiles.objectOf(condition);
			if (tile == null || object == null)
				continue;
			ObjectDef owner = claimed.putIfAbsent(tile, object);
			if (owner != null && owner != object)
				error(Messages.get("win.goalTaken", owner.getName()),
						condition, tileFeature(condition), INSIGNIFICANT_INDEX);
		}
	}

	private static EStructuralFeature tileFeature(WinCondition condition) {
		return condition instanceof AllOn
				? GBSokobanPackage.Literals.ALL_ON__TILE
				: GBSokobanPackage.Literals.SOME_ON__TILE;
	}

	@Check
	public void checkSomeOnTileHolds(SomeOn condition) {
		warnIfTileRejectsObjects(condition.getTile(), GBSokobanPackage.Literals.SOME_ON__TILE);
	}

	private void warnIfTileRejectsObjects(TileDef tile, EStructuralFeature feature) {
		if (tile == null)
			return;
		if (tile.getBehaviour() instanceof Solid)
			warning(Messages.get("win.tileBlocks"), feature);
		else if (tile.getBehaviour() instanceof Deadly || tile.getBehaviour() instanceof Fillable)
			warning(Messages.get("win.tileDestroys"), feature);
	}
}
