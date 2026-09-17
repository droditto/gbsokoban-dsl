#ifndef BOX_H
#define BOX_H

#include "config.h"
#include "common.h"

// Box-type properties (one per object kind), indexed by Box.group.
typedef struct {
    uint8_t cell;
#ifdef FEAT_ON_GOAL
    uint8_t on_goal_cell;
#endif
    uint8_t metasprite_idx;
    // Copy of metasprite_props[metasprite_idx], to save an indexed load per frame.
    uint8_t oam_props;
} BoxTypeProps;

// A moving box is drawn as a sprite (smooth motion). When it stops it gets committed
// to BG (frees OAM, cheaper to render). draw_as_sprite, bg_commit_pending and
// clear_origin_pending coordinate that handover.
typedef struct {
    uint8_t  grid_x, grid_y;
    int16_t  pixel_x, pixel_y;
    int8_t   move_dx, move_dy;
    int16_t  move_steps_remaining;
#ifdef FEAT_ON_GOAL
    uint8_t  on_goal;
#endif
#ifdef FEAT_DESTROYABLE
    uint8_t  destroyed;
#endif
    uint8_t  group;
    uint8_t  draw_as_sprite;
    uint8_t  bg_commit_pending;
    uint8_t  clear_origin_pending;
} Box;

extern const BoxTypeProps box_type_props[NUM_BOX_TYPES];
extern Box   boxes[MAX_BOXES];
extern uint8_t num_boxes;

Box   *box_find_at(uint8_t gx, uint8_t gy);
uint8_t box_can_move_to(uint8_t gx, uint8_t gy);
#ifdef FEAT_GOAL_LANDING
uint8_t box_on_its_goal(const Box *box);
#endif

void box_start_move(Box *box, int8_t dx, int8_t dy);
void box_animate_step(Box *box);
void box_finish_move(Box *box);

void    box_load(uint8_t level_num);
#ifdef FEAT_LEVEL_SELECT
void    box_draw_preview(uint8_t level_num);
#endif
void    box_draw_all(void);
void    box_draw_to_bg(const Box *box);
void    box_flush_all_to_bg(void);
uint8_t box_draw_sprites(uint8_t oam_slot);

#endif // BOX_H
