package com.drodo.gbsokoban.generator.plan;

import java.util.List;
import java.util.Map;

import com.drodo.gbsokoban.util.Palettes;

public record PalettePlan(String bgp, String obp0, String obp1,
		List<List<String>> backgroundColors, List<List<String>> spriteColors,
		Map<String, Integer> backgroundPaletteOf) {
	public static final String DEFAULT =
			"DMG_PALETTE(" + String.join(", ", Palettes.DEFAULT_SHADES) + ")";

	public boolean hasColor() {
		return !backgroundColors.isEmpty();
	}

	/** The attribute byte a cell drawn with this texture carries in VRAM bank 1. */
	public int attributeOf(String textureName) {
		return backgroundPaletteOf.get(textureName);
	}
}
