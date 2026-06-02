package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.util.Feature
import java.util.Set

/** Emits ui.h and ui.c: window-layer text, HUD, overlay menus, fullscreen helper. */
class UiGen {

    def String uiH(Set<Feature> features) '''
    #ifndef UI_H
    #define UI_H

    #include <stdint.h>

    // ====================================================================
    // Layout constants
    // ====================================================================

    // VRAM tile slot for the cursor arrow. font_min occupies 0..36.
    #define UI_CURSOR_TILE 37

    // Cap displayed move counts so the HUD field never overflows.
    #define MOVES_DISPLAY_MAX 9999u

    // HUD layout in window-overlay coordinates.
    #define HUD_ROUND_COL    6
    #define HUD_ROUND_ROW    0
    #define HUD_ROUND_DIGITS 3
    #define HUD_MOVES_COL    6
    #define HUD_MOVES_ROW    1
    #define HUD_MOVES_DIGITS 4

    // ====================================================================
    // API
    // ====================================================================

    void ui_init(void);

    void ui_win_put_number(uint8_t x, uint8_t y, uint16_t value, uint8_t digits);
    void ui_win_draw_menu(const char *item0, const char *item1, uint8_t cursor);
    void ui_win_draw_hud(uint16_t round_num, uint16_t value, uint8_t is_best);

    // 2-item menu tick. Returns 0 (nothing), 1 (item0) or 2 (item1).
    uint8_t ui_overlay_menu_tick(const char *item0, const char *item1, uint8_t *cursor);
    «IF features.contains(Feature.TITLE_SCREEN) || features.contains(Feature.END_SCREEN)»

    void ui_show_fullscreen(uint8_t tile_count, const uint8_t *tiles, const uint8_t *map);
    «ENDIF»

    #endif // UI_H
    '''

	def String uiC(Game game, Set<Feature> features) {
		val hasSound = features.contains(Feature.SOUND)
		val sounds = if (hasSound) game.sounds else null
		val sndMenuMove = sounds?.menuMove !== null
		val sndMenuSelect = sounds?.menuSelect !== null
        '''
        #include "ui.h"
        #include "common.h"
        «IF hasSound»
        #include "sound.h"
        «ENDIF»
        #include <gb/gb.h>
        #include <gbdk/bcd.h>
        #include <gbdk/font.h>

        // ====================================================================
        // State
        // ====================================================================

        // Right-pointing cursor arrow (2bpp).
        static const uint8_t cursor_tile[] = {
            0x00, 0x00,
            0x00, 0x00,
            0x10, 0x10,
            0x18, 0x18,
            0x1C, 0x1C,
            0x18, 0x18,
            0x10, 0x10,
            0x00, 0x00,
        };

        // ====================================================================
        // Helpers
        // ====================================================================

        // font_min layout: 0 = space, 1..10 = '0'..'9', 11..36 = 'A'..'Z'.
        static uint8_t ui_char_tile(char c) {
            if (c >= 'A' && c <= 'Z') return (uint8_t)(c - 'A' + 11);
            if (c >= 'a' && c <= 'z') return (uint8_t)(c - 'a' + 11);
            if (c >= '0' && c <= '9') return (uint8_t)(c - '0' + 1);
            return 0;
        }

        static void ui_win_print(uint8_t x, uint8_t y, const char *text) {
            while (*text != '\0' && x < DEVICE_SCREEN_WIDTH) {
                set_win_tile_xy(x, y, ui_char_tile(*text));
                x++;
                text++;
            }
        }

        static void ui_win_clear_rows(uint8_t y_start, uint8_t rows) {
            fill_win_rect(0, y_start, DEVICE_SCREEN_WIDTH, rows, 0);
        }

        // ====================================================================
        // Public API
        // ====================================================================

        void ui_init(void) {
            font_init();
            font_load(font_min);
            set_bkg_data(UI_CURSOR_TILE, 1, cursor_tile);
        }

        void ui_win_put_number(uint8_t x, uint8_t y, uint16_t value, uint8_t digits) {
            // uint2bcd + bcd2text avoid runtime division (GBDK guideline).
            // tile_offset 1 because digit '0' is at tile 1 in font_min.
            BCD     bcd;
            uint8_t bcdText[9];
            uint8_t i;
            uint2bcd(value, &bcd);
            bcd2text(&bcd, 1, bcdText);
            // bcd2text writes 8 zero-padded digits. Show the rightmost N.
            for (i = 0; i < digits; i++)
                set_win_tile_xy(x + i, y, bcdText[8 - digits + i]);
        }

        void ui_win_draw_menu(const char *item0, const char *item1, uint8_t cursor) {
            ui_win_clear_rows(0, 2);
            set_win_tile_xy(0, 0, cursor == 0U ? UI_CURSOR_TILE : 0);
            ui_win_print(1, 0, item0);
            if (item1 != NULL) {
                set_win_tile_xy(0, 1, cursor == 1U ? UI_CURSOR_TILE : 0);
                ui_win_print(1, 1, item1);
            }
        }

        void ui_win_draw_hud(uint16_t round_num, uint16_t value, uint8_t is_best) {
            if (value > MOVES_DISPLAY_MAX) value = MOVES_DISPLAY_MAX;
            ui_win_clear_rows(0, 2);
            ui_win_print(0, HUD_ROUND_ROW, "ROUND");
            ui_win_put_number(HUD_ROUND_COL, HUD_ROUND_ROW, round_num, HUD_ROUND_DIGITS);
            ui_win_print(0, HUD_MOVES_ROW, is_best ? "BEST " : "MOVES");
            ui_win_put_number(HUD_MOVES_COL, HUD_MOVES_ROW, value, HUD_MOVES_DIGITS);
        }

        uint8_t ui_overlay_menu_tick(const char *item0, const char *item1, uint8_t *cursor) {
            if (item1 != NULL && (KEY_TICKED(J_UP) || KEY_TICKED(J_DOWN))) {
                *cursor ^= 1;
                «IF sndMenuMove»
                sound_play(&sfx_menu_move);
                «ENDIF»
                ui_win_draw_menu(item0, item1, *cursor);
                return 0;
            }
            if (KEY_CONFIRM) {
                «IF sndMenuSelect»
                sound_play(&sfx_menu_select);
                «ENDIF»
                return (uint8_t)(*cursor + 1);
            }
            return 0;
        }
        «IF features.contains(Feature.TITLE_SCREEN) || features.contains(Feature.END_SCREEN)»

        void ui_show_fullscreen(uint8_t tile_count, const uint8_t *tiles, const uint8_t *map) {
            DISPLAY_OFF;
            HIDE_SPRITES;
            HIDE_WIN;
            SCX_REG = 0;
            SCY_REG = 0;
            set_bkg_data(0, tile_count, tiles);
            set_bkg_tiles(0, 0, DEVICE_SCREEN_WIDTH, DEVICE_SCREEN_HEIGHT, map);
            SHOW_BKG;
            DISPLAY_ON;
        }
        «ENDIF»
        '''
	}
}
