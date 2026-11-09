package com.drodo.gbsokoban.validation;

import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.drodo.gbsokoban.gBSokoban.Deadly;
import com.drodo.gbsokoban.gBSokoban.Fillable;
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.ObjectRef;
import com.drodo.gbsokoban.gBSokoban.PlayerRef;
import com.drodo.gbsokoban.gBSokoban.Solid;
import com.drodo.gbsokoban.util.Messages;

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
		error(Messages.get("legend.noPlayer"),
				game, GBSokobanPackage.Literals.GAME__LEGEND, INSIGNIFICANT_INDEX,
				GBSokobanValidator.ISSUE_LEGEND_NO_PLAYER);
	}

	@Check
	public void checkLegendTileHoldsSubject(LegendEntry entry) {
		if (entry.getTile() == null)
			return;
		Object behaviour = entry.getTile().getBehaviour();
		String modifier = behaviour instanceof Solid ? "BLOCKS"
				: behaviour instanceof Deadly ? "DESTROYS"
				: behaviour instanceof Fillable ? "FILLS INTO" : null;
		if (modifier == null)
			return;
		if (entry.getSubject() instanceof ObjectRef) {
			ObjectDef object = ((ObjectRef) entry.getSubject()).getObject();
			if (object != null && !object.eIsProxy())
				warning(Messages.get("legend.objectStartsOn", object.getName(), modifier),
						GBSokobanPackage.Literals.LEGEND_ENTRY__TILE);
		} else
			warning(Messages.get("legend.playerStartsOn", modifier), GBSokobanPackage.Literals.LEGEND_ENTRY__TILE);
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
				warning(Messages.get("legend.notPlaced"),
						object, GBSokobanPackage.Literals.ENTITY__NAME, -1);
		}
	}
}
