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
		TileDef tile = null;
		if (condition instanceof AllOn)
			tile = ((AllOn) condition).getTile();
		else if (condition instanceof SomeOn)
			tile = ((SomeOn) condition).getTile();
		return tile == null || tile.eIsProxy() ? null : tile;
	}

	public static ObjectDef objectOf(WinCondition condition) {
		Object subject = null;
		if (condition instanceof AllOn)
			subject = ((AllOn) condition).getSubject();
		else if (condition instanceof SomeOn)
			subject = ((SomeOn) condition).getSubject();
		ObjectDef object = subject instanceof ObjectRef ? ((ObjectRef) subject).getObject() : null;
		return object == null || object.eIsProxy() ? null : object;
	}

	public static Map<TileDef, ObjectDef> owners(Game game) {
		Map<TileDef, ObjectDef> owners = new LinkedHashMap<>();
		for (WinCondition condition : game.getWin()) {
			TileDef tile = tileOf(condition);
			ObjectDef object = objectOf(condition);
			if (tile != null && object != null)
				owners.putIfAbsent(tile, object);
		}
		return owners;
	}
}
