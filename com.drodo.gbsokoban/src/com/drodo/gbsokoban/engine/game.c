#include "game.h"
#include "config.h"
#ifdef FEAT_BOXES
#include "box.h"
#endif
#include "camera.h"
#include "common.h"
#include "level.h"
#include "player.h"
#include "render.h"
#include "turn.h"
#include "ui.h"
#include <gb/gb.h>

// STATE_GAME: puts a level on the screen, then hands every frame to the turn machine.

void game_init(void) {
#ifdef FEAT_TITLE_SCREEN
    // A fullscreen state may have overwritten low VRAM. Reload the font.
    if (previous_state == STATE_TITLE) ui_init();
#endif
#ifdef FEAT_END_SCREEN
    if (previous_state == STATE_ALL_LEVELS_COMPLETE) ui_init();
#endif
    DISPLAY_OFF;
    SCX_REG = 0;
    SCY_REG = 0;

    level_load_tileset();
    player_load_sprites();
    level_load(current_level);
#ifdef FEAT_BOXES
    box_load(current_level);
#endif
    player_load(current_level);

    turn_reset();

    level_render_full();
#ifdef FEAT_BOXES
    box_draw_all();
#endif
    camera_init();
    camera_center_on(player.pixel_x >> SUBPIXEL_SHIFT, player.pixel_y >> SUBPIXEL_SHIFT);
    // First frame, drawn before the main loop's flush.
    camera_flush_scroll();
    ui_win_draw_playing(HUD_ROW_PLAYING, current_level + 1, move_count);
    move_win(WIN_OVERLAY_X, WIN_OVERLAY_Y);
    render_actors();

    SHOW_BKG;
    SHOW_SPRITES;
    SHOW_WIN;
    DISPLAY_ON;
}

// Entered from a menu drawn over the level. Nothing to reload.
void game_resume(void) {
    ui_win_draw_playing(HUD_ROW_PLAYING, current_level + 1, move_count);
    move_win(WIN_OVERLAY_X, WIN_OVERLAY_Y);
    SHOW_BKG;
    SHOW_SPRITES;
    SHOW_WIN;
    render_actors();
}

void game_update(void) {
    turn_update();
}
