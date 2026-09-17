#ifndef PLAYER_H
#define PLAYER_H

#include <stdint.h>
#include "config.h"

// Index into <name>_frames[]. Set by game logic, read by player_draw.
#define PLAYER_ANIM_WALK 0
#define PLAYER_ANIM_PUSH 1
#ifdef FEAT_PULL
#define PLAYER_ANIM_PULL 2
#endif

typedef struct {
    uint8_t  grid_x, grid_y;
    int16_t  pixel_x, pixel_y;
    int8_t   move_dx, move_dy;
    int16_t  move_steps_remaining;
    uint8_t  anim_mode;
    uint8_t  direction;
    uint8_t  anim_frame;
} Player;

extern Player player;

void player_load(uint8_t level_num);
void player_init(uint8_t gx, uint8_t gy);
void player_load_sprites(void);

void player_start_move(int8_t dx, int8_t dy);
void player_animate_step(void);
void player_finish_move(void);

// Returns the next free OAM slot so callers can chain box_draw_sprites().
uint8_t player_draw(uint8_t oam_slot);
#ifdef FEAT_LEVEL_SELECT
void    player_draw_preview(uint8_t screen_x, uint8_t screen_y);
#endif

#endif // PLAYER_H
