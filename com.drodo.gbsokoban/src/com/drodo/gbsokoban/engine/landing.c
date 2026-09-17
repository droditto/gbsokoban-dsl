#include "landing.h"
#include "common.h"
#include "level.h"
#include "player.h"
#ifdef FEAT_SOUND
#include "sound.h"
#endif

#if (defined(FEAT_BOXES) && defined(FEAT_FILLABLE)) || defined(FEAT_CRUMBLE)
// Both fillable and crumble replace the tile via TILE_BECOMES(t).
static void transform_tile(uint8_t x, uint8_t y) {
    uint8_t target = TILE_BECOMES(LOGIC_TILE(x, y));
    LOGIC_TILE(x, y) = target;
    level_draw_cell(x, y, TILE_CELL(target));
}
#endif

#ifdef FEAT_BOXES

#ifdef FEAT_GOAL_LANDING
// Also clears it: a box can be pushed off a goal.
static void check_box_reached_goal(Box *box) {
    uint8_t home = box_on_its_goal(box);
#ifdef FEAT_ON_GOAL
    box->on_goal = home;
#endif
#ifdef FEAT_SND_BOX_ON_GOAL
    if (home)
        sound_play(&sfx_box_on_goal);
#endif
}
#endif

#ifdef FEAT_DESTROYABLE
// Fillable consumes the box and changes; deadly consumes and stays.
static void check_box_consumed(Box *box) {
    uint8_t tile = LOGIC_TILE(box->grid_x, box->grid_y);

#ifdef FEAT_FILLABLE
    if (TILE_IS_FILLABLE(tile)) {
        box->destroyed = TRUE;
        transform_tile(box->grid_x, box->grid_y);
#ifdef FEAT_SND_BOX_DESTROYED
        sound_play(&sfx_box_destroyed);
#endif
        return;
    }
#endif
    if (TILE_KILLS(tile)) {
        box->destroyed = TRUE;
        level_draw_cell(box->grid_x, box->grid_y, TILE_CELL(tile));
#ifdef FEAT_SND_BOX_DESTROYED
        sound_play(&sfx_box_destroyed);
#endif
    }
}
#endif

// Called when a box stops moving: marks the on-goal flag and fires fillable or
// destroy effects.
void landing_box(Box *box) {
    box->bg_commit_pending = TRUE;
#ifdef FEAT_GOAL_LANDING
    check_box_reached_goal(box);
#endif
#ifdef FEAT_DESTROYABLE
    check_box_consumed(box);
#endif
}
#endif

#ifdef FEAT_DEADLY

uint8_t landing_kills_player(void) {
    return TILE_KILLS(LOGIC_TILE(player.grid_x, player.grid_y));
}
#endif

#ifdef FEAT_CRUMBLE

// Crumble fires when the player LEAVES the cell. We remember it on arrival and
// collapse it on the next move.

static uint8_t held_crumble_x;
static uint8_t held_crumble_y;

void landing_reset(void) {
    held_crumble_x = INVALID_POS;
    held_crumble_y = INVALID_POS;
}

// Resolves the held crumble (the cell the player just left) and arms a new one if
// the player's current cell is also crumble.
void landing_player(void) {
    uint8_t tile;
    if (held_crumble_x != INVALID_POS) {
        uint8_t cx = held_crumble_x;
        uint8_t cy = held_crumble_y;
        held_crumble_x = INVALID_POS;
        if (TILE_IS_CRUMBLE(LOGIC_TILE(cx, cy))) {
            transform_tile(cx, cy);
#ifdef FEAT_SND_CRUMBLE
            sound_play(&sfx_crumble);
#endif
        }
    }
    tile = LOGIC_TILE(player.grid_x, player.grid_y);
    if (TILE_IS_CRUMBLE(tile)) {
        held_crumble_x = player.grid_x;
        held_crumble_y = player.grid_y;
    }
}
#endif
