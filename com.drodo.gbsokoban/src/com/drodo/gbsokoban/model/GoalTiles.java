package com.drodo.gbsokoban.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.drodo.gbsokoban.gBSokoban.AllOn;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.ObjectRef;
import com.drodo.gbsokoban.gBSokoban.SomeOn;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.gBSokoban.WinCondition;

public final class GoalTiles {

	private GoalTiles() {
	}

	public static TileDef tileOf(WinCondition condition) {
		if (condition instanceof AllOn)
			return ((AllOn) condition).getTile();
		if (condition instanceof SomeOn)
			return ((SomeOn) condition).getTile();
		return null;
	}

	public static ObjectDef objectOf(WinCondition condition) {
		Object subject = null;
		if (condition instanceof AllOn)
			subject = ((AllOn) condition).getSubject();
		else if (condition instanceof SomeOn)
			subject = ((SomeOn) condition).getSubject();
		return subject instanceof ObjectRef ? ((ObjectRef) subject).getObject() : null;
	}

	public static Map<TileDef, ObjectDef> owners(Game game) {
		Map<TileDef, ObjectDef> owners = new LinkedHashMap<>();
		for (WinCondition condition : game.getWin()) {
			TileDef tile = tileOf(condition);
			ObjectDef object = objectOf(condition);
			if (tile != null && object != null)
				owners.put(tile, object);
		}
		return owners;
	}
}
