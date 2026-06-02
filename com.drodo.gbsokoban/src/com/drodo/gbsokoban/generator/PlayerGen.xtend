package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.AnimBlock
import com.drodo.gbsokoban.gBSokoban.DirAnim
import com.drodo.gbsokoban.gBSokoban.Direction
import com.drodo.gbsokoban.gBSokoban.FlipAnim
import com.drodo.gbsokoban.gBSokoban.FrameList
import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.gBSokoban.PlayerDef
import com.drodo.gbsokoban.util.Feature
import java.util.Set

/** Emits player.h and player.c: state, movement lifecycle, sprite rendering. */
class PlayerGen {

    def String playerH(Set<Feature> features) '''
    #ifndef PLAYER_H
    #define PLAYER_H

    #include <stdint.h>

    // Index into <name>_frames[]. Set by game logic, read by player_draw.
    #define PLAYER_ANIM_WALK 0
    #define PLAYER_ANIM_PUSH 1
    «IF features.contains(Feature.PULL)»
    #define PLAYER_ANIM_PULL 2
    «ENDIF»

    typedef struct {
        uint8_t  grid_x, grid_y;
        int16_t  pixel_x, pixel_y;
        int8_t   move_dx, move_dy;
        int16_t  move_steps_remaining;
        uint8_t  anim_mode;
        uint8_t  direction;
        uint8_t  anim_frame;
    } Player;

    extern Player player;

    void player_init(uint8_t gx, uint8_t gy);
    void player_load_sprites(void);

    void player_start_move(int8_t dx, int8_t dy);
    void player_animate_step(void);
    void player_finish_move(void);

    // Returns the next free OAM slot so callers can chain box_draw_sprites().
    uint8_t player_draw(uint8_t oam_slot);
    «IF features.contains(Feature.LEVEL_SELECT)»
    void    player_draw_preview(uint8_t screen_x, uint8_t screen_y);
    «ENDIF»

    #endif // PLAYER_H
    '''

	def String playerC(Game game, Set<Feature> features) {
		val player = game.player
		val usedMirrors = collectMirrors(player.walk, features, player)
        '''
        #include "player.h"
        #include "camera.h"
        #include "common.h"
        #include "sprites.h"
        #include <gb/gb.h>
        #include <gb/metasprites.h>
        #include <string.h>

        // ====================================================================
        // Sprite-rendering constants
        // ====================================================================

        #define SPR_BASE 0

        // 2-bit field stored in <name>_mirror[]: bit 0 = flipX, bit 1 = flipY.
        #define MIRROR_NONE 0
        #define MIRROR_X    1
        #define MIRROR_Y    2
        #define MIRROR_XY   3

        // ====================================================================
        // State
        // ====================================================================

        Player player;

        «animArrays("walk", player.walk)»
        «IF features.contains(Feature.PUSH_ANIM)»
        «animArrays("push", player.push)»
        «ENDIF»
        «IF features.contains(Feature.PULL_ANIM)»
        «animArrays("pull", player.pull)»
        «ENDIF»

        // ====================================================================
        // Sprite rendering
        // ====================================================================

        // Dispatches to the move_metasprite_* variant matching `mirror`.
        static uint8_t draw_player_sprite(uint8_t metasprite_idx, uint8_t mirror,
                                          uint8_t screen_x, uint8_t screen_y, uint8_t oam_slot) {
            «drawPlayerSpriteBody(usedMirrors)»
        }

        // ====================================================================
        // Lifecycle
        // ====================================================================

        void player_init(uint8_t gx, uint8_t gy) {
            memset(&player, 0, sizeof(Player));
            player.grid_x  = gx;
            player.grid_y  = gy;
            player.pixel_x = GRID_TO_PIXEL(gx);
            player.pixel_y = GRID_TO_PIXEL(gy);
        }

        void player_load_sprites(void) {
            set_sprite_data(0, sprites_TILE_COUNT, sprites_tiles);
        }

        // ====================================================================
        // Movement
        // ====================================================================

        void player_start_move(int8_t dx, int8_t dy) {
            player.move_dx              = dx;
            player.move_dy              = dy;
            player.move_steps_remaining = CELL_PX << SUBPIXEL_SHIFT;
            // Start on the second animation frame so motion is visible from
            // the first rendered frame. The modulo guards single-frame anims.
            player.anim_frame           = (1 % PLAYER_ANIM_FRAMES) << SUBPIXEL_SHIFT;
        }

        void player_animate_step(void) {
            if      (player.move_dx > 0) player.pixel_x += MOVE_SPEED;
            else if (player.move_dx < 0) player.pixel_x -= MOVE_SPEED;
            if      (player.move_dy > 0) player.pixel_y += MOVE_SPEED;
            else if (player.move_dy < 0) player.pixel_y -= MOVE_SPEED;
            player.move_steps_remaining -= MOVE_SPEED;

            player.anim_frame += PLAYER_ANIM_SPEED;
            if ((player.anim_frame >> SUBPIXEL_SHIFT) >= PLAYER_ANIM_FRAMES)
                player.anim_frame = 0;
        }

        void player_finish_move(void) {
            player.grid_x  += player.move_dx;
            player.grid_y  += player.move_dy;
            player.pixel_x  = GRID_TO_PIXEL(player.grid_x);
            player.pixel_y  = GRID_TO_PIXEL(player.grid_y);
            player.move_dx  = 0;
            player.move_dy  = 0;
            player.move_steps_remaining = 0;
            player.anim_frame           = 0;
        }

        // ====================================================================
        // Public draw API
        // ====================================================================

        uint8_t player_draw(uint8_t oam_slot) {
            int16_t screen_x = camera_to_screen_x(player.pixel_x >> SUBPIXEL_SHIFT);
            int16_t screen_y = camera_to_screen_y(player.pixel_y >> SUBPIXEL_SHIFT);
            const uint8_t *frames       = walk_frames[player.direction];
            const uint8_t *mirror_table = walk_mirror;

            «IF features.contains(Feature.PUSH_ANIM)»
            if (player.anim_mode == PLAYER_ANIM_PUSH) {
                frames       = push_frames[player.direction];
                mirror_table = push_mirror;
            }
            «ENDIF»
            «IF features.contains(Feature.PULL_ANIM)»
            if (player.anim_mode == PLAYER_ANIM_PULL) {
                frames       = pull_frames[player.direction];
                mirror_table = pull_mirror;
            }
            «ENDIF»

            return draw_player_sprite(frames[player.anim_frame >> SUBPIXEL_SHIFT],
                                      mirror_table[player.direction],
                                      (uint8_t)screen_x + SPRITE_OFFSET_X,
                                      (uint8_t)screen_y + SPRITE_OFFSET_Y,
                                      oam_slot);
        }

        «IF features.contains(Feature.LEVEL_SELECT)»
        void player_draw_preview(uint8_t screen_x, uint8_t screen_y) {
            uint8_t metasprite_idx = walk_frames[DIR_DOWN][0];
            uint8_t oam_used = draw_player_sprite(metasprite_idx, MIRROR_NONE,
                                                  screen_x + SPRITE_OFFSET_X,
                                                  screen_y + SPRITE_OFFSET_Y,
                                                  SPR_BASE);
            hide_sprites_range(oam_used, MAX_HARDWARE_SPRITES);
        }
        «ENDIF»
        '''
	}

	// ====================================================================
	// Animation table generation
	// ====================================================================

	private def String animArrays(String animName, AnimBlock block) {
		val directions = #[block.down, block.up, block.left, block.right]
		val resolvedAnims = directions.map[resolveAnim(it, block)]
        '''
        static const uint8_t «animName»_frames[4][PLAYER_ANIM_FRAMES] = {
            «FOR resolved : resolvedAnims SEPARATOR ','»
            {«resolved.frames.join(", ")»}
            «ENDFOR»
        };
        static const uint8_t «animName»_mirror[4] = {«resolvedAnims.map[mirrorBits].join(", ")»};
        '''
	}

	/** Resolves a DirAnim to (frames, mirror bits) by chasing flip references. */
	private def AnimResolution resolveAnim(DirAnim direction, AnimBlock block) {
		if (direction instanceof FrameList)
			return new AnimResolution(direction.frames.map[intValue], 0)
		val flip = direction as FlipAnim
		val source = switch (flip.source) {
			case Direction.DOWN: block.down
			case Direction.UP: block.up
			case Direction.LEFT: block.left
			default: block.right
		}
		val base = resolveAnim(source, block)
		val mirrorBit = if (flip.flipX) 1 else 2
		return new AnimResolution(base.frames, base.mirrorBits.bitwiseOr(mirrorBit))
	}

	/** Distinct mirror values used across walk + push + pull. */
	private def Set<Integer> collectMirrors(AnimBlock walk, Set<Feature> features, PlayerDef player) {
		val mirrors = newLinkedHashSet
		for (block : #{walk,
						if (features.contains(Feature.PUSH_ANIM)) player.push else null,
						if (features.contains(Feature.PULL_ANIM)) player.pull else null}.filterNull) {
			for (direction : #[block.down, block.up, block.left, block.right]) {
				mirrors.add(resolveAnim(direction, block).mirrorBits)
			}
		}
		mirrors
	}

	/** Body of draw_player_sprite, branching only on the mirror cases actually used. */
	private def String drawPlayerSpriteBody(Set<Integer> usedMirrors) {
		val args = "sprites_metasprites[metasprite_idx], SPR_BASE, PLAYER_OAM_PROPS, oam_slot, screen_x, screen_y"
		if (usedMirrors.size == 1 && usedMirrors.contains(0))
            '''
            (void)mirror;
            return move_metasprite_ex(«args»);'''
		else
            '''
            switch (mirror) {
            «IF usedMirrors.contains(1)»
                case MIRROR_X:
                    return move_metasprite_flipx(«args»);
            «ENDIF»
            «IF usedMirrors.contains(2)»
                case MIRROR_Y:
                    return move_metasprite_flipy(«args»);
            «ENDIF»
            «IF usedMirrors.contains(3)»
                case MIRROR_XY:
                    return move_metasprite_flipxy(«args»);
            «ENDIF»
                default:
                    return move_metasprite_ex(«args»);
                }'''
	}
}
