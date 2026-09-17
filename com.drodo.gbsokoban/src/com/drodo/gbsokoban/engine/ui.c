#include "ui.h"
#include "common.h"
#ifdef FEAT_SOUND
#include "sound.h"
#endif
#ifdef FEAT_COLOR
#include "assets.h"
#include <gb/cgb.h>
#endif
#include <gb/gb.h>
#include <gbdk/bcd.h>
#include <gbdk/font.h>

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

// font_min layout: 0 = space, 1..10 = '0'..'9', 11..36 = 'A'..'Z'. Lower case maps
// onto the capitals.

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

void ui_init(void) {
    font_init();
    font_load(font_min);
    set_bkg_data(UI_CURSOR_TILE, 1, cursor_tile);
#ifdef FEAT_COLOR
    if (_cpu == CGB_TYPE) {
        set_bkg_palette(0, BKG_PALETTE_COUNT, bkg_palettes);
        set_sprite_palette(0, SPRITE_PALETTE_COUNT, sprite_palettes);
        // Clear both planes: the HUD would take whatever palette VRAM held.
        VBK_REG = VBK_ATTRIBUTES;
        fill_bkg_rect(0, 0, BKG_MAP_TILES, BKG_MAP_TILES, 0);
        fill_win_rect(0, 0, BKG_MAP_TILES, BKG_MAP_TILES, 0);
        VBK_REG = VBK_TILES;
    }
#endif
}

void ui_win_put_number(uint8_t x, uint8_t y, uint16_t value, uint8_t digits) {
    // uint2bcd + bcd2text avoid runtime division (GBDK guideline). tile_offset 1
    // because digit '0' is at tile 1 in font_min.
    BCD     bcd;
    uint8_t bcd_text[9];
    uint8_t i;
    if (value > COUNTER_DISPLAY_MAX) value = COUNTER_DISPLAY_MAX;
    uint2bcd(value, &bcd);
    bcd2text(&bcd, 1, bcd_text);
    // bcd2text writes 8 zero-padded digits. Show the rightmost N.
    for (i = 0; i < digits; i++)
        set_win_tile_xy(x + i, y, bcd_text[8 - digits + i]);
}

static uint8_t ui_text_width(const char *text) {
    uint8_t n = 0;
    while (text[n] != '\0') n++;
    return n;
}

// The counter keeps its column; the label ends one space before it.
static void ui_win_field(uint8_t value_col, uint8_t row, const char *label,
                         uint16_t value, uint8_t digits) {
    ui_win_print(value_col - 1 - ui_text_width(label), row, label);
    ui_win_put_number(value_col, row, value, digits);
}

void ui_win_draw_playing(uint8_t row, uint16_t stage, uint16_t moves) {
    ui_win_clear_rows(row, 1);
    ui_win_field(HUD_VALUE_L, row, "STAGE", stage, HUD_STAGE_DIGITS);
    ui_win_field(HUD_VALUE_R, row, "MOVES", moves, HUD_COUNT_DIGITS);
}

void ui_win_draw_result(uint8_t row, uint16_t moves, uint16_t best) {
    ui_win_clear_rows(row, 1);
    ui_win_field(HUD_VALUE_L, row, "MOVES", moves, HUD_COUNT_DIGITS);
    ui_win_field(HUD_VALUE_R, row, "BEST", best, HUD_COUNT_DIGITS);
}

#ifdef FEAT_LEVEL_SELECT
void ui_win_draw_browse(uint8_t row, uint16_t stage, uint16_t best) {
    ui_win_clear_rows(row, 1);
    ui_win_field(HUD_VALUE_L, row, "STAGE", stage, HUD_STAGE_DIGITS);
    ui_win_field(HUD_VALUE_R, row, "BEST", best, HUD_COUNT_DIGITS);
}
#endif

static void ui_win_draw_item(uint8_t at, uint8_t row, const char *text, uint8_t picked) {
    set_win_tile_xy(at, row, picked ? UI_CURSOR_TILE : 0);
    ui_win_print(at + 1, row, text);
}

void ui_win_draw_menu(uint8_t row, const char *item0, const char *item1, uint8_t cursor) {
    ui_win_clear_rows(row, 1);
    if (item1 == NULL) {
        ui_win_draw_item((uint8_t)((DEVICE_SCREEN_WIDTH - 1 - ui_text_width(item0)) >> 1),
                         row, item0, cursor == 0U);
        return;
    }
    // A half of the screen each, like the HUD row's own two.
    ui_win_draw_item(0, row, item0, cursor == 0U);
    ui_win_draw_item(DEVICE_SCREEN_WIDTH / 2, row, item1, cursor == 1U);
}

uint8_t ui_overlay_menu_tick(uint8_t row, const char *item0, const char *item1, uint8_t *cursor) {
    // Side by side, but up and down are taken too.
    if (item1 != NULL && (KEY_TICKED(J_LEFT) || KEY_TICKED(J_RIGHT)
                          || KEY_TICKED(J_UP) || KEY_TICKED(J_DOWN))) {
        *cursor ^= 1;
#ifdef FEAT_SND_MENU_MOVE
        sound_play(&sfx_menu_move);
#endif
        ui_win_draw_menu(row, item0, item1, *cursor);
        return 0;
    }
    if (KEY_CONFIRM) {
#ifdef FEAT_SND_MENU_SELECT
        sound_play(&sfx_menu_select);
#endif
        return (uint8_t)(*cursor + 1);
    }
    return 0;
}
#ifdef FEAT_TEXT_SCREEN

static uint8_t ui_row_length(const char *text) {
    uint8_t length = 0;
    while (text[length] && text[length] != '\n' && length < DEVICE_SCREEN_WIDTH)
        length++;
    return length;
}

uint8_t ui_text_rows(const char *text) {
    uint8_t rows = 1;
    while (*text)
        if (*text++ == '\n')
            rows++;
    return rows;
}

static uint8_t ui_text_top_for(uint8_t rows) {
    return rows >= DEVICE_SCREEN_HEIGHT ? 0 : (uint8_t)((DEVICE_SCREEN_HEIGHT - rows) >> 1);
}

void ui_text_screen_begin(void) {
    DISPLAY_OFF;
    HIDE_SPRITES;
    HIDE_WIN;
    SCX_REG = 0;
    SCY_REG = 0;
    fill_bkg_rect(0, 0, BKG_MAP_TILES, BKG_MAP_TILES, 0);
}

uint8_t ui_text_block(uint8_t y, const char *text) {
    while (*text) {
        uint8_t length = ui_row_length(text);
        uint8_t x = (uint8_t)((DEVICE_SCREEN_WIDTH - length) >> 1);
        uint8_t i;
        for (i = 0; i < length; i++)
            set_bkg_tile_xy((uint8_t)(x + i), y, ui_char_tile(text[i]));
        y++;
        while (*text && *text != '\n')
            text++;
        if (*text == '\n')
            text++;
    }
    return y;
}

void ui_text_block_centered(const char *text) {
    ui_text_block(ui_text_top_for(ui_text_rows(text)), text);
}

void ui_text_clear_row(uint8_t y) {
    fill_bkg_rect(0, y, DEVICE_SCREEN_WIDTH, 1, 0);
}

void ui_text_screen_end(void) {
    SHOW_BKG;
    DISPLAY_ON;
}
#endif
