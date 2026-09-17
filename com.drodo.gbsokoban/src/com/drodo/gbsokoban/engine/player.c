#include "player.h"
#include "camera.h"
#include "level.h"
#include "common.h"
#include "assets.h"
#include "render.h"
#include <gb/gb.h>
#include <gb/metasprites.h>
#include <string.h>

#ifdef FEAT_LEVEL_SELECT
#define PREVIEW_OAM_SLOT 0
#endif

#ifdef FEAT_MIRROR_ANY
// 2-bit field stored in <name>_mirror[]: bit 0 = flipX, bit 1 = flipY.
#define MIRROR_X    1
#define MIRROR_Y    2
#endif

Player player;

#include "player_data.h"

// Dispatches to the move_metasprite_* variant matching `mirror`.
static uint8_t draw_player_metasprite(uint8_t metasprite_idx, uint8_t mirror,
                                  uint8_t screen_x, uint8_t screen_y, uint8_t oam_slot) {
#ifdef FEAT_MIRROR_ANY
    switch (mirror) {
#ifdef FEAT_MIRROR_X
    case MIRROR_X:
        return move_metasprite_flipx(metasprites[metasprite_idx], SPR_BASE,
                                     metasprite_props[metasprite_idx],
                                     oam_slot, screen_x, screen_y);
#endif
#ifdef FEAT_MIRROR_Y
    case MIRROR_Y:
        return move_metasprite_flipy(metasprites[metasprite_idx], SPR_BASE,
                                     metasprite_props[metasprite_idx],
                                     oam_slot, screen_x, screen_y);
#endif
    default:
        return move_metasprite_ex(metasprites[metasprite_idx], SPR_BASE,
                                  metasprite_props[metasprite_idx],
                                  oam_slot, screen_x, screen_y);
    }
#else
    (void)mirror;
    return move_metasprite_ex(metasprites[metasprite_idx], SPR_BASE,
                              metasprite_props[metasprite_idx],
                              oam_slot, screen_x, screen_y);
#endif
}

void player_load(uint8_t level_num) {
    const LevelDef *level = &levels[level_clamp(level_num)];
    player_init(level->player_x, level->player_y);
}

void player_init(uint8_t gx, uint8_t gy) {
    memset(&player, 0, sizeof(Player));
    player.grid_x  = gx;
    player.grid_y  = gy;
    player.pixel_x = GRID_TO_PIXEL(gx);
    player.pixel_y = GRID_TO_PIXEL(gy);
}

void player_load_sprites(void) {
    set_sprite_data(0, SPRITE_TILE_COUNT, sprite_tiles);
}

void player_start_move(int8_t dx, int8_t dy) {
    player.move_dx              = dx;
    player.move_dy              = dy;
    player.move_steps_remaining = CELL_PX << SUBPIXEL_SHIFT;
    // Start on the second animation frame so motion is visible from the first rendered
    // frame. The modulo guards single-frame anims.
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

uint8_t player_draw(uint8_t oam_slot) {
    int16_t screen_x = camera_to_screen_x(player.pixel_x >> SUBPIXEL_SHIFT);
    int16_t screen_y = camera_to_screen_y(player.pixel_y >> SUBPIXEL_SHIFT);
    const uint8_t *frames       = walk_frames[player.direction];
    const uint8_t *mirror_table = walk_mirror;

#ifdef FEAT_PUSH_ANIM
    if (player.anim_mode == PLAYER_ANIM_PUSH) {
        frames       = push_frames[player.direction];
        mirror_table = push_mirror;
    }
#endif
#ifdef FEAT_PULL_ANIM
    if (player.anim_mode == PLAYER_ANIM_PULL) {
        frames       = pull_frames[player.direction];
        mirror_table = pull_mirror;
    }
#endif

    return draw_player_metasprite(frames[player.anim_frame >> SUBPIXEL_SHIFT],
                              mirror_table[player.direction],
                              (uint8_t)screen_x + SPRITE_OFFSET_X,
                              (uint8_t)screen_y + SPRITE_OFFSET_Y,
                              oam_slot);
}

#ifdef FEAT_LEVEL_SELECT
void player_draw_preview(uint8_t screen_x, uint8_t screen_y) {
    uint8_t metasprite_idx = walk_frames[DIR_DOWN][0];
    uint8_t oam_used = draw_player_metasprite(metasprite_idx, walk_mirror[DIR_DOWN],
                                          screen_x + SPRITE_OFFSET_X,
                                          screen_y + SPRITE_OFFSET_Y,
                                          PREVIEW_OAM_SLOT);
    hide_sprites_range(oam_used, MAX_HARDWARE_SPRITES);
}
#endif
