package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.util.Feature
import java.util.Set

import static extension com.drodo.gbsokoban.generator.GenUtils.*

/** Emits main.c: globals, StateDef table (order must match GameState), vsync loop. */
class MainGen {

	def String mainC(Set<Feature> features) {
		val hasTitleScreen = features.contains(Feature.TITLE_SCREEN)
		val hasLevelSelect = features.contains(Feature.LEVEL_SELECT)
		val hasEndScreen = features.contains(Feature.END_SCREEN)
		val hasSound = features.contains(Feature.SOUND)
		val hasSave = features.needsSave
        '''
        #include "camera.h"
        #include "common.h"
        #include "game.h"
        #include "menus.h"
        «IF hasSave»
        #include "save.h"
        «ENDIF»
        «IF hasSound»
        #include "sound.h"
        «ENDIF»
        #include "ui.h"
        #include <gb/gb.h>

        // ====================================================================
        // Globals
        // ====================================================================

        uint8_t   joypad_current  = 0;
        uint8_t   joypad_previous = 0;

        GameState current_state;
        GameState previous_state;
        uint8_t   current_level;

        // ====================================================================
        // State table (order matches GameState in menus.h)
        // ====================================================================

        static const StateDef states[STATE_COUNT] = {
            «IF hasTitleScreen»
            {.init = title_init,  .update = title_update},
            «ENDIF»
            {.init = game_init,   .update = game_update},
            {.init = pause_init,  .update = pause_update},
            «IF hasLevelSelect»
            {.init = select_init, .update = select_update},
            «ENDIF»
            {.init = clear_init,  .update = clear_update},
            «IF hasEndScreen»
            {.init = ending_init, .update = ending_update},
            «ENDIF»
        };

        // ====================================================================
        // Entry point
        // ====================================================================

        void main(void) {
            DISPLAY_OFF;

            BGP_REG  = PALETTE_BGP;
            OBP0_REG = PALETTE_OBP0;
            OBP1_REG = PALETTE_OBP1;
            SPRITES_8x16;

            ui_init();
            «IF hasSave»
            save_init();
            «ENDIF»
            «IF hasSound»
            sound_init();
            «ENDIF»

            «IF hasTitleScreen»
            current_state = STATE_TITLE;
            «ELSE»
            «IF hasSave»
            current_level = save_get_resume_level();
            «ELSE»
            current_level = 0;
            «ENDIF»
            current_state = STATE_GAME;
            «ENDIF»
            previous_state = STATE_COUNT;  // guarantees init() runs on frame 1

            DISPLAY_ON;

            while (1) {
                joypad_previous = joypad_current;
                joypad_current  = joypad();

                if (current_state != previous_state) {
                    states[current_state].init();
                    previous_state = current_state;
                }
                states[current_state].update();

                vsync();
                camera_flush_scroll();
            }
        }
        '''
	}
}
