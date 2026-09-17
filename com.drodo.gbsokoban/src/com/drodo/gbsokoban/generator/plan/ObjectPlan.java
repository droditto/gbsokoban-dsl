package com.drodo.gbsokoban.generator.plan;

import com.drodo.gbsokoban.util.CSymbols;

public record ObjectPlan(String name, String textureName, String goalTextureName, String oamProps) {

	public String cellSymbol() {
		return CSymbols.cell(name);
	}

	public String onGoalCellSymbol() {
		return goalTextureName == null ? cellSymbol() : CSymbols.onGoalCell(name);
	}

	public boolean hasOwnGoalArt() {
		return goalTextureName != null;
	}
}
