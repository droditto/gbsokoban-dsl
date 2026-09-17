package com.drodo.gbsokoban.generator.emit

import com.drodo.gbsokoban.generator.plan.GamePlan
import com.drodo.gbsokoban.generator.plan.HardwareTile
import com.drodo.gbsokoban.util.VramLayout
import java.util.Locale

class AssetEmitter {

	def String assetsH(GamePlan plan) '''
	#ifndef ASSETS_H
	#define ASSETS_H

	#include <stdint.h>
	#include <gb/cgb.h>
	#include <gb/metasprites.h>

	#define BKG_TILE_COUNT    «plan.background.tiles.size»
	#define SPRITE_TILE_COUNT «plan.sprites.tiles.size»

	// Hardware tiles making up one cell: 1 at 8x8, 4 at 16x16.
	#define TILES_PER_CELL «plan.background.tilesPerCell»

	extern const uint8_t bkg_tiles[];
	extern const uint8_t sprite_tiles[];

	// Cell id -> the TILES_PER_CELL background tiles it is drawn from, in the order
	// the engine writes them.
	extern const uint8_t cell_tiles[];

	extern const metasprite_t * const metasprites[];

	extern const uint8_t metasprite_props[];
	«IF plan.palette.hasColor»

	#define BKG_PALETTE_COUNT    «plan.palette.backgroundColors.size»
	#define SPRITE_PALETTE_COUNT «plan.palette.spriteColors.size»

	extern const palette_color_t bkg_palettes[];
	extern const palette_color_t sprite_palettes[];

	// Cell id -> the VRAM bank 1 attribute byte its tiles carry, which is the
	// background palette a Game Boy Color draws that cell with.
	extern const uint8_t cell_attrs[];
	«ENDIF»

	#endif // ASSETS_H
	'''

	def String assetsC(GamePlan plan) '''
	#include "assets.h"

	// ====================================================================
	// Background tiles
	// One 8x8 tile per line, 2bpp. Identical tiles are stored once.
	// ====================================================================

	const uint8_t bkg_tiles[] = {
	    «FOR tile : plan.background.tiles»
	    «bytes(tile)»
	    «ENDFOR»
	};

	// ====================================================================
	// Cell art
	// Indexed by cell id, with the font's share already added to every tile number.
	// ====================================================================

	const uint8_t cell_tiles[] = {
	    «FOR cell : plan.cells»
	    «labeled(plan.background.tilesOf.get(cell.textureName).map[it + VramLayout.TILESET_BASE].join(", ") + ",", cell.symbol)»
	    «ENDFOR»
	};

	// ====================================================================
	// Sprite tiles
	// ====================================================================

	const uint8_t sprite_tiles[] = {
	    «FOR tile : plan.sprites.tiles»
	    «bytes(tile)»
	    «ENDFOR»
	};

	// ====================================================================
	// Metasprites
	// Two 8x16 hardware sprites side by side at 16x16, a single one at 8x8.
	// ====================================================================

	«FOR entry : plan.sprites.firstTileOf.entrySet»
	const metasprite_t «metaspriteName(entry.key)»[] = {
	    «metaspriteBody(plan, entry.value)»
	    METASPR_TERM
	};
	«ENDFOR»

	// Animation frames and object properties store an index into this table.
	const metasprite_t * const metasprites[] = {
	    «FOR name : plan.sprites.firstTileOf.keySet»
	    «metaspriteName(name)»,
	    «ENDFOR»
	};

	const uint8_t metasprite_props[] = {«plan.metaspriteProps.join(", ")»};
	«IF plan.palette.hasColor»

	// ====================================================================
	// Color palettes
	// ====================================================================

	const palette_color_t bkg_palettes[] = {
	    «FOR entry : plan.palette.backgroundColors SEPARATOR ","»
	    «entry.join(", ")»
	    «ENDFOR»
	};

	const palette_color_t sprite_palettes[] = {
	    «FOR entry : plan.palette.spriteColors SEPARATOR ","»
	    «entry.join(", ")»
	    «ENDFOR»
	};

	const uint8_t cell_attrs[] = {
	    «FOR cell : plan.cells SEPARATOR ", "»«plan.palette.attributeOf(cell.textureName)»«ENDFOR»
	};
	«ENDIF»
	'''

	private def String labeled(String row, String label) {
		String.format("%-24s// %s", row, label)
	}

	private def String bytes(HardwareTile tile) {
		tile.bytes.map[String.format("0x%02X", it)].join(", ") + ","
	}

	private def String metaspriteName(String texture) {
		"ms_" + texture.toLowerCase(Locale.ROOT)
	}

	private def String metaspriteBody(GamePlan plan, Integer firstTile) {
		val half = plan.geometry.halfPx
		if (plan.sprites.usesTallSprites)
			'''METASPR_ITEM(«-half», «-half», «firstTile», 0), METASPR_ITEM(0, 8, «firstTile + 2», 0),'''.toString
		else
			'''METASPR_ITEM(«-half», «-half», «firstTile», 0),'''.toString
	}
}
