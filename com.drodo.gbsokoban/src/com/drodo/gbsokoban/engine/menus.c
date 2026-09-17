#include "menus.h"
#ifdef FEAT_BOXES
#include "box.h"
#endif
#include "camera.h"
#include "common.h"
#include "game.h"
#include "level.h"
#include "player.h"
#include "render.h"
#ifdef FEAT_SAVE
#include "save.h"
#endif
#ifdef FEAT_SOUND
#include "sound.h"
#endif
#include "turn.h"
#include "ui.h"
#include <gb/gb.h>

#ifdef FEAT_LEVEL_SELECT
#define MENU_ITEM_SELECT "SELECT"
#else
#define MENU_ITEM_SELECT NULL
#endif

#ifdef FEAT_TITLE_SCREEN

// ====================================================================
// STATE_TITLE
// ====================================================================

#define TITLE_BLINK_FRAMES 32
#define TITLE_PROMPT       "PUSH START"
// Fixed row; the generator caps the title short of it.
#define TITLE_PROMPT_ROW   (DEVICE_SCREEN_HEIGHT - 3)

static uint8_t title_blink;
static uint8_t title_prompt_shown;

void title_init(void) {
    uint8_t rows = ui_text_rows(TITLE_SCREEN_TEXT);
    uint8_t top = rows >= TITLE_PROMPT_ROW ? 0 : (uint8_t)((TITLE_PROMPT_ROW - rows) >> 1);

    camera_reset();
    ui_text_screen_begin();
    ui_text_block(top, TITLE_SCREEN_TEXT);
    ui_text_block(TITLE_PROMPT_ROW, TITLE_PROMPT);
    ui_text_screen_end();
    title_blink = 0;
    title_prompt_shown = TRUE;
}

void title_update(void) {
    if (++title_blink == TITLE_BLINK_FRAMES) {
        title_blink = 0;
        title_prompt_shown = !title_prompt_shown;
        vsync();
        if (title_prompt_shown)
            ui_text_block(TITLE_PROMPT_ROW, TITLE_PROMPT);
        else
            ui_text_clear_row(TITLE_PROMPT_ROW);
    }
    if (KEY_CONFIRM) {
#ifdef FEAT_SAVE
        current_level = save_get_resume_level();
#else
        current_level = 0;
#endif
        current_state = STATE_GAME;
    }
}
#endif

// ====================================================================
// STATE_PAUSE_MENU
// ====================================================================

static uint8_t pause_cursor;

// A row above the HUD row: the counters keep their scanlines.
static void open_panel(uint8_t cursor, const char *item0) {
    ui_win_draw_menu(PANEL_MENU_ROW, item0, MENU_ITEM_SELECT, cursor);
    move_win(WIN_OVERLAY_X, WIN_PANEL_Y);
}

// Same values as while playing: pausing changes neither.
static void draw_pause_panel(uint8_t cursor) {
    ui_win_draw_playing(PANEL_HUD_ROW, current_level + 1, move_count);
    open_panel(cursor, "RETRY");
}

static void draw_clear_panel(uint8_t cursor) {
#ifdef FEAT_SAVE
    ui_win_draw_result(PANEL_HUD_ROW, move_count, save_get_best_moves(current_level));
#else
    ui_win_draw_playing(PANEL_HUD_ROW, current_level + 1, move_count);
#endif
    open_panel(cursor, "NEXT");
}

void pause_init(void) {
    pause_cursor = 0;
    draw_pause_panel(pause_cursor);
}

void pause_update(void) {
    uint8_t choice;

    if (KEY_TICKED(J_START) || KEY_TICKED(J_B)) {
        previous_state = STATE_GAME;
        current_state  = STATE_GAME;
        game_resume();
        return;
    }

    choice = ui_overlay_menu_tick(PANEL_MENU_ROW, "RETRY", MENU_ITEM_SELECT, &pause_cursor);
    if (choice == 1) {
#ifdef FEAT_SND_LEVEL_RESTART
        sound_play(&sfx_level_restart);
#endif
        STATE_GOTO(STATE_GAME);
#ifdef FEAT_LEVEL_SELECT
    } else if (choice == 2) {
        current_state = STATE_LEVEL_SELECT;
#endif
    }
}

// ====================================================================
// STATE_LEVEL_CLEAR
// ====================================================================

static uint8_t clear_cursor;

#if defined(FEAT_SAVE) && defined(FEAT_SAVE_ALL_BEATEN)
// Set on the clear that beat the last level, not on every clear after.
static uint8_t just_finished;
#endif

void clear_init(void) {
#ifdef FEAT_SAVE
#ifdef FEAT_SAVE_ALL_BEATEN
    uint8_t finished_before = save_all_levels_beaten();
#endif
    save_complete_level(current_level, move_count);
#ifdef FEAT_SAVE_ALL_BEATEN
    just_finished = !finished_before && save_all_levels_beaten();
#endif
#endif
    clear_cursor = 0;
    draw_clear_panel(clear_cursor);
}

void clear_update(void) {
    uint8_t choice = ui_overlay_menu_tick(PANEL_MENU_ROW, "NEXT", MENU_ITEM_SELECT, &clear_cursor);
    if (choice == 1) {
#ifdef FEAT_MULTI_LEVEL
        // With a level select, the last one beaten can be any of them.
#if defined(FEAT_SAVE) && defined(FEAT_SAVE_ALL_BEATEN)
        if (just_finished) {
#ifdef FEAT_END_SCREEN
            current_state = STATE_ALL_LEVELS_COMPLETE;
#else
            current_state = STATE_TITLE;
#endif
            return;
        }
#endif
        if (current_level < TOTAL_LEVELS - 1) {
            current_level++;
        } else {
#ifdef FEAT_SAVE
            // Past the last level with unbeaten ones still left: wrap to the
            // first unbeaten one.
            current_level = save_get_resume_level();
#else
            // No save, so nothing records what is beaten: wrap to the start.
            current_level = 0;
#endif
        }
        STATE_GOTO(STATE_GAME);
#elif defined(FEAT_END_SCREEN)
        current_state = STATE_ALL_LEVELS_COMPLETE;
#elif defined(FEAT_TITLE_SCREEN)
        current_state = STATE_TITLE;
#else
        // Only one level: replay it.
        current_level = 0;
        STATE_GOTO(STATE_GAME);
#endif
#ifdef FEAT_LEVEL_SELECT
    } else if (choice == 2) {
        current_state = STATE_LEVEL_SELECT;
#endif
    }
}
#ifdef FEAT_LEVEL_SELECT

// ====================================================================
// STATE_LEVEL_SELECT
// ====================================================================

static uint8_t   selected_level;
static GameState saved_state;  // PAUSE or LEVEL_CLEAR we came from

static void show_preview(void) {
    const LevelDef *level    = &levels[selected_level];
    int16_t player_pixel_x = (int16_t)level->player_x << CELL_SHIFT;
    int16_t player_pixel_y = (int16_t)level->player_y << CELL_SHIFT;
    uint8_t player_screen_x, player_screen_y;
    uint16_t best_moves;

    // A whole-map repaint takes more than a frame. DISPLAY_OFF hides the bands.
    DISPLAY_OFF;
    level_preview(selected_level);
#ifdef FEAT_BOXES
    box_draw_preview(selected_level);
#endif
    camera_set_for_level_dims((uint16_t)level->width  << CELL_SHIFT,
                              (uint16_t)level->height << CELL_SHIFT,
                              player_pixel_x, player_pixel_y);

    player_screen_x = (uint8_t)camera_to_screen_x(player_pixel_x);
    player_screen_y = (uint8_t)camera_to_screen_y(player_pixel_y);
    player_draw_preview(player_screen_x, player_screen_y);

#ifdef FEAT_SAVE
    best_moves = save_get_best_moves(selected_level);
#else
    best_moves = 0;
#endif
    ui_win_draw_browse(HUD_ROW_PLAYING, (uint16_t)(selected_level + 1), best_moves);
    move_win(WIN_OVERLAY_X, WIN_OVERLAY_Y);
    DISPLAY_ON;
}

void select_init(void) {
    saved_state    = previous_state;
    selected_level = current_level;
    show_preview();
}

void select_update(void) {
    if (KEY_CONFIRM) {
#ifdef FEAT_SND_MENU_SELECT
        sound_play(&sfx_menu_select);
#endif
        current_level = selected_level;
        STATE_GOTO(STATE_GAME);
        return;
    }

    if (KEY_TICKED(J_B)) {
        if (saved_state == STATE_LEVEL_CLEAR) {
            // Entered from level-clear. Nothing in-progress to resume.
#ifdef FEAT_TITLE_SCREEN
            current_state = STATE_TITLE;
#else
            current_state = STATE_GAME;
#endif
        } else {
            // Restore the paused game and go back to the pause menu.
            DISPLAY_OFF;
            level_render_full();
#ifdef FEAT_BOXES
            box_draw_all();
#endif
            camera_center_on(player.pixel_x >> SUBPIXEL_SHIFT, player.pixel_y >> SUBPIXEL_SHIFT);
            camera_flush_scroll();
            render_actors();
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
#ifdef FEAT_SND_MENU_MOVE
    sound_play(&sfx_menu_move);
#endif
    show_preview();
}
#endif
#ifdef FEAT_END_SCREEN

void all_levels_complete_init(void) {
    camera_reset();
    ui_text_screen_begin();
    ui_text_block_centered(ALL_LEVELS_COMPLETE_TEXT);
    ui_text_screen_end();
}

void all_levels_complete_update(void) {
    if (KEY_CONFIRM) {
#ifdef FEAT_TITLE_SCREEN
        current_state = STATE_TITLE;
#else
        current_state = STATE_GAME;
#endif
    }
}
#endif
