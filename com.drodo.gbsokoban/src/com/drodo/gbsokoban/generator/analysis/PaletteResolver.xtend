package com.drodo.gbsokoban.generator.analysis

import com.drodo.gbsokoban.gBSokoban.PaletteDef
import com.drodo.gbsokoban.gBSokoban.RgbColor
import com.drodo.gbsokoban.gBSokoban.Texture
import com.drodo.gbsokoban.generator.plan.PalettePlan
import java.util.LinkedHashMap
import java.util.List

class PaletteResolver {

	static val GRAY_RAMP = #["RGB_WHITE", "RGB_LIGHTGRAY", "RGB_DARKGRAY", "RGB_BLACK"]

	val List<PaletteDef> backgroundPalettes
	val List<PaletteDef> spritePalettes
	val List<Texture> background

	new(List<Texture> background, List<Texture> sprites) {
		this.background = background
		this.backgroundPalettes = distinctPalettes(background)
		this.spritePalettes = distinctPalettes(sprites)
	}

	private def List<PaletteDef> distinctPalettes(List<Texture> textures) {
		val out = <PaletteDef>newArrayList
		for (texture : textures)
			if (texture.palette !== null && !out.contains(texture.palette)) out.add(texture.palette)
		out
	}

	def PalettePlan plan() {
		val color = (backgroundPalettes + spritePalettes).exists[!colors.empty]

		val backgroundPaletteOf = new LinkedHashMap<String, Integer>
		if (color)
			for (texture : background)
				backgroundPaletteOf.put(texture.name, backgroundPalettes.indexOf(texture.palette))

		new PalettePlan(
			dmgRegister(backgroundPalettes.head),
			dmgRegister(spritePalettes.head),
			dmgRegister(if (spritePalettes.size > 1) spritePalettes.get(1) else null),
			if (color) backgroundPalettes.map[rgbRamp(it) ?: GRAY_RAMP].toList else newArrayList,
			if (color) spritePalettes.map[rgbRamp(it) ?: GRAY_RAMP].toList else newArrayList,
			backgroundPaletteOf)
	}

	/**
	 * The OAM attribute byte a texture draws with. A CGB reads bits 0-2, a DMG bit 4: past the
	 * second palette a DMG uses OBP1.
	 */
	def String oamProps(Texture texture) {
		val index = spritePalettes.indexOf(texture.palette)
		if (index <= 0)
			return "0"
		"(OAMF_PAL1 | " + index + ")"
	}

	private def String dmgRegister(PaletteDef palette) {
		if (palette === null || !palette.colors.empty || palette.shades.empty)
			return PalettePlan.DEFAULT
		'''DMG_PALETTE(«palette.shades.map[literal].join(", ")»)'''.toString
	}

	private def List<String> rgbRamp(PaletteDef palette) {
		if (palette === null || palette.colors.empty) return null
		palette.colors.map[rgbExpression].toList
	}

	/** A hex value wins: an unset enum reads back as its first literal rather than null. */
	private def String rgbExpression(RgbColor color) {
		if (color.hex !== null) '''RGBHTML(0x«color.hex.substring(1)»)'''.toString else color.name.literal
	}
}
