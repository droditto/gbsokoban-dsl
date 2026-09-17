package com.drodo.gbsokoban.validation;

import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.drodo.gbsokoban.gBSokoban.Crumble;
import com.drodo.gbsokoban.gBSokoban.Deadly;
import com.drodo.gbsokoban.gBSokoban.Fillable;
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.ObjectRef;
import com.drodo.gbsokoban.gBSokoban.PlayerRef;
import com.drodo.gbsokoban.gBSokoban.Solid;

public class LegendValidator extends AbstractDeclarativeValidator {

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	@Check
	public void checkLegendHasPlayer(Game game) {
		for (LegendEntry entry : game.getLegend())
			if (entry.getSubject() instanceof PlayerRef)
				return;
		error("LEGEND needs an entry for the player",
				game, GBSokobanPackage.Literals.GAME__LEGEND, INSIGNIFICANT_INDEX,
				GBSokobanValidator.ISSUE_LEGEND_NO_PLAYER);
	}

	@Check
	public void checkLegendTileHoldsSubject(LegendEntry entry) {
		if (entry.getTile() == null)
			return;
		String who = entry.getSubject() instanceof PlayerRef ? "The player" : "This object";
		Object behaviour = entry.getTile().getBehaviour();
		if (behaviour instanceof Solid)
			warning(who + " would start inside a tile that blocks movement",
					GBSokobanPackage.Literals.LEGEND_ENTRY__TILE);
		else if (behaviour instanceof Deadly)
			warning(who + " would start on a tile that destroys what stands on it",
					GBSokobanPackage.Literals.LEGEND_ENTRY__TILE);
		else if (behaviour instanceof Crumble || behaviour instanceof Fillable)
			warning(who + " would start on a tile that disappears from under it",
					GBSokobanPackage.Literals.LEGEND_ENTRY__TILE);
	}

	@Check
	public void checkEveryObjectIsPlaced(Game game) {
		for (ObjectDef object : game.getObjects()) {
			boolean placed = false;
			for (LegendEntry entry : game.getLegend())
				if (entry.getSubject() instanceof ObjectRef
						&& ((ObjectRef) entry.getSubject()).getObject() == object)
					placed = true;
			if (!placed)
				warning("Nothing places '" + object.getName() + "'. Give it a legend entry, such as "
						+ "\"c\" = " + object.getName() + " ON <tile>",
						object, GBSokobanPackage.Literals.ENTITY__NAME, -1);
		}
	}
}
