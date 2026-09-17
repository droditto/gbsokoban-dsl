#include "win.h"
#include "config.h"
#ifdef FEAT_BOXES
#include "box.h"
#endif
#include "common.h"
#include "level.h"
#include "player.h"

#include "win_data.h"

// One predicate per WIN_* type. A condition with nothing to check is met.

#if defined(FEAT_NO_TILE) || defined(FEAT_PLAYER_ON)
static uint8_t no_tile_left(uint8_t tile_id) {
    uint16_t i;
    for (i = 0; i < level_total_cells; i++) {
        if (logic_map[i] == tile_id)
            return FALSE;
    }
    return TRUE;
}
#endif

#if defined(FEAT_BOXES) && (defined(FEAT_ALL_ON) || defined(FEAT_SOME_ON) || defined(FEAT_NO_OBJECT))
static uint8_t box_is_live(const Box *box, uint8_t group) {
#ifdef FEAT_DESTROYABLE
    // Destroyed boxes are off the grid and out of the count.
    if (box->destroyed)
        return FALSE;
#endif
    return box->group == group;
}
#endif

#if defined(FEAT_BOXES) && (defined(FEAT_ALL_ON) || defined(FEAT_SOME_ON))
static uint8_t goal_holds_box(const GroupedPos *goal) {
    uint8_t b;
    for (b = 0; b < num_boxes; b++) {
        if (box_is_live(&boxes[b], goal->group) &&
            boxes[b].grid_x == goal->x && boxes[b].grid_y == goal->y)
            return TRUE;
    }
    return FALSE;
}
#endif

#if defined(FEAT_ALL_ON) && defined(FEAT_BOXES)
static uint8_t every_goal_covered(uint8_t group) {
    uint8_t g;
    for (g = 0; g < num_goals; g++) {
        if (goals[g].group == group && !goal_holds_box(&goals[g]))
            return FALSE;
    }
    return TRUE;
}
#endif

#if defined(FEAT_SOME_ON) && defined(FEAT_BOXES)
static uint8_t any_goal_covered(uint8_t group) {
    uint8_t g;
    uint8_t group_has_goals = FALSE;
    for (g = 0; g < num_goals; g++) {
        if (goals[g].group != group)
            continue;
        group_has_goals = TRUE;
        if (goal_holds_box(&goals[g]))
            return TRUE;
    }
    return !group_has_goals;
}
#endif

#if defined(FEAT_NO_OBJECT) && defined(FEAT_BOXES)
static uint8_t no_box_left(uint8_t group) {
    uint8_t b;
    for (b = 0; b < num_boxes; b++) {
        if (box_is_live(&boxes[b], group))
            return FALSE;
    }
    return TRUE;
}
#endif

#ifdef FEAT_PLAYER_ON
// No such tile means met, not unwinnable.
static uint8_t player_stands_on(uint8_t tile_id) {
    return LOGIC_TILE(player.grid_x, player.grid_y) == tile_id ||
           no_tile_left(tile_id);
}
#endif

static uint8_t win_condition_met(const WinCondition *wc) {
    switch (wc->type) {
#if defined(FEAT_ALL_ON) && defined(FEAT_BOXES)
    case WIN_ALL_ON:
        return every_goal_covered(wc->group);
#endif
#if defined(FEAT_SOME_ON) && defined(FEAT_BOXES)
    case WIN_SOME_ON:
        return any_goal_covered(wc->group);
#endif
#if defined(FEAT_NO_OBJECT) && defined(FEAT_BOXES)
    case WIN_NO_OBJECT:
        return no_box_left(wc->group);
#endif
#ifdef FEAT_NO_TILE
    case WIN_NO_TILE:
        return no_tile_left(wc->group);
#endif
#ifdef FEAT_PLAYER_ON
    case WIN_PLAYER_ON:
        return player_stands_on(wc->group);
#endif
    default:
        return TRUE;
    }
}

// The grammar requires at least one condition.
uint8_t win_level_complete(void) {
    uint8_t c;

    for (c = 0; c < num_win_conditions; c++) {
        if (!win_condition_met(&win_conditions[c]))
            return FALSE;
    }
    return TRUE;
}
