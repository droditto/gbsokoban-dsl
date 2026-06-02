package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.FrameList
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage
import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.gBSokoban.PaletteBlock
import com.drodo.gbsokoban.gBSokoban.PaletteRegister
import com.drodo.gbsokoban.util.Feature
import java.util.List
import java.util.Set

import static extension com.drodo.gbsokoban.generator.GenUtils.*
import static extension com.drodo.gbsokoban.util.ModelHelpers.*

/** Emits common.h: DSL-derived constants and shared engine macros. */
class CommonGen {

	def String commonH(Game game, List<LevelParser> levels, Set<Feature> features) {
		val hasBoxes = features.contains(Feature.BOXES)
		val maxBoxes = Math.max(levels.map[boxes.size].max, 1)
		val maxGoals = Math.max(levels.map[goals.size].max, 1)
		val maxLevelW = levels.map[width].max
		val maxLevelH = levels.map[height].max
		val mtCount = game.mtCount
		val animFrames = frameCount(game)
		val moveSpeed = if (game.movementSpeed > 0) game.movementSpeed else 16
		val animSpeed = if (game.animationSpeed > 0) game.animationSpeed else 2
		val playerOam = oamProps(game.player.objectPalette)
        '''
        #ifndef COMMON_H
        #define COMMON_H

        #include <gb/gb.h>
        #include <stdint.h>
        #include <types.h>

        // ====================================================================
        // Cell geometry
        // ====================================================================

        // Each gameplay cell is a 16x16 metatile (2x2 hardware tiles).
        #define CELL_PX        16
        #define CELL_SHIFT     4
        // pixel_x = real_pixel << SUBPIXEL_SHIFT.
        #define SUBPIXEL_SHIFT 4
        // Sub-pixel units added to pixel_x per frame.
        #define MOVE_SPEED     «moveSpeed»

        // GB hardware sprite origin (x=8, y=16) plus half a cell (8) so sprites sit on cell centres.
        #define SPRITE_OFFSET_X 16
        #define SPRITE_OFFSET_Y 24

        // Grid (cell) coordinate to sub-pixel-space pixel coordinate.
        #define GRID_TO_PIXEL(g) ((int16_t)(g) << (CELL_SHIFT + SUBPIXEL_SHIFT))

        // Centring offset to write into SCX/SCY: SCX/SCY wrap at 256, so the
        // negative value shifts the level toward the right/bottom of the screen.
        #define CAMERA_CENTER_OFFSET(screen, level_px) ((int16_t)(-(((screen) - (level_px)) >> 1)))

        // ====================================================================
        // Direction encoding
        // ====================================================================

        #define DIR_DOWN  0
        #define DIR_UP    1
        #define DIR_LEFT  2
        #define DIR_RIGHT 3
        #define INVALID_DIR 0xFF

        #define DIR_FROM_DELTA(dx, dy) \
            ((dy) < 0 ? DIR_UP : (dy) > 0 ? DIR_DOWN : (dx) < 0 ? DIR_LEFT : DIR_RIGHT)

        // Out-of-range marker for a grid (cell) coordinate.
        #define INVALID_POS 0xFF

        // ====================================================================
        // Input
        // ====================================================================

        extern uint8_t joypad_current;
        extern uint8_t joypad_previous;

        #define KEY_PRESSED(k) ( joypad_current & (k))
        #define KEY_TICKED(k)  ((joypad_current & (k)) && !(joypad_previous & (k)))
        #define KEY_CONFIRM    (KEY_TICKED(J_A) || KEY_TICKED(J_START))

        // ====================================================================
        // Capacity limits
        // ====================================================================

        #define TOTAL_LEVELS  «levels.size»
        «IF hasBoxes»
        #define NUM_BOX_TYPES «objectsOf(game).size»
        #define MAX_BOXES     «maxBoxes»
        «ENDIF»
        «IF features.contains(Feature.HAS_GOALS)»
        #define MAX_GOALS     «maxGoals»
        «ENDIF»
        #define MAX_LEVEL_W   «maxLevelW»
        #define MAX_LEVEL_H   «maxLevelH»

        // ====================================================================
        // Tile and metatile IDs
        // ====================================================================

        // TILE_X is the logical index used in logic_map and tile_props lookup.
        «FOR i : 0 ..< game.tiles.entries.size»
        #define TILE_«cname(game.tiles.entries.get(i))» «i»
        «ENDFOR»

        // MT_X: index into metatile_corners[] (built at startup). Used by level_draw_metatile.
        «FOR tile : game.tiles.entries»
        #define MT_«cname(tile)» «tile.tileIdx»
        «ENDFOR»
        «IF hasBoxes»
        «FOR object : objectsOf(game)»
        #define MT_«cname(object)» «object.tileIdx»
        «ENDFOR»
        «FOR object : objectsOf(game).filter[hasExplicit(GBSokobanPackage.Literals.OBJECT_DEF__ON_GOAL_TILE_IDX)]»
        #define MT_«cname(object)»_ON_GOAL «object.onGoalTileIdx»
        «ENDFOR»
        «ENDIF»
        #define MT_COUNT «mtCount»

        #define TILE_KIND_SOLID    0
        #define TILE_KIND_FLOOR    1
        «IF features.contains(Feature.DEADLY)»
        #define TILE_KIND_DEADLY   2
        «ENDIF»
        «IF features.contains(Feature.SLIDING)»
        #define TILE_KIND_ICE      3
        «ENDIF»
        «IF features.contains(Feature.CRUMBLE)»
        #define TILE_KIND_CRUMBLE  4
        «ENDIF»
        «IF features.contains(Feature.FILLABLE)»
        #define TILE_KIND_FILLABLE 5
        «ENDIF»
        «IF features.contains(Feature.CONVEYOR)»
        #define TILE_KIND_CONVEYOR 6
        «ENDIF»

        // ====================================================================
        // Win condition types
        // ====================================================================

        «IF hasBoxes && features.contains(Feature.ALL_ON)»
        #define WIN_ALL_ON    0
        «ENDIF»
        «IF hasBoxes && features.contains(Feature.SOME_ON)»
        #define WIN_SOME_ON   1
        «ENDIF»
        «IF hasBoxes && features.contains(Feature.NO_OBJECT)»
        #define WIN_NO_OBJECT 2
        «ENDIF»
        «IF features.contains(Feature.NO_TILE)»
        #define WIN_NO_TILE   3
        «ENDIF»
        «IF features.contains(Feature.PLAYER_ON)»
        #define WIN_PLAYER_ON 4
        «ENDIF»

        // ====================================================================
        // Player animation
        // ====================================================================

        #define PLAYER_ANIM_FRAMES «animFrames»
        #define PLAYER_ANIM_SPEED  «animSpeed»
        #define PLAYER_OAM_PROPS   «playerOam»

        // ====================================================================
        // Palettes
        // ====================================================================

        «paletteDefines(game.palette)»

        // ====================================================================
        // Window overlay (HUD anchor + reserved VRAM range)
        // ====================================================================

        // 2-tile-high HUD anchored at the bottom-right of the screen.
        #define WIN_OVERLAY_X 87
        #define WIN_OVERLAY_Y 128

        // VRAM tile slot where the game tileset begins: 37 (font_min) + 1 (cursor).
        #define TILESET_VRAM_BASE 38
        «IF !features.empty»

        // Emitted for tooling (eval_loc.sh) and ROM inspection only. Runtime
        // is feature-gated at generation time.
        «FOR feature : features»
        #define FEATURE_«feature»
        «ENDFOR»
        «ENDIF»

        #endif // COMMON_H
        '''
	}

	private def String paletteDefines(PaletteBlock palette) {
		val defaultExpr = "DMG_PALETTE(DMG_WHITE, DMG_LITE_GRAY, DMG_DARK_GRAY, DMG_BLACK)"
        '''
        #define PALETTE_BGP  «palExpr(palette, PaletteRegister.BGP)  ?: defaultExpr»
        #define PALETTE_OBP0 «palExpr(palette, PaletteRegister.OBP0) ?: defaultExpr»
        #define PALETTE_OBP1 «palExpr(palette, PaletteRegister.OBP1) ?: defaultExpr»'''
	}

	private def String palExpr(PaletteBlock palette, PaletteRegister register) {
		val entry = palette?.entries?.findFirst[it.register == register]
		if (entry === null) return null
        '''DMG_PALETTE(«entry.colors.map[getLiteral].join(", ")»)'''
	}

	private def int frameCount(Game game) {
		val block = game.player.walk
		for (direction : #[block.down, block.up, block.left, block.right])
			if (direction instanceof FrameList) return direction.frames.size
		2
	}
}
