#ifndef UI_H
#define UI_H

#include <stdint.h>
#include "common.h"
#include "config.h"

// Cap displayed counters so the HUD field never overflows.
#define COUNTER_DISPLAY_MAX 9999u

// Counter columns. Labels end one space before their own.
#define HUD_VALUE_L 6
#define HUD_VALUE_R 16
#define HUD_COUNT_DIGITS 4

// The HUD keeps the bottom row; a panel grows upward from it.
#define HUD_ROW_PLAYING 0
#define PANEL_MENU_ROW  0
#define PANEL_HUD_ROW   1

void ui_init(void);

void ui_win_put_number(uint8_t x, uint8_t y, uint16_t value, uint8_t digits);
// Level and move count.
void ui_win_draw_playing(uint8_t row, uint16_t stage, uint16_t moves);

// This run's moves against the record.
void ui_win_draw_result(uint8_t row, uint16_t moves, uint16_t best);

#ifdef FEAT_LEVEL_SELECT
// Level and its best, for the select screen.
void ui_win_draw_browse(uint8_t row, uint16_t stage, uint16_t best);
#endif

void ui_win_draw_menu(uint8_t row, const char *item0, const char *item1, uint8_t cursor);

// 2-item menu tick. Returns 0 (nothing), 1 (item0) or 2 (item1).
uint8_t ui_overlay_menu_tick(uint8_t row, const char *item0, const char *item1, uint8_t *cursor);
#ifdef FEAT_TEXT_SCREEN

// Clears the background and hides everything over it.
void ui_text_screen_begin(void);

// Rows a block of text needs, its rows separated by '\n'.
uint8_t ui_text_rows(const char *text);

// From row y down, centered. Returns the row after the last.
uint8_t ui_text_block(uint8_t y, const char *text);

void ui_text_block_centered(const char *text);

void ui_text_clear_row(uint8_t y);

void ui_text_screen_end(void);
#endif

#endif // UI_H
