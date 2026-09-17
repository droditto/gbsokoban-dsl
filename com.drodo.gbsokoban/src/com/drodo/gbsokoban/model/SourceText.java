package com.drodo.gbsokoban.model;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

public final class SourceText {

	private SourceText() {
	}

	/**
	 * True when the feature was written in the source: EMF cannot tell "the author wrote 0"
	 * from "wrote nothing".
	 */
	public static boolean wasWritten(EObject host, EStructuralFeature feature) {
		return !NodeModelUtils.findNodesForFeature(host, feature).isEmpty();
	}
}
