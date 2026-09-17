#include "config.h"
#include "camera.h"
#include "common.h"
#include "game.h"
#include "menus.h"
#ifdef FEAT_SAVE
#include "save.h"
#endif
#ifdef FEAT_SOUND
#include "sound.h"
#endif
#include "ui.h"
#include <gb/gb.h>

uint8_t   joypad_current  = 0;
uint8_t   joypad_previous = 0;

GameState current_state;
GameState previous_state;
uint8_t   current_level;

// Designated initialisers, so states[] cannot drift from the GameState enum.
static const StateDef states[STATE_COUNT] = {
#ifdef FEAT_TITLE_SCREEN
    [STATE_TITLE]               = {.init = title_init,  .update = title_update},
#endif
    [STATE_GAME]                = {.init = game_init,   .update = game_update},
    [STATE_PAUSE_MENU]          = {.init = pause_init,  .update = pause_update},
#ifdef FEAT_LEVEL_SELECT
    [STATE_LEVEL_SELECT]        = {.init = select_init, .update = select_update},
#endif
    [STATE_LEVEL_CLEAR]         = {.init = clear_init,  .update = clear_update},
#ifdef FEAT_END_SCREEN
    [STATE_ALL_LEVELS_COMPLETE] = {.init = all_levels_complete_init, .update = all_levels_complete_update},
#endif
};

void main(void) {
    DISPLAY_OFF;

    BGP_REG  = PALETTE_BGP;
    OBP0_REG = PALETTE_OBP0;
    OBP1_REG = PALETTE_OBP1;
#ifdef FEAT_TALL_SPRITES
    // 8x16 pairs tiles by index: a 16px cell is two side by side.
    SPRITES_8x16;
#else
    SPRITES_8x8;
#endif

    ui_init();
#ifdef FEAT_SAVE
    save_init();
#endif
#ifdef FEAT_SOUND
    sound_init();
#endif

#ifdef FEAT_TITLE_SCREEN
    current_state = STATE_TITLE;
#else
#ifdef FEAT_SAVE
    current_level = save_get_resume_level();
#else
    current_level = 0;
#endif
    current_state = STATE_GAME;
#endif
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
