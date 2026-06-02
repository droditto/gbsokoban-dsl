package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.util.Feature
import java.util.Set

import static extension com.drodo.gbsokoban.generator.GenUtils.*

/**
 * Emits menus.h and menus.c: GameState enum plus every overlay state (title,
 * pause, level select, level clear, ending). STATE_GAME lives in game.c.
 * The enum order must match the StateDef[] table in main.c.
 */
class MenusGen {

	def String menusH(Set<Feature> features) {
		val hasTitleScreen = features.contains(Feature.TITLE_SCREEN)
		val hasLevelSelect = features.contains(Feature.LEVEL_SELECT)
		val hasEndScreen = features.contains(Feature.END_SCREEN)
        '''
        #ifndef MENUS_H
        #define MENUS_H

        #include <stdint.h>

        typedef enum {
            «IF hasTitleScreen»
            STATE_TITLE,
            «ENDIF»
            STATE_GAME,
            STATE_PAUSE_MENU,
            «IF hasLevelSelect»
            STATE_LEVEL_SELECT,
            «ENDIF»
            STATE_LEVEL_CLEAR,
            «IF hasEndScreen»
            STATE_ENDING,
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

        #define STATE_GOTO(s) (previous_state = STATE_COUNT, current_state = (s))

        «IF hasTitleScreen»
        void title_init(void);
        void title_update(void);

        «ENDIF»
        void pause_init(void);
        void pause_update(void);

        «IF hasLevelSelect»
        void select_init(void);
        void select_update(void);

        «ENDIF»
        void clear_init(void);
        void clear_update(void);

        «IF hasEndScreen»
        void ending_init(void);
        void ending_update(void);

        «ENDIF»
        #endif // MENUS_H
        '''
	}

	def String menusC(Game game, Set<Feature> features) {
		val hasTitleScreen = features.contains(Feature.TITLE_SCREEN)
		val hasLevelSelect = features.contains(Feature.LEVEL_SELECT)
		val hasEndScreen = features.contains(Feature.END_SCREEN)
		val hasSound = features.contains(Feature.SOUND)
		val hasSave = features.needsSave
		val sounds = if (hasSound) game.sounds else null
		val sndLevelRestart = sounds?.levelRestart !== null
		val sndMenuMove = sounds?.menuMove !== null
		val sndMenuSelect = sounds?.menuSelect !== null
        '''
        #include "menus.h"
        #include "camera.h"
        #include "common.h"
        #include "game.h"
        #include "level.h"
        #include "player.h"
        «IF hasSave»
        #include "save.h"
        «ENDIF»
        «IF hasSound»
        #include "sound.h"
        «ENDIF»
        #include "ui.h"
        #include <gb/gb.h>
        «IF hasTitleScreen»
        #include "title_screen.h"
        «ENDIF»
        «IF hasEndScreen»
        #include "ending_screen.h"
        «ENDIF»
        «IF hasTitleScreen»

        // ====================================================================
        // STATE_TITLE
        // ====================================================================

        void title_init(void) {
            camera_reset();
            ui_show_fullscreen(title_screen_TILE_COUNT, title_screen_tiles, title_screen_map);
        }

        void title_update(void) {
            if (KEY_CONFIRM) {
                «IF hasSave»
                current_level = save_get_resume_level();
                «ELSE»
                current_level = 0;
                «ENDIF»
                current_state = STATE_GAME;
            }
        }
        «ENDIF»

        // ====================================================================
        // STATE_PAUSE_MENU
        // ====================================================================

        static uint8_t pause_cursor;

        void pause_init(void) {
            pause_cursor = 0;
            ui_win_draw_menu("RETRY", «IF hasLevelSelect»"SELECT"«ELSE»NULL«ENDIF», pause_cursor);
            move_win(WIN_OVERLAY_X, WIN_OVERLAY_Y);
        }

        void pause_update(void) {
            uint8_t sel;

            if (KEY_TICKED(J_START) || KEY_TICKED(J_B)) {
                previous_state = STATE_GAME;
                current_state  = STATE_GAME;
                game_resume();
                return;
            }

            sel = ui_overlay_menu_tick("RETRY", «IF hasLevelSelect»"SELECT"«ELSE»NULL«ENDIF», &pause_cursor);
            if (sel == 1) {
                «IF sndLevelRestart»
                sound_play(&sfx_level_restart);
                «ENDIF»
                STATE_GOTO(STATE_GAME);
            «IF hasLevelSelect»
            } else if (sel == 2) {
                current_state = STATE_LEVEL_SELECT;
            «ENDIF»
            }
        }

        // ====================================================================
        // STATE_LEVEL_CLEAR
        // ====================================================================

        static uint8_t clear_cursor;

        void clear_init(void) {
            «IF hasSave»
            save_complete_level(current_level, move_count);
            «ENDIF»
            clear_cursor = 0;
            ui_win_draw_menu("NEXT", «IF hasLevelSelect»"SELECT"«ELSE»NULL«ENDIF», clear_cursor);
            move_win(WIN_OVERLAY_X, WIN_OVERLAY_Y);
        }

        void clear_update(void) {
            uint8_t sel = ui_overlay_menu_tick("NEXT", «IF hasLevelSelect»"SELECT"«ELSE»NULL«ENDIF», &clear_cursor);
            if (sel == 1) {
                «IF features.contains(Feature.MULTI_LEVEL)»
                if (current_level < TOTAL_LEVELS - 1) {
                    current_level++;
                «IF hasEndScreen»
                } else if (save_all_levels_beaten()) {
                    current_state = STATE_ENDING;
                    return;
                «ELSEIF hasTitleScreen»
                } else if (save_all_levels_beaten()) {
                    current_state = STATE_TITLE;
                    return;
                «ENDIF»
                } else {
                    // Past the last level with unbeaten ones still left: wrap
                    // to the first unbeaten one.
                    current_level = save_get_resume_level();
                }
                «ELSEIF hasEndScreen»
                current_state = STATE_ENDING;
                return;
                «ELSEIF hasTitleScreen»
                current_state = STATE_TITLE;
                return;
                «ELSE»
                // Only one level: replay it.
                current_level = 0;
                «ENDIF»
                STATE_GOTO(STATE_GAME);
            «IF hasLevelSelect»
            } else if (sel == 2) {
                current_state = STATE_LEVEL_SELECT;
            «ENDIF»
            }
        }
        «IF hasLevelSelect»

        // ====================================================================
        // STATE_LEVEL_SELECT
        // ====================================================================

        static uint8_t   selected_level;
        static uint8_t   saved_level;
        static GameState saved_state;  // PAUSE or LEVEL_CLEAR we came from

        static void show_preview(void) {
            const LevelDef *level    = &levels[selected_level];
            int16_t player_pixel_x = (int16_t)level->player_x << CELL_SHIFT;
            int16_t player_pixel_y = (int16_t)level->player_y << CELL_SHIFT;
            uint8_t player_screen_x, player_screen_y;
            uint16_t best_moves;

            DISPLAY_OFF;
            level_preview(selected_level);
            camera_set_for_level_dims((uint16_t)level->width  << CELL_SHIFT,
                                      (uint16_t)level->height << CELL_SHIFT,
                                      player_pixel_x, player_pixel_y);

            player_screen_x = (uint8_t)camera_to_screen_x(player_pixel_x);
            player_screen_y = (uint8_t)camera_to_screen_y(player_pixel_y);
            player_draw_preview(player_screen_x, player_screen_y);

            best_moves = save_get_best_moves(selected_level);
            ui_win_draw_hud((uint16_t)(selected_level + 1), best_moves, TRUE);
            move_win(WIN_OVERLAY_X, WIN_OVERLAY_Y);

            SHOW_BKG;
            SHOW_SPRITES;
            SHOW_WIN;
            DISPLAY_ON;
        }

        void select_init(void) {
            saved_state    = previous_state;
            saved_level    = current_level;
            selected_level = current_level;
            show_preview();
        }

        void select_update(void) {
            if (KEY_CONFIRM) {
                «IF sndMenuSelect»
                sound_play(&sfx_menu_select);
                «ENDIF»
                current_level = selected_level;
                STATE_GOTO(STATE_GAME);
                return;
            }

            if (KEY_TICKED(J_B)) {
                if (saved_state == STATE_LEVEL_CLEAR) {
                    // Entered from level-clear. Nothing in-progress to resume.
                    «IF hasTitleScreen»
                    current_state = STATE_TITLE;
                    «ELSE»
                    current_state = STATE_GAME;
                    «ENDIF»
                } else {
                    // Restore the paused game and go back to the pause menu.
                    DISPLAY_OFF;
                    current_level = saved_level;
                    level_render_full();
                    camera_center_on_player();
                    game_render_sprites();
                    SHOW_BKG;
                    SHOW_SPRITES;
                    SHOW_WIN;
                    DISPLAY_ON;
                    current_state = STATE_PAUSE_MENU;
                }
                return;
            }

            if (KEY_TICKED(J_UP) || KEY_TICKED(J_RIGHT)) {
                selected_level = (selected_level == TOTAL_LEVELS - 1) ? 0 : selected_level + 1;
            } else if (KEY_TICKED(J_DOWN) || KEY_TICKED(J_LEFT)) {
                selected_level = (selected_level == 0) ? TOTAL_LEVELS - 1 : selected_level - 1;
            } else {
                return;
            }
            «IF sndMenuMove»
            sound_play(&sfx_menu_move);
            «ENDIF»
            show_preview();
        }
        «ENDIF»
        «IF hasEndScreen»

        // ====================================================================
        // STATE_ENDING
        // ====================================================================

        void ending_init(void) {
            camera_reset();
            ui_show_fullscreen(ending_screen_TILE_COUNT, ending_screen_tiles, ending_screen_map);
        }

        void ending_update(void) {
            if (KEY_CONFIRM) {
                «IF hasTitleScreen»
                current_state = STATE_TITLE;
                «ELSE»
                current_state = STATE_GAME;
                «ENDIF»
            }
        }
        «ENDIF»
        '''
	}
}
