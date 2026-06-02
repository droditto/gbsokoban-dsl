package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.util.Feature
import java.util.Set

import static extension com.drodo.gbsokoban.generator.GenUtils.*
import static extension com.drodo.gbsokoban.util.ModelHelpers.*

/** Emits box.h and box.c: state, type properties, query and draw helpers. */
class BoxGen {

	def String boxH(Set<Feature> features) {
		val hasOnGoal = features.contains(Feature.ON_GOAL)
		val hasDestroy = features.contains(Feature.DESTROYABLE)
        '''
        #ifndef BOX_H
        #define BOX_H

        #include "common.h"

        // Box-type properties (one per object kind), indexed by Box.group.
        typedef struct {
            uint8_t tile_idx;
            «IF hasOnGoal»
            uint8_t on_goal_tile_idx;
            «ENDIF»
            uint8_t sprite_idx;
            uint8_t oam_props;
        } BoxTypeProps;

        // A moving box is drawn as a sprite (smooth motion). When it stops it
        // gets committed to BG (frees OAM, cheaper to render). draw_as_sprite,
        // bg_commit_pending and clear_origin_pending coordinate that handover.
        typedef struct {
            uint8_t  grid_x, grid_y;
            int16_t  pixel_x, pixel_y;
            int8_t   move_dx, move_dy;
            int16_t  move_steps_remaining;
            «IF hasOnGoal»
            uint8_t  on_goal;
            «ENDIF»
            «IF hasDestroy»
            uint8_t  destroyed;
            «ENDIF»
            uint8_t  group;
            uint8_t  draw_as_sprite;
            uint8_t  bg_commit_pending;
            uint8_t  clear_origin_pending;
        } Box;

        extern const BoxTypeProps box_type_props[NUM_BOX_TYPES];
        extern Box   boxes[MAX_BOXES];
        extern uint8_t num_boxes;

        Box   *box_find_at(uint8_t gx, uint8_t gy);
        uint8_t box_can_move_to(uint8_t gx, uint8_t gy);

        void box_start_move(Box *box, int8_t dx, int8_t dy);
        void box_animate_step(Box *box);
        void box_finish_move(Box *box);

        void    box_draw_to_bg(const Box *box);
        void    box_flush_all_to_bg(void);
        uint8_t box_draw_sprites(uint8_t oam_slot);

        #endif // BOX_H
        '''
	}

	def String boxC(Game game, Set<Feature> features) {
		val hasOnGoal = features.contains(Feature.ON_GOAL)
		val hasDestroy = features.contains(Feature.DESTROYABLE)
        '''
        #include "box.h"
        #include "camera.h"
        #include "common.h"
        #include "level.h"
        #include "sprites.h"
        #include <gb/gb.h>
        #include <gb/metasprites.h>

        // ====================================================================
        // State
        // ====================================================================

        const BoxTypeProps box_type_props[NUM_BOX_TYPES] = {
            «FOR object : objectsOf(game) SEPARATOR ','»
            {.tile_idx = «mtName(object, false)»«IF hasOnGoal», .on_goal_tile_idx = «mtName(object, true)»«ENDIF», .sprite_idx = «object.spriteIdx», .oam_props = «object.objectPalette.oamProps»}
            «ENDFOR»
        };

        Box     boxes[MAX_BOXES];
        uint8_t num_boxes;

        // ====================================================================
        // Query
        // ====================================================================

        Box *box_find_at(uint8_t gx, uint8_t gy) {
            uint8_t i;
            for (i = 0; i < num_boxes; i++) {
                if («IF hasDestroy»!boxes[i].destroyed && «ENDIF»boxes[i].grid_x == gx && boxes[i].grid_y == gy)
                    return &boxes[i];
            }
            return NULL;
        }

        uint8_t box_can_move_to(uint8_t gx, uint8_t gy) {
            if (gx >= level_width || gy >= level_height)        return FALSE;
            if (!TILE_IS_PASSABLE(LOGIC_TILE(gx, gy)))          return FALSE;
            if (box_find_at(gx, gy))                            return FALSE;
            return TRUE;
        }

        // ====================================================================
        // Movement
        // ====================================================================

        void box_start_move(Box *box, int8_t dx, int8_t dy) {
            box->draw_as_sprite       = TRUE;
            box->bg_commit_pending    = FALSE;
            box->clear_origin_pending = TRUE;
            box->move_dx              = dx;
            box->move_dy              = dy;
            box->move_steps_remaining = CELL_PX << SUBPIXEL_SHIFT;
            «IF hasOnGoal»
            box->on_goal = FALSE;
            «ENDIF»
        }

        void box_animate_step(Box *box) {
            // Repaint the origin cell on the first step so the moving sprite
            // doesn't leave a ghost copy behind.
            if (box->clear_origin_pending) {
                uint8_t origin_tile = LOGIC_TILE(box->grid_x, box->grid_y);
                level_draw_metatile(box->grid_x, box->grid_y, TILE_TO_METATILE(origin_tile));
                box->clear_origin_pending = FALSE;
            }
            if      (box->move_dx > 0) box->pixel_x += MOVE_SPEED;
            else if (box->move_dx < 0) box->pixel_x -= MOVE_SPEED;
            if      (box->move_dy > 0) box->pixel_y += MOVE_SPEED;
            else if (box->move_dy < 0) box->pixel_y -= MOVE_SPEED;
            box->move_steps_remaining -= MOVE_SPEED;
        }

        void box_finish_move(Box *box) {
            box->grid_x  += box->move_dx;
            box->grid_y  += box->move_dy;
            box->pixel_x  = GRID_TO_PIXEL(box->grid_x);
            box->pixel_y  = GRID_TO_PIXEL(box->grid_y);
            box->move_dx  = 0;
            box->move_dy  = 0;
            box->move_steps_remaining = 0;
        }

        // ====================================================================
        // Rendering
        // ====================================================================

        void box_draw_to_bg(const Box *box) {
            const BoxTypeProps *props = &box_type_props[box->group];
            «IF hasOnGoal»
            uint8_t metatile = box->on_goal ? props->on_goal_tile_idx : props->tile_idx;
            «ELSE»
            uint8_t metatile = props->tile_idx;
            «ENDIF»
            level_draw_metatile(box->grid_x, box->grid_y, metatile);
        }

        void box_flush_all_to_bg(void) {
            uint8_t i;
            for (i = 0; i < num_boxes; i++) {
                if (!boxes[i].bg_commit_pending) continue;
                boxes[i].bg_commit_pending = FALSE;
                boxes[i].draw_as_sprite    = FALSE;
                «IF hasDestroy»if (boxes[i].destroyed) continue;«ENDIF»
                box_draw_to_bg(&boxes[i]);
            }
        }

        uint8_t box_draw_sprites(uint8_t oam_slot) {
            uint8_t i;
            for (i = 0; i < num_boxes; i++) {
                const BoxTypeProps *props;
                int16_t screen_x, screen_y;
                if («IF hasDestroy»boxes[i].destroyed || «ENDIF»!boxes[i].draw_as_sprite)
                    continue;
                props    = &box_type_props[boxes[i].group];
                screen_x = camera_to_screen_x(boxes[i].pixel_x >> SUBPIXEL_SHIFT);
                screen_y = camera_to_screen_y(boxes[i].pixel_y >> SUBPIXEL_SHIFT);
                oam_slot += move_metasprite_ex(sprites_metasprites[props->sprite_idx], 0,
                                               props->oam_props, oam_slot,
                                               (uint8_t)screen_x + SPRITE_OFFSET_X,
                                               (uint8_t)screen_y + SPRITE_OFFSET_Y);
            }
            return oam_slot;
        }
        '''
	}

}
