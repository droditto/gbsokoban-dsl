package com.drodo.gbsokoban.util;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.TileDef;

/** Defensive accessors for optional Game children plus the explicit-feature predicate. */
public final class ModelHelpers {

	private ModelHelpers() {}

	/** True when {@code feature} was assigned explicitly in the source (not just defaulted). */
	public static boolean hasExplicit(EObject host, EStructuralFeature feature) {
		return !NodeModelUtils.findNodesForFeature(host, feature).isEmpty();
	}

	public static List<TileDef> tilesOf(Game game) {
		return game.getTiles() != null ? game.getTiles().getEntries() : Collections.emptyList();
	}

	public static List<ObjectDef> objectsOf(Game game) {
		return game.getObjects() != null ? game.getObjects().getEntries() : Collections.emptyList();
	}

	public static List<LegendEntry> legendEntriesOf(Game game) {
		return game.getLegend() != null ? game.getLegend().getEntries() : Collections.emptyList();
	}
}
