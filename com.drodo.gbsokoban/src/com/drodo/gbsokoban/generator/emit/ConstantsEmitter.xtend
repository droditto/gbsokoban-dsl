package com.drodo.gbsokoban.generator.emit

import com.drodo.gbsokoban.gBSokoban.Direction
import com.drodo.gbsokoban.generator.plan.GamePlan
import com.drodo.gbsokoban.model.Directions
import com.drodo.gbsokoban.util.CSymbols
import com.drodo.gbsokoban.util.CellGeometry
import com.drodo.gbsokoban.util.Feature
import com.drodo.gbsokoban.util.ScreenText
import com.drodo.gbsokoban.util.TileKind
import com.drodo.gbsokoban.util.VramLayout
import com.drodo.gbsokoban.util.WinKind

class ConstantsEmitter {

	def String commonH(GamePlan plan) {
		val geometry = plan.geometry
		'''
		#ifndef COMMON_H
		#define COMMON_H

		#include <gb/gb.h>
		#include <stdint.h>
		#include <types.h>

		// ====================================================================
		// Cell geometry
		// ====================================================================

		// A cell is «geometry.px»x«geometry.px», drawn as «geometry.tilesPerSide»x«geometry.tilesPerSide» tiles of 8x8.
		#define CELL_PX         «geometry.px»
		#define CELL_SHIFT      «geometry.pixelShift»
		#define TILES_PER_SIDE  «geometry.tilesPerSide»
		// pixel_x = real_pixel << SUBPIXEL_SHIFT.
		#define SUBPIXEL_SHIFT  «CellGeometry.SUBPIXEL_SHIFT»
		// Sub-pixel units added to pixel_x per frame.
		#define MOVE_SPEED      «plan.moveSpeed»

		// GB hardware sprite origin (x=8, y=16) plus half a cell so sprites sit on cell centres.
		#define SPRITE_OFFSET_X «geometry.spriteOffsetX»
		#define SPRITE_OFFSET_Y «geometry.spriteOffsetY»

		// Grid (cell) coordinate to sub-pixel-space pixel coordinate.
		#define GRID_TO_PIXEL(g) ((int16_t)(g) << (CELL_SHIFT + SUBPIXEL_SHIFT))

		// SCX/SCY wrap at 256, so a negative offset shifts the level right and down.
		#define CAMERA_CENTER_OFFSET(screen, level_px) ((int16_t)(-(((screen) - (level_px)) >> 1)))

		// ====================================================================
		// Direction encoding
		// ====================================================================

		«FOR d : Direction.values»
		#define «String.format("%-9s %d", Directions.symbol(d), d.ordinal)»
		«ENDFOR»
		#define INVALID_DIR 0xFF

		#define DIR_FROM_DELTA(dx, dy) \
		    ((dy) < 0 ? DIR_UP : (dy) > 0 ? DIR_DOWN : (dx) < 0 ? DIR_LEFT : DIR_RIGHT)

		// Out-of-range marker for a grid (cell) coordinate.
		#define INVALID_POS 0xFF

		// ====================================================================
		// Game state
		// Shared so no module includes another just to name a state.
		// ====================================================================

		typedef enum {
		    «IF plan.has(Feature.TITLE_SCREEN)»
		    STATE_TITLE,
		    «ENDIF»
		    STATE_GAME,
		    STATE_PAUSE_MENU,
		    «IF plan.has(Feature.LEVEL_SELECT)»
		    STATE_LEVEL_SELECT,
		    «ENDIF»
		    STATE_LEVEL_CLEAR,
		    «IF plan.has(Feature.END_SCREEN)»
		    STATE_ALL_LEVELS_COMPLETE,
		    «ENDIF»
		    STATE_COUNT
		} GameState;

		typedef void (*StateFunction)(void);

		typedef struct {
		    StateFunction init;
		    StateFunction update;
		} StateDef;

		extern GameState current_state;
		extern GameState previous_state;
		extern uint8_t   current_level;

		// Re-enters a state even if already running, by making the main loop see a change.
		#define STATE_GOTO(s) (previous_state = STATE_COUNT, current_state = (s))

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

		#define TOTAL_LEVELS  «plan.capacities.totalLevels»
		«IF plan.has(Feature.BOXES)»
		#define NUM_BOX_TYPES «plan.capacities.numBoxTypes»
		#define MAX_BOXES     «plan.capacities.maxBoxes»
		«ENDIF»
		«IF plan.has(Feature.HAS_GOALS)»
		#define MAX_GOALS     «plan.capacities.maxGoals»
		«ENDIF»
		#define MAX_LEVEL_W   «plan.capacities.maxLevelW»
		#define MAX_LEVEL_H   «plan.capacities.maxLevelH»

		// ====================================================================
		// Tile and cell IDs
		// ====================================================================

		// TILE_X is the logical index used in logic_map and tile_props lookup.
		«FOR tile : plan.tiles»
		#define «tile.tileSymbol» «tile.index»
		«ENDFOR»

		// CELL_X indexes cell_tiles[]: the tiles, then the objects at rest, then on their goal.
		«FOR cell : plan.cells.indexed»
		#define «cell.value.symbol» «cell.key»
		«ENDFOR»

		// What the background map is cleared to, so nothing of the last level shows
		// through.
		#define BLANK_CELL «CSymbols.cell(CSymbols.OUTSIDE)»

		«FOR kind : TileKind.values»
		#define «String.format("%-18s %d", kind.symbol, kind.ordinal)»
		«ENDFOR»

		// ====================================================================
		// Win condition types
		// ====================================================================

		«FOR kind : WinKind.values»
		#define «String.format("%-13s %d", kind.symbol, kind.ordinal)»
		«ENDFOR»

		// ====================================================================
		// Player animation
		// ====================================================================

		// As many digits as the level count needs.
		#define HUD_STAGE_DIGITS «digitsOf(plan.capacities.totalLevels)»

		#define PLAYER_ANIM_FRAMES «plan.animFrames»
		#define PLAYER_ANIM_SPEED  «plan.animSpeed»

		// ====================================================================
		// Palettes
		// ====================================================================

		#define PALETTE_BGP  «plan.palette.bgp»
		#define PALETTE_OBP0 «plan.palette.obp0»
		#define PALETTE_OBP1 «plan.palette.obp1»

		// ====================================================================
		// Window overlay (HUD anchor + reserved VRAM range)
		// ====================================================================

		// The window is anchored bottom-right, so the HUD is a bar across the bottom and
		// PLAY_H is what is left.
		#define WIN_OVERLAY_X 7
		#define WIN_OVERLAY_Y «ScreenText.PLAY_PIXEL_HEIGHT»
		// A panel takes a row the level had, which only happens while it is paused.
		#define WIN_PANEL_Y   «ScreenText.PIXEL_HEIGHT - ScreenText.PANEL_ROWS * 8»
		#define PLAY_H        «ScreenText.PLAY_PIXEL_HEIGHT»

		#define UI_CURSOR_TILE    «VramLayout.FONT_TILES»
		#define TILESET_VRAM_BASE «VramLayout.TILESET_BASE»

		#define BKG_MAP_TILES     «VramLayout.BKG_MAP_TILES»

		#endif // COMMON_H
		'''
	}

	private def int digitsOf(int value) {
		if (value < 10) 1 else if (value < 100) 2 else 3
	}
}
