#include "box.h"
#include "camera.h"
#include "common.h"
#include "level.h"
#include "render.h"
#include <string.h>
#include "assets.h"
#include <gb/gb.h>
#include <gb/metasprites.h>

#include "box_data.h"

Box     boxes[MAX_BOXES];
uint8_t num_boxes;

Box *box_find_at(uint8_t gx, uint8_t gy) {
    uint8_t i;
    for (i = 0; i < num_boxes; i++) {
#ifdef FEAT_DESTROYABLE
        if (boxes[i].destroyed) continue;
#endif
        if (boxes[i].grid_x == gx && boxes[i].grid_y == gy)
            return &boxes[i];
    }
    return NULL;
}

uint8_t box_can_move_to(uint8_t gx, uint8_t gy) {
    if (gx >= level_width || gy >= level_height)        return FALSE;
    if (!TILE_IS_PASSABLE(LOGIC_TILE(gx, gy)))          return FALSE;
    if (box_find_at(gx, gy) != NULL)                    return FALSE;
    return TRUE;
}

#ifdef FEAT_GOAL_LANDING
static uint8_t stands_on_goal(const GroupedPos *slots, uint8_t count,
                              uint8_t gx, uint8_t gy, uint8_t group) {
    uint8_t g;
    for (g = 0; g < count; g++) {
        if (slots[g].x == gx && slots[g].y == gy && slots[g].group == group)
            return TRUE;
    }
    return FALSE;
}

uint8_t box_on_its_goal(const Box *box) {
    return stands_on_goal(goals, num_goals, box->grid_x, box->grid_y, box->group);
}
#endif

void box_start_move(Box *box, int8_t dx, int8_t dy) {
    box->draw_as_sprite       = TRUE;
    box->bg_commit_pending    = FALSE;
    box->clear_origin_pending = TRUE;
    box->move_dx              = dx;
    box->move_dy              = dy;
    box->move_steps_remaining = CELL_PX << SUBPIXEL_SHIFT;
#ifdef FEAT_ON_GOAL
    box->on_goal = FALSE;
#endif
}

void box_animate_step(Box *box) {
    // Repaint the origin cell on the first step so the moving sprite doesn't leave a
    // ghost copy behind.
    if (box->clear_origin_pending) {
        uint8_t origin_tile = LOGIC_TILE(box->grid_x, box->grid_y);
        level_draw_cell(box->grid_x, box->grid_y, TILE_CELL(origin_tile));
        box->clear_origin_pending = FALSE;
    }
    if      (box->move_dx > 0) box->pixel_x += MOVE_SPEED;
    else if (box->move_dx < 0) box->pixel_x -= MOVE_SPEED;
    if      (box->move_dy > 0) box->pixel_y += MOVE_SPEED;
    else if (box->move_dy < 0) box->pixel_y -= MOVE_SPEED;
    box->move_steps_remaining -= MOVE_SPEED;
}

void box_finish_move(Box *box) {
    box->grid_x  += box->move_dx;
    box->grid_y  += box->move_dy;
    box->pixel_x  = GRID_TO_PIXEL(box->grid_x);
    box->pixel_y  = GRID_TO_PIXEL(box->grid_y);
    box->move_dx  = 0;
    box->move_dy  = 0;
    box->move_steps_remaining = 0;
}

void box_load(uint8_t level_num) {
    const LevelDef *level = &levels[level_clamp(level_num)];
    uint8_t i;

    num_boxes = level->num_boxes;
    for (i = 0; i < num_boxes; i++) {
        uint8_t spawn_x = level->boxes[i].x;
        uint8_t spawn_y = level->boxes[i].y;
        memset(&boxes[i], 0, sizeof(Box));
        boxes[i].grid_x  = spawn_x;
        boxes[i].grid_y  = spawn_y;
        boxes[i].pixel_x = GRID_TO_PIXEL(spawn_x);
        boxes[i].pixel_y = GRID_TO_PIXEL(spawn_y);
        boxes[i].group   = level->boxes[i].group;
#ifdef FEAT_ON_GOAL
        // on_goal is otherwise only set when a box finishes a move.
        boxes[i].on_goal = box_on_its_goal(&boxes[i]);
#endif
    }
}

// ====================================================================
// Rendering
// ====================================================================

#ifdef FEAT_LEVEL_SELECT
// Reads the level table, so a game in progress is untouched.
void box_draw_preview(uint8_t level_num) {
    const LevelDef *level = &levels[level_clamp(level_num)];
    uint8_t i;

    for (i = 0; i < level->num_boxes; i++) {
        const BoxTypeProps *props = &box_type_props[level->boxes[i].group];
        uint8_t cell = props->cell;
#ifdef FEAT_ON_GOAL
        if (stands_on_goal(level->goals, level->num_goals, level->boxes[i].x,
                           level->boxes[i].y, level->boxes[i].group))
            cell = props->on_goal_cell;
#endif
        level_draw_cell(level->boxes[i].x, level->boxes[i].y, cell);
    }
}
#endif

void box_draw_all(void) {
    uint8_t i;
    for (i = 0; i < num_boxes; i++) {
#ifdef FEAT_DESTROYABLE
        if (boxes[i].destroyed) continue;
#endif
        box_draw_to_bg(&boxes[i]);
    }
}

void box_draw_to_bg(const Box *box) {
    const BoxTypeProps *props = &box_type_props[box->group];
#ifdef FEAT_ON_GOAL
    uint8_t cell = box->on_goal ? props->on_goal_cell : props->cell;
#else
    uint8_t cell = props->cell;
#endif
    level_draw_cell(box->grid_x, box->grid_y, cell);
}

void box_flush_all_to_bg(void) {
    uint8_t i;
    for (i = 0; i < num_boxes; i++) {
        if (!boxes[i].bg_commit_pending) continue;
        boxes[i].bg_commit_pending = FALSE;
        boxes[i].draw_as_sprite    = FALSE;
#ifdef FEAT_DESTROYABLE
        if (boxes[i].destroyed) continue;
#endif
        box_draw_to_bg(&boxes[i]);
    }
}

uint8_t box_draw_sprites(uint8_t oam_slot) {
    uint8_t i;
    for (i = 0; i < num_boxes; i++) {
        const BoxTypeProps *props;
        int16_t screen_x, screen_y;
#ifdef FEAT_DESTROYABLE
        if (boxes[i].destroyed) continue;
#endif
        if (!boxes[i].draw_as_sprite) continue;
        props    = &box_type_props[boxes[i].group];
        screen_x = camera_to_screen_x(boxes[i].pixel_x >> SUBPIXEL_SHIFT);
        screen_y = camera_to_screen_y(boxes[i].pixel_y >> SUBPIXEL_SHIFT);
        oam_slot += move_metasprite_ex(metasprites[props->metasprite_idx], SPR_BASE,
                                       props->oam_props, oam_slot,
                                       (uint8_t)screen_x + SPRITE_OFFSET_X,
                                       (uint8_t)screen_y + SPRITE_OFFSET_Y);
    }
    return oam_slot;
}
