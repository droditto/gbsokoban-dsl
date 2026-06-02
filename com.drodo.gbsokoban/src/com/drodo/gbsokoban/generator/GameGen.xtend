package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.AllOn
import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.gBSokoban.NoEntity
import com.drodo.gbsokoban.gBSokoban.ObjectDef
import com.drodo.gbsokoban.gBSokoban.ObjectRef
import com.drodo.gbsokoban.gBSokoban.PlayerRef
import com.drodo.gbsokoban.gBSokoban.SomeOn
import com.drodo.gbsokoban.gBSokoban.TileDef
import com.drodo.gbsokoban.gBSokoban.WinCondition
import com.drodo.gbsokoban.util.Feature
import java.util.Set

import static extension com.drodo.gbsokoban.generator.GenUtils.*

/** Emits game.h and game.c: main loop, movement, tile effects, win check. */
class GameGen {

    def String gameH() '''
    #ifndef GAME_H
    #define GAME_H

    #include <stdint.h>

    extern uint16_t move_count;

    void game_init(void);
    void game_resume(void);
    void game_update(void);
    void game_render_sprites(void);

    #endif // GAME_H
    '''

	def String gameC(Game game, Set<Feature> features) {
		val hasPull = features.contains(Feature.PULL)
		val hasSliding = features.contains(Feature.SLIDING)
		val hasDeadly = features.contains(Feature.DEADLY)
		val hasDestroy = features.contains(Feature.DESTROYABLE)
		val hasFillable = features.contains(Feature.FILLABLE)
		val hasCrumble = features.contains(Feature.CRUMBLE)
		val hasConveyor = features.contains(Feature.CONVEYOR)
		val hasTitleScreen = features.contains(Feature.TITLE_SCREEN)
		val hasEndScreen = features.contains(Feature.END_SCREEN)
		val hasSound = features.contains(Feature.SOUND)
		val hasBoxes = features.contains(Feature.BOXES)
		val sounds = if (hasSound) game.sounds else null
		val sndPlayerMove = sounds?.playerMove !== null
		val sndPlayerBlocked = sounds?.playerBlocked !== null
		val sndBoxPush = sounds?.boxPush !== null
		val sndBoxOnGoal = sounds?.boxOnGoal !== null
		val sndBoxDestroyed = sounds?.boxDestroyed !== null
		val sndCrumble = sounds?.crumble !== null
		val sndLevelComplete = sounds?.levelComplete !== null
		val winConditions = game.win.conditions
		val hasAllOn = features.contains(Feature.ALL_ON)
		val hasSomeOn = features.contains(Feature.SOME_ON)
		val hasPlayerOn = features.contains(Feature.PLAYER_ON)
		val hasNoObject = features.contains(Feature.NO_OBJECT)
		val hasNoTile = features.contains(Feature.NO_TILE)
		val hasOnGoal = features.contains(Feature.ON_GOAL)
		val needsGoalLanding = features.contains(Feature.HAS_GOALS) && (hasOnGoal || sndBoxOnGoal)
        '''
        #include "game.h"
        «IF hasBoxes»
        #include "box.h"
        «ENDIF»
        #include "camera.h"
        #include "common.h"
        #include "level.h"
        #include "menus.h"
        #include "player.h"
        «IF hasSound»
        #include "sound.h"
        «ENDIF»
        #include "ui.h"
        #include <gb/gb.h>
        #include <gb/metasprites.h>

        // ====================================================================
        // State machine
        // ====================================================================

        // PHASE_IDLE reads input. PHASE_PLAYER_MOVING animates a move. When it
        // finishes, finish_player_move resolves the push and check_player_tile
        // fires post-move effects (death, ice, conveyor) before returning to
        // idle. The two SLIDING phases handle ice chains.

        #define PHASE_IDLE 0
        #define PHASE_PLAYER_MOVING 1
        «IF hasSliding»
        «IF hasBoxes»
        #define PHASE_BOX_SLIDING    2
        «ENDIF»
        #define PHASE_PLAYER_SLIDING 3
        «ENDIF»

        // ====================================================================
        // State
        // ====================================================================

        uint16_t move_count;

        static uint8_t game_phase;
        «IF sndPlayerBlocked»
        static uint8_t last_blocked_dir;
        «ENDIF»
        «IF hasBoxes»
        static Box   *pushing_box;
        «IF hasPull»
        static Box   *pulling_box;
        «ENDIF»
        «IF hasSliding»
        static Box   *sliding_box;
        static uint8_t slide_carries_player_motion;
        «ENDIF»
        «ENDIF»
        «IF hasCrumble»
        // Crumble fires when the player LEAVES the cell. We remember it on
        // arrival and collapse it on the next move.
        static uint8_t held_crumble_x;
        static uint8_t held_crumble_y;
        «ENDIF»

        // ====================================================================
        // Forward declarations
        // ====================================================================

        «IF hasSliding»
        «IF hasBoxes»
        static uint8_t try_start_box_slide(Box *box, int8_t prev_dx, int8_t prev_dy);
        «ENDIF»
        static void    start_player_slide(int8_t dx, int8_t dy);
        «ENDIF»
        static void    check_player_tile(int8_t pdx, int8_t pdy);
        static void    go_idle(void);
        «IF hasDeadly»
        static uint8_t player_check_death(void);
        «ENDIF»

        // ====================================================================
        // Win conditions
        // ====================================================================

        const WinCondition win_conditions[] = {
            «FOR wc : winConditions SEPARATOR ','»
            «winEntry(wc, game)»
            «ENDFOR»
        };
        const uint8_t num_win_conditions = «winConditions.size»;

        «IF hasNoTile || hasPlayerOn»
        static uint8_t no_tile_left(uint8_t tile_id) {
            uint16_t i;
            for (i = 0; i < level_total_cells; i++) {
                if (logic_map[i] == tile_id)
                    return FALSE;
            }
            return TRUE;
        }

        «ENDIF»
        static uint8_t level_is_complete(void) {
            uint8_t c«IF (hasAllOn || hasSomeOn) && hasBoxes», g«ENDIF»;

            if (num_win_conditions == 0)
                return FALSE;

            for (c = 0; c < num_win_conditions; c++) {
                const WinCondition *wc = &win_conditions[c];
                switch (wc->type) {
                «IF hasAllOn && hasBoxes»
                case WIN_ALL_ON: {
                    uint8_t b;
                    for (g = 0; g < num_goals; g++) {
                        if (goals[g].group != wc->group)
                            continue;
                        for (b = 0; b < num_boxes; b++) {
                            «IF hasDestroy»if (boxes[b].destroyed) continue;«ENDIF»
                            if (boxes[b].group != wc->group)
                                continue;
                            if (boxes[b].grid_x == goals[g].x &&
                                boxes[b].grid_y == goals[g].y)
                                break;
                        }
                        if (b == num_boxes)
                            return FALSE;
                    }
                    break;
                }
                «ENDIF»
                «IF hasSomeOn && hasBoxes»
                case WIN_SOME_ON: {
                    uint8_t b;
                    uint8_t has_matching_goal = FALSE;
                    uint8_t found = FALSE;
                    for (g = 0; g < num_goals; g++) {
                        if (goals[g].group == wc->group) {
                            has_matching_goal = TRUE;
                            break;
                        }
                    }
                    if (!has_matching_goal)
                        break;
                    for (b = 0; b < num_boxes && !found; b++) {
                        «IF hasDestroy»
                        if (boxes[b].destroyed)
                            continue;
                        «ENDIF»
                        if (boxes[b].group != wc->group)
                            continue;
                        for (g = 0; g < num_goals; g++) {
                            if (goals[g].group == wc->group &&
                                goals[g].x == boxes[b].grid_x &&
                                goals[g].y == boxes[b].grid_y) {
                                found = TRUE;
                                break;
                            }
                        }
                    }
                    if (!found)
                        return FALSE;
                    break;
                }
                «ENDIF»
                «IF hasNoObject && hasBoxes»
                case WIN_NO_OBJECT: {
                    uint8_t b;
                    for (b = 0; b < num_boxes; b++) {
                        if (boxes[b].destroyed)
                            continue;
                        if (boxes[b].group == wc->group)
                            return FALSE;
                    }
                    break;
                }
                «ENDIF»
                «IF hasNoTile»
                case WIN_NO_TILE:
                    if (!no_tile_left(wc->group))
                        return FALSE;
                    break;
                «ENDIF»
                «IF hasPlayerOn»
                case WIN_PLAYER_ON:
                    if (no_tile_left(wc->group))
                        break;
                    if (LOGIC_TILE(player.grid_x, player.grid_y) != wc->group)
                        return FALSE;
                    break;
                «ENDIF»
                default:
                    break;
                }
            }
            return TRUE;
        }

        // ====================================================================
        // Tile effects
        // Both fillable and crumble replace the tile via TILE_BECOMES(t).
        // Trigger differs: fillable fires when a box lands, crumble fires
        // when the player leaves.
        // ====================================================================

        «IF hasFillable || hasCrumble»
        static void transform_tile(uint8_t x, uint8_t y) {
            uint8_t target = TILE_BECOMES(LOGIC_TILE(x, y));
            LOGIC_TILE(x, y) = target;
            level_draw_metatile(x, y, TILE_TO_METATILE(target));
        }

        «ENDIF»
        «IF hasBoxes»
        // Called when a box stops moving: marks the on-goal flag and fires
        // fillable or destroy effects.
        static void apply_box_landing(Box *box) {
            box->bg_commit_pending = TRUE;
            «IF needsGoalLanding»
            {
                uint8_t g;
                «IF hasOnGoal»box->on_goal = FALSE;«ENDIF»
                for (g = 0; g < num_goals; g++) {
                    if (goals[g].x == box->grid_x && goals[g].y == box->grid_y &&
                        box->group == goals[g].group) {
                        «IF hasOnGoal»box->on_goal = TRUE;«ENDIF»
                        «IF sndBoxOnGoal»
                        sound_play(&sfx_box_on_goal);
                        «ENDIF»
                        break;
                    }
                }
            }
            «ENDIF»
            «IF hasFillable || hasDestroy»
            {
                uint8_t tile = LOGIC_TILE(box->grid_x, box->grid_y);
                «IF hasFillable»
                if (TILE_IS_FILLABLE(tile)) {
                    box->destroyed = TRUE;
                    transform_tile(box->grid_x, box->grid_y);
                    «IF sndBoxDestroyed»
                    sound_play(&sfx_box_destroyed);
                    «ENDIF»
                } else «ENDIF»if (TILE_KILLS(tile)) {
                    box->destroyed = TRUE;
                    level_draw_metatile(box->grid_x, box->grid_y, TILE_TO_METATILE(tile));
                    «IF sndBoxDestroyed»
                    sound_play(&sfx_box_destroyed);
                    «ENDIF»
                }
            }
            «ENDIF»
        }
        «ENDIF»

        «IF hasCrumble»
        // Resolves the held crumble (the cell the player just left) and arms
        // a new one if the player's current cell is also crumble.
        static void apply_player_landing(void) {
            uint8_t tile;
            if (held_crumble_x != INVALID_POS) {
                uint8_t cx = held_crumble_x;
                uint8_t cy = held_crumble_y;
                held_crumble_x = INVALID_POS;
                if (TILE_IS_CRUMBLE(LOGIC_TILE(cx, cy))) {
                    transform_tile(cx, cy);
                    «IF sndCrumble»
                    sound_play(&sfx_crumble);
                    «ENDIF»
                }
            }
            tile = LOGIC_TILE(player.grid_x, player.grid_y);
            if (TILE_IS_CRUMBLE(tile)) {
                held_crumble_x = player.grid_x;
                held_crumble_y = player.grid_y;
            }
        }
        «ENDIF»

        // ====================================================================
        // Movement
        // ====================================================================

        // Validates a player move and sets up animation. On ice it delegates
        // to start_player_slide instead of pushing. Returns TRUE on success.
        static uint8_t try_move_player(int8_t dx, int8_t dy) {
            uint8_t nx, ny;
            «IF hasBoxes»
            Box *box;
            «ENDIF»

            player.anim_mode = PLAYER_ANIM_WALK;

            «IF hasSliding»
            // No traction on ice: the player slides instead of pushing.
            if (TILE_IS_ICE(LOGIC_TILE(player.grid_x, player.grid_y))) {
                start_player_slide(dx, dy);
                if (game_phase == PHASE_IDLE)
                    return FALSE;
                «IF sndPlayerMove»
                if (game_phase == PHASE_PLAYER_SLIDING)
                    sound_play(&sfx_player_move);
                «ENDIF»
                move_count++;
                ui_win_put_number(HUD_MOVES_COL, HUD_MOVES_ROW, move_count, HUD_MOVES_DIGITS);
                «IF sndPlayerBlocked»
                last_blocked_dir = INVALID_DIR;
                «ENDIF»
                return TRUE;
            }

            «ENDIF»
            nx = player.grid_x + dx;
            ny = player.grid_y + dy;
            if (nx >= level_width || ny >= level_height)
                return FALSE;

            «IF hasBoxes»
            box = box_find_at(nx, ny);
            if (box != NULL) {
                if (!box_can_move_to((uint8_t)(nx + dx), (uint8_t)(ny + dy)))
                    return FALSE;
                player.anim_mode = PLAYER_ANIM_PUSH;
                pushing_box = box;
                box_start_move(box, dx, dy);
            } else if (!TILE_IS_PASSABLE(LOGIC_TILE(nx, ny))) {
                return FALSE;
            }
            «ELSE»
            if (!TILE_IS_PASSABLE(LOGIC_TILE(nx, ny)))
                return FALSE;
            «ENDIF»

            «IF hasPull && hasBoxes»
            // B held + not pushing: drag the box behind the player.
            if (player.anim_mode != PLAYER_ANIM_PUSH && KEY_PRESSED(J_B)) {
                uint8_t behind_x = player.grid_x - dx;
                uint8_t behind_y = player.grid_y - dy;
                if (behind_x < level_width && behind_y < level_height) {
                    Box *pull_box = box_find_at(behind_x, behind_y);
                    if (pull_box != NULL) {
                        player.anim_mode = PLAYER_ANIM_PULL;
                        pulling_box = pull_box;
                        box_start_move(pull_box, dx, dy);
                    }
                }
            }

            «ENDIF»
            player_start_move(dx, dy);
            move_count++;
            ui_win_put_number(HUD_MOVES_COL, HUD_MOVES_ROW, move_count, HUD_MOVES_DIGITS);
            «IF sndPlayerBlocked»
            last_blocked_dir = INVALID_DIR;
            «ENDIF»

            «IF sndBoxPush || sndPlayerMove»
            if (player.anim_mode == PLAYER_ANIM_PUSH) {
                «IF sndBoxPush»sound_play(&sfx_box_push);«ENDIF»
            } else {
                «IF sndPlayerMove»sound_play(&sfx_player_move);«ENDIF»
            }
            «ENDIF»
            game_phase = PHASE_PLAYER_MOVING;
            return TRUE;
        }

        static void animate_blocked_push(void) {
            «IF sndPlayerBlocked»
            if (player.direction != last_blocked_dir)
                sound_play(&sfx_player_blocked);
            last_blocked_dir = player.direction;
            «ENDIF»
            player.anim_mode = PLAYER_ANIM_PUSH;
            player.anim_frame += PLAYER_ANIM_SPEED;
            if ((player.anim_frame >> SUBPIXEL_SHIFT) >= PLAYER_ANIM_FRAMES)
                player.anim_frame = 0;
        }

        «IF hasBoxes»
        // Resolves a box that just stopped moving: chains a slide if it
        // landed on ice, otherwise applies landing effects.
        static uint8_t resolve_pushed_box(void) {
            int8_t prev_dx = pushing_box->move_dx;
            int8_t prev_dy = pushing_box->move_dy;

            box_finish_move(pushing_box);

            «IF hasSliding»
            slide_carries_player_motion = TRUE;
            if (try_start_box_slide(pushing_box, prev_dx, prev_dy)) {
                player.anim_mode = PLAYER_ANIM_WALK;
                pushing_box = NULL;
                return TRUE;
            }
            «ELSE»
            (void)prev_dx;
            (void)prev_dy;
            «ENDIF»

            apply_box_landing(pushing_box);
            player.anim_mode = PLAYER_ANIM_WALK;
            pushing_box = NULL;
            return FALSE;
        }

        // Lands the player and resolves any push. Returns TRUE if the push
        // chained into a box slide, in which case the caller must skip the
        // rest of the turn so the slide drives the next phase.
        static uint8_t finish_player_resolve_push(void) {
            player_finish_move();
            if (player.anim_mode == PLAYER_ANIM_PUSH && pushing_box != NULL) {
                if (resolve_pushed_box()) {
                    «IF hasCrumble»
                    apply_player_landing();
                    «ENDIF»
                    return TRUE;
                }
            }
            return FALSE;
        }
        «ENDIF»

        static void finish_player_move(void) {
            int8_t pdx = player.move_dx;
            int8_t pdy = player.move_dy;

            «IF hasBoxes»
            if (finish_player_resolve_push())
                return;
            «ELSE»
            player_finish_move();
            «ENDIF»

            «IF hasCrumble»
            // Must run before the pulled box settles: the box lands on the
            // player's old cell, so it needs the post-crumble tile in place
            // (e.g. a crumble that turns into Pit must kill the box).
            apply_player_landing();
            «ENDIF»

            «IF hasPull && hasBoxes»
            if (player.anim_mode == PLAYER_ANIM_PULL && pulling_box != NULL) {
                box_finish_move(pulling_box);
                apply_box_landing(pulling_box);
                player.anim_mode = PLAYER_ANIM_WALK;
                pulling_box = NULL;
            }

            «ENDIF»
            check_player_tile(pdx, pdy);
        }

        «IF hasSliding»
        // ====================================================================
        // Sliding
        // ====================================================================

        «IF hasBoxes»
        // Starts a sliding box if it sits on ice and the next cell is free.
        static uint8_t try_start_box_slide(Box *box, int8_t prev_dx, int8_t prev_dy) {
            uint8_t tile;
            int8_t  dx, dy;
            uint8_t nx, ny;

            «IF hasDestroy»
            if (box->destroyed)
                return FALSE;
            «ENDIF»

            tile = LOGIC_TILE(box->grid_x, box->grid_y);
            if (!TILE_IS_ICE(tile))
                return FALSE;

            dx = prev_dx;
            dy = prev_dy;
            nx = box->grid_x + dx;
            ny = box->grid_y + dy;
            if (!box_can_move_to(nx, ny))
                return FALSE;

            sliding_box = box;
            box_start_move(box, dx, dy);
            game_phase = PHASE_BOX_SLIDING;
            return TRUE;
        }

        // End of a box slide: chains another slide if still on ice, otherwise
        // applies landing and returns to the player or to idle.
        static void finish_box_slide(void) {
            int8_t prev_dx, prev_dy;

            if (sliding_box == NULL) {
                go_idle();
                return;
            }

            prev_dx = sliding_box->move_dx;
            prev_dy = sliding_box->move_dy;

            box_finish_move(sliding_box);

            if (try_start_box_slide(sliding_box, prev_dx, prev_dy))
                return;

            apply_box_landing(sliding_box);
            sliding_box = NULL;

            if (slide_carries_player_motion) {
                slide_carries_player_motion = FALSE;
                check_player_tile(prev_dx, prev_dy);
            } else {
                go_idle();
            }
        }
        «ENDIF»

        // Player is on ice after a move: chain a box slide if the next cell
        // has a box, enter PHASE_PLAYER_SLIDING if the next cell is free,
        // otherwise go idle.
        static void start_player_slide(int8_t dx, int8_t dy) {
            uint8_t nx = player.grid_x + dx;
            uint8_t ny = player.grid_y + dy;
            «IF hasBoxes»
            Box    *box;
            «ENDIF»

            if (nx >= level_width || ny >= level_height) {
                go_idle();
                return;
            }

            «IF hasBoxes»
            box = box_find_at(nx, ny);
            if (box != NULL) {
                // No traction on ice: the box slides off, billiard-style.
                slide_carries_player_motion = FALSE;
                if (try_start_box_slide(box, dx, dy)) {
                    «IF sndBoxPush»
                    sound_play(&sfx_box_push);
                    «ENDIF»
                    return;
                }
                go_idle();
                return;
            }
            «ENDIF»
            if (!TILE_IS_PASSABLE(LOGIC_TILE(nx, ny))) {
                go_idle();
                return;
            }
            player.anim_mode = PLAYER_ANIM_WALK;
            player.direction = DIR_FROM_DELTA(dx, dy);
            player_start_move(dx, dy);
            game_phase = PHASE_PLAYER_SLIDING;
        }

        «ENDIF»
        // ====================================================================
        // Turn end
        // ====================================================================

        // Post-move tile effects: death, ice slide, conveyor re-entry. Falls
        // through to go_idle when nothing applies.
        static void check_player_tile(int8_t pdx, int8_t pdy) {
            «IF !hasSliding»
            (void)pdx;
            (void)pdy;
            «ENDIF»
            «IF hasDeadly»
            if (player_check_death())
                return;
            «ENDIF»
            «IF hasSliding»
            if (TILE_IS_ICE(LOGIC_TILE(player.grid_x, player.grid_y))) {
                start_player_slide(pdx, pdy);
                return;
            }
            «ENDIF»
            «IF hasConveyor»
            // Re-enter try_move_player with the tile's direction. The chain
            // continues because finish_player_move calls back into here.
            {
                uint8_t tile = LOGIC_TILE(player.grid_x, player.grid_y);
                if (TILE_IS_CONVEYOR(tile)) {
                    static const int8_t conveyor_dx[4] = {0, 0, -1, 1};
                    static const int8_t conveyor_dy[4] = {1, -1, 0, 0};
                    uint8_t dir = TILE_CONVEYOR_DIR(tile);
                    int8_t  cdx = conveyor_dx[dir];
                    int8_t  cdy = conveyor_dy[dir];
                    player.direction = dir;
                    if (try_move_player(cdx, cdy))
                        return;
                }
            }
            «ENDIF»
            go_idle();
        }

        // Wraps up the turn: commits boxes to BG, runs the win check, returns
        // to PHASE_IDLE (or to STATE_LEVEL_CLEAR on win).
        static void go_idle(void) {
            camera_update();
            if (level_is_complete()) {
                «IF hasBoxes»
                box_flush_all_to_bg();
                «ENDIF»
                game_render_sprites();
                «IF sndLevelComplete»
                sound_play(&sfx_level_complete);
                «ENDIF»
                current_state = STATE_LEVEL_CLEAR;
                return;
            }
            game_render_sprites();
            game_phase = PHASE_IDLE;
        }

        «IF hasDeadly»
        // Reload the level by re-entering STATE_GAME. Setting previous_state
        // to STATE_COUNT forces init() to fire even though current_state is
        // unchanged.
        static uint8_t player_check_death(void) {
            uint8_t tile = LOGIC_TILE(player.grid_x, player.grid_y);
            if (TILE_KILLS(tile)) {
                hide_sprites_range(0, MAX_HARDWARE_SPRITES);
                STATE_GOTO(STATE_GAME);
                return TRUE;
            }
            return FALSE;
        }
        «ENDIF»

        // ====================================================================
        // Public API
        // ====================================================================

        void game_render_sprites(void) {
            uint8_t oam = player_draw(0);
            «IF hasBoxes»
            oam = box_draw_sprites(oam);
            «ENDIF»
            hide_sprites_range(oam, MAX_HARDWARE_SPRITES);
        }

        void game_init(void) {
            «IF hasTitleScreen»
            // A fullscreen state may have overwritten low VRAM. Reload the font.
            if (previous_state == STATE_TITLE) ui_init();
            «ENDIF»
            «IF hasEndScreen»
            if (previous_state == STATE_ENDING) ui_init();
            «ENDIF»
            DISPLAY_OFF;
            SCX_REG = 0;
            SCY_REG = 0;

            level_load_tileset();
            player_load_sprites();
            level_load(current_level);

            move_count       = 0;
            game_phase       = PHASE_IDLE;
            «IF sndPlayerBlocked»
            last_blocked_dir = INVALID_DIR;
            «ENDIF»
            «IF hasBoxes»
            pushing_box      = NULL;
            «IF hasPull»
            pulling_box      = NULL;
            «ENDIF»
            «IF hasSliding»
            sliding_box                 = NULL;
            slide_carries_player_motion = FALSE;
            «ENDIF»
            «ENDIF»
            «IF hasCrumble»
            held_crumble_x = INVALID_POS;
            held_crumble_y = INVALID_POS;
            «ENDIF»

            level_render_full();
            camera_init();
            ui_win_draw_hud(current_level + 1, move_count, FALSE);
            move_win(WIN_OVERLAY_X, WIN_OVERLAY_Y);
            game_render_sprites();

            SHOW_BKG;
            SHOW_SPRITES;
            SHOW_WIN;
            DISPLAY_ON;
        }

        void game_resume(void) {
            ui_win_draw_hud(current_level + 1, move_count, FALSE);
            move_win(WIN_OVERLAY_X, WIN_OVERLAY_Y);
            SHOW_BKG;
            SHOW_SPRITES;
            SHOW_WIN;
            game_render_sprites();
        }

        void game_update(void) {
            switch (game_phase) {
            case PHASE_PLAYER_MOVING:
            «IF hasSliding»
            case PHASE_PLAYER_SLIDING:
            «ENDIF»
                if (player.move_steps_remaining > 0) {
                    player_animate_step();
                    «IF hasBoxes»
                    if (player.anim_mode == PLAYER_ANIM_PUSH && pushing_box != NULL)
                        box_animate_step(pushing_box);
                    «IF hasPull»
                    if (player.anim_mode == PLAYER_ANIM_PULL && pulling_box != NULL)
                        box_animate_step(pulling_box);
                    «ENDIF»
                    «ENDIF»
                    camera_update();
                    game_render_sprites();
                    if (player.move_steps_remaining <= 0)
                        finish_player_move();
                }
                return;

            «IF hasSliding && hasBoxes»
            case PHASE_BOX_SLIDING:
                if (sliding_box != NULL && sliding_box->move_steps_remaining > 0) {
                    box_animate_step(sliding_box);
                    game_render_sprites();
                    if (sliding_box->move_steps_remaining <= 0)
                        finish_box_slide();
                }
                return;

            «ENDIF»
            default:
                break;
            }

            // PHASE_IDLE - read input.
            if (KEY_TICKED(J_START)) {
                «IF hasBoxes»
                box_flush_all_to_bg();
                «ENDIF»
                current_state = STATE_PAUSE_MENU;
                return;
            }

            {
                int8_t dx = 0, dy = 0;
                if      (KEY_PRESSED(J_UP))    { dy = -1; player.direction = DIR_UP; }
                else if (KEY_PRESSED(J_DOWN))  { dy =  1; player.direction = DIR_DOWN; }
                else if (KEY_PRESSED(J_LEFT))  { dx = -1; player.direction = DIR_LEFT; }
                else if (KEY_PRESSED(J_RIGHT)) { dx =  1; player.direction = DIR_RIGHT; }

                if (dx != 0 || dy != 0) {
                    if (try_move_player(dx, dy)) {
                        camera_update();
                    } else {
                        animate_blocked_push();
                    }
                } else {
                    «IF sndPlayerBlocked»
                    last_blocked_dir   = INVALID_DIR;
                    «ENDIF»
                    player.anim_mode  = PLAYER_ANIM_WALK;
                    player.anim_frame = 0;
                }

                «IF hasBoxes»
                box_flush_all_to_bg();
                «ENDIF»
                game_render_sprites();
            }
        }
        '''
	}

	private def String winEntry(WinCondition wc, Game game) {
		switch wc {
			AllOn case wc.subject instanceof ObjectRef:
                '''{WIN_ALL_ON,    «game.objectIndex((wc.subject as ObjectRef).ref)»}'''
			SomeOn case wc.subject instanceof ObjectRef:
                '''{WIN_SOME_ON,   «game.objectIndex((wc.subject as ObjectRef).ref)»}'''
			AllOn case wc.subject instanceof PlayerRef:
                '''{WIN_PLAYER_ON, «game.tileIndex(wc.tile)»}'''
			SomeOn case wc.subject instanceof PlayerRef:
                '''{WIN_PLAYER_ON, «game.tileIndex(wc.tile)»}'''
			NoEntity case wc.entity instanceof ObjectDef:
                '''{WIN_NO_OBJECT, «game.objectIndex(wc.entity as ObjectDef)»}'''
			NoEntity case wc.entity instanceof TileDef:
                '''{WIN_NO_TILE,   «game.tileIndex(wc.entity as TileDef)»}'''
		}
	}

}
