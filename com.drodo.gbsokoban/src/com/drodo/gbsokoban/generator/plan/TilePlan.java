package com.drodo.gbsokoban.generator.plan;

import com.drodo.gbsokoban.util.CSymbols;
import com.drodo.gbsokoban.util.TileKind;

public record TilePlan(String name, int index, String textureName, TileKind kind,
		String becomes) {
	public String tileSymbol() {
		return CSymbols.tile(name);
	}

	public String cellSymbol() {
		return CSymbols.cell(name);
	}
}
