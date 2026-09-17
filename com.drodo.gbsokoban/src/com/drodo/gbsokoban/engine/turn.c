#include "turn.h"
#include "config.h"
#ifdef FEAT_BOXES
#include "box.h"
#endif
#include "camera.h"
#include "common.h"
#if defined(FEAT_BOXES) || defined(FEAT_CRUMBLE) || defined(FEAT_DEADLY)
#include "landing.h"
#endif
#include "level.h"
#include "player.h"
#include "render.h"
#ifdef FEAT_SOUND
#include "sound.h"
#endif
#include "ui.h"
#include "win.h"
#include <gb/gb.h>
#ifdef FEAT_DEADLY
#include <gb/metasprites.h>
#endif

// PHASE_IDLE reads input. PHASE_PLAYER_MOVING animates a move. When it
// finishes, finish_player_move resolves the push and check_player_tile
// fires post-move effects (death, ice, conveyor) before returning to idle.

#define PHASE_IDLE 0
#define PHASE_PLAYER_MOVING 1
#ifdef FEAT_SLIDING
// The two SLIDING phases handle ice chains.
#ifdef FEAT_BOXES
#define PHASE_BOX_SLIDING    2
#endif
#define PHASE_PLAYER_SLIDING 3
#endif

uint16_t move_count;

static uint8_t game_phase;
#ifdef FEAT_SND_PLAYER_BLOCKED
static uint8_t last_blocked_dir;
#endif
#ifdef FEAT_BOXES
static Box   *pushing_box;
#ifdef FEAT_PULL
static Box   *pulling_box;
#endif
#ifdef FEAT_SLIDING
static Box   *sliding_box;
static uint8_t slide_carries_player_motion;
#endif
#endif

// pixel_x is sub-pixel; the camera works in screen pixels.
#define camera_follow_player() \
    camera_follow(player.pixel_x >> SUBPIXEL_SHIFT, player.pixel_y >> SUBPIXEL_SHIFT)

#ifdef FEAT_SLIDING
#ifdef FEAT_BOXES
static uint8_t try_start_box_slide(Box *box, int8_t prev_dx, int8_t prev_dy);
#endif
static void    start_player_slide(int8_t dx, int8_t dy);
#endif
static void    check_player_tile(int8_t pdx, int8_t pdy);
static void    go_idle(void);

// ====================================================================
// Starting a move
// ====================================================================

// One per direction pressed. Ice and conveyors do not add another.
static void count_turn(void) {
    move_count++;
    ui_win_put_number(HUD_VALUE_R, HUD_ROW_PLAYING, move_count, HUD_COUNT_DIGITS);
#ifdef FEAT_SND_PLAYER_BLOCKED
    last_blocked_dir = INVALID_DIR;
#endif
}

#if defined(FEAT_SND_BOX_PUSH) || defined(FEAT_SND_PLAYER_MOVE)
static void play_step_sound(void) {
    if (player.anim_mode == PLAYER_ANIM_PUSH) {
#ifdef FEAT_SND_BOX_PUSH
        sound_play(&sfx_box_push);
#endif
    } else {
#ifdef FEAT_SND_PLAYER_MOVE
        sound_play(&sfx_player_move);
#endif
    }
}
#endif

#ifdef FEAT_SLIDING
// No traction on ice: the player slides instead of pushing.
static uint8_t try_move_on_ice(int8_t dx, int8_t dy) {
    start_player_slide(dx, dy);
    return game_phase != PHASE_IDLE;
}
#endif

static uint8_t enter_or_push(uint8_t nx, uint8_t ny, int8_t dx, int8_t dy) {
#ifdef FEAT_BOXES
    // A box with somewhere to go is pushed, not a wall.
    Box *box = box_find_at(nx, ny);
    if (box != NULL) {
        if (!box_can_move_to((uint8_t)(nx + dx), (uint8_t)(ny + dy)))
            return FALSE;
        player.anim_mode = PLAYER_ANIM_PUSH;
        pushing_box = box;
        box_start_move(box, dx, dy);
        return TRUE;
    }
#else
    (void)dx;
    (void)dy;
#endif
    return TILE_IS_PASSABLE(LOGIC_TILE(nx, ny)) ? TRUE : FALSE;
}

#if defined(FEAT_PULL) && defined(FEAT_BOXES)
// B held + not pushing: drag the box behind the player.
static void try_pull_box_behind(int8_t dx, int8_t dy) {
    uint8_t behind_x, behind_y;
    Box *box;

    if (player.anim_mode == PLAYER_ANIM_PUSH || !KEY_PRESSED(J_B))
        return;

    behind_x = player.grid_x - dx;
    behind_y = player.grid_y - dy;
    if (behind_x >= level_width || behind_y >= level_height)
        return;

    box = box_find_at(behind_x, behind_y);
    if (box == NULL)
        return;

    player.anim_mode = PLAYER_ANIM_PULL;
    pulling_box = box;
    box_start_move(box, dx, dy);
}
#endif

// Validates a player move and sets up animation. On ice it delegates to
// start_player_slide instead of pushing. Returns TRUE on success.
static uint8_t try_move_player(int8_t dx, int8_t dy, uint8_t own_step) {
    uint8_t nx, ny;
#if !defined(FEAT_PULL) || !defined(FEAT_BOXES)
    (void)own_step;
#endif

    player.anim_mode = PLAYER_ANIM_WALK;

#ifdef FEAT_SLIDING
    if (TILE_IS_ICE(LOGIC_TILE(player.grid_x, player.grid_y)))
        return try_move_on_ice(dx, dy);
#endif

    nx = player.grid_x + dx;
    ny = player.grid_y + dy;
    if (nx >= level_width || ny >= level_height)
        return FALSE;
    if (!enter_or_push(nx, ny, dx, dy))
        return FALSE;

#if defined(FEAT_PULL) && defined(FEAT_BOXES)
    // own_step only: being carried is not a pull.
    if (own_step)
        try_pull_box_behind(dx, dy);
#endif

    player_start_move(dx, dy);
    game_phase = PHASE_PLAYER_MOVING;
    return TRUE;
}

static void animate_blocked_push(void) {
#ifdef FEAT_SND_PLAYER_BLOCKED
    if (player.direction != last_blocked_dir)
        sound_play(&sfx_player_blocked);
    last_blocked_dir = player.direction;
#endif
    player.anim_mode = PLAYER_ANIM_PUSH;
    player.anim_frame += PLAYER_ANIM_SPEED;
    if ((player.anim_frame >> SUBPIXEL_SHIFT) >= PLAYER_ANIM_FRAMES)
        player.anim_frame = 0;
}

// ====================================================================
// Finishing a move
// ====================================================================

#ifdef FEAT_BOXES
// The pushed box has stopped. Land it, unless it slides on.
static uint8_t resolve_pushed_box(void) {
#ifdef FEAT_SLIDING
    // The direction a slide on ice would carry it.
    int8_t prev_dx = pushing_box->move_dx;
    int8_t prev_dy = pushing_box->move_dy;
#endif
    box_finish_move(pushing_box);

#ifdef FEAT_SLIDING
    slide_carries_player_motion = TRUE;
    if (try_start_box_slide(pushing_box, prev_dx, prev_dy)) {
        player.anim_mode = PLAYER_ANIM_WALK;
        pushing_box = NULL;
        return TRUE;
    }
#endif

    landing_box(pushing_box);
    player.anim_mode = PLAYER_ANIM_WALK;
    pushing_box = NULL;
    return FALSE;
}

// TRUE when the box drives the next phase itself.
static uint8_t finish_player_resolve_push(void) {
    player_finish_move();
    if (player.anim_mode == PLAYER_ANIM_PUSH && pushing_box != NULL) {
        if (resolve_pushed_box()) {
#ifdef FEAT_CRUMBLE
            landing_player();
#endif
            return TRUE;
        }
    }
    return FALSE;
}
#endif

static void finish_player_move(void) {
    int8_t pdx = player.move_dx;
    int8_t pdy = player.move_dy;

#ifdef FEAT_BOXES
    if (finish_player_resolve_push())
        return;
#else
    player_finish_move();
#endif

#ifdef FEAT_CRUMBLE
    // Must run before the pulled box settles: the box lands on the player's old cell,
    // so it needs the post-crumble tile in place (a crumble that turns into a pit must
    // kill the box).
    landing_player();
#endif

#if defined(FEAT_PULL) && defined(FEAT_BOXES)
    if (player.anim_mode == PLAYER_ANIM_PULL && pulling_box != NULL) {
        box_finish_move(pulling_box);
        landing_box(pulling_box);
        player.anim_mode = PLAYER_ANIM_WALK;
        pulling_box = NULL;
    }
#endif
    check_player_tile(pdx, pdy);
}

#ifdef FEAT_SLIDING

// ====================================================================
// Sliding
// ====================================================================

#ifdef FEAT_BOXES
// Starts a sliding box if it sits on ice and the next cell is free.
static uint8_t try_start_box_slide(Box *box, int8_t prev_dx, int8_t prev_dy) {
    uint8_t tile;
    int8_t  dx, dy;
    uint8_t nx, ny;

#ifdef FEAT_DESTROYABLE
    if (box->destroyed)
        return FALSE;
#endif

    tile = LOGIC_TILE(box->grid_x, box->grid_y);
    if (!TILE_IS_ICE(tile))
        return FALSE;

    dx = prev_dx;
    dy = prev_dy;
    nx = box->grid_x + dx;
    ny = box->grid_y + dy;
    if (!box_can_move_to(nx, ny))
        return FALSE;

    sliding_box = box;
    box_start_move(box, dx, dy);
    game_phase = PHASE_BOX_SLIDING;
    return TRUE;
}

static void finish_box_slide(void) {
    int8_t prev_dx = sliding_box->move_dx;
    int8_t prev_dy = sliding_box->move_dy;

    box_finish_move(sliding_box);

    if (try_start_box_slide(sliding_box, prev_dx, prev_dy))
        return;

    landing_box(sliding_box);
    sliding_box = NULL;

    if (slide_carries_player_motion) {
        slide_carries_player_motion = FALSE;
        check_player_tile(prev_dx, prev_dy);
    } else {
        go_idle();
    }
}
#endif

static void start_player_slide(int8_t dx, int8_t dy) {
    uint8_t nx = player.grid_x + dx;
    uint8_t ny = player.grid_y + dy;
#ifdef FEAT_BOXES
    Box    *box;
#endif

    if (nx >= level_width || ny >= level_height) {
        go_idle();
        return;
    }

#ifdef FEAT_BOXES
    box = box_find_at(nx, ny);
    if (box != NULL) {
        // No traction on ice: the box slides off, billiard-style.
        slide_carries_player_motion = FALSE;
        if (try_start_box_slide(box, dx, dy)) {
            player.anim_mode = PLAYER_ANIM_PUSH;
            return;
        }
        go_idle();
        return;
    }
#endif
    if (!TILE_IS_PASSABLE(LOGIC_TILE(nx, ny))) {
        go_idle();
        return;
    }
    player.anim_mode = PLAYER_ANIM_WALK;
    player.direction = DIR_FROM_DELTA(dx, dy);
    player_start_move(dx, dy);
    game_phase = PHASE_PLAYER_SLIDING;
}
#endif

// ====================================================================
// Turn end
// ====================================================================

// Post-move tile effects: death, ice slide, conveyor re-entry. Falls through to
// go_idle when nothing applies.
static void check_player_tile(int8_t pdx, int8_t pdy) {
#ifndef FEAT_SLIDING
    (void)pdx;
    (void)pdy;
#endif
#ifdef FEAT_DEADLY
    // Reload the level by re-entering STATE_GAME. Setting previous_state to
    // STATE_COUNT forces init() to fire even though current_state is unchanged.
    if (landing_kills_player()) {
        hide_sprites_range(0, MAX_HARDWARE_SPRITES);
        STATE_GOTO(STATE_GAME);
        return;
    }
#endif
#ifdef FEAT_SLIDING
    if (TILE_IS_ICE(LOGIC_TILE(player.grid_x, player.grid_y))) {
        start_player_slide(pdx, pdy);
        return;
    }
#endif
#ifdef FEAT_CONVEYOR
    // Re-enter try_move_player with the tile's direction. The chain continues because
    // finish_player_move calls back into here.
    {
        uint8_t tile = LOGIC_TILE(player.grid_x, player.grid_y);
        if (TILE_IS_CONVEYOR(tile)) {
            static const int8_t conveyor_dx[4] = {0, 0, -1, 1};
            static const int8_t conveyor_dy[4] = {1, -1, 0, 0};
            uint8_t dir = TILE_CONVEYOR_DIR(tile);
            int8_t  cdx = conveyor_dx[dir];
            int8_t  cdy = conveyor_dy[dir];
            player.direction = dir;
            if (try_move_player(cdx, cdy, FALSE))
                return;
        }
    }
#endif
    go_idle();
}

// Wraps up the turn: runs the win check, returns to PHASE_IDLE (or to
// STATE_LEVEL_CLEAR on win).
static void go_idle(void) {
    camera_follow_player();
    if (win_level_complete()) {
#ifdef FEAT_BOXES
        box_flush_all_to_bg();
#endif
        render_actors();
#ifdef FEAT_SND_LEVEL_COMPLETE
        sound_play(&sfx_level_complete);
#endif
        current_state = STATE_LEVEL_CLEAR;
        return;
    }
    render_actors();
    game_phase = PHASE_IDLE;
}

// ====================================================================
// Frame update
// ====================================================================

static void update_player_motion(void) {
    if (player.move_steps_remaining <= 0)
        return;

    player_animate_step();
#ifdef FEAT_BOXES
    if (player.anim_mode == PLAYER_ANIM_PUSH && pushing_box != NULL)
        box_animate_step(pushing_box);
#ifdef FEAT_PULL
    if (player.anim_mode == PLAYER_ANIM_PULL && pulling_box != NULL)
        box_animate_step(pulling_box);
#endif
#endif
    camera_follow_player();
    render_actors();

    if (player.move_steps_remaining <= 0)
        finish_player_move();
}

#if defined(FEAT_SLIDING) && defined(FEAT_BOXES)
static void update_box_slide(void) {
    if (sliding_box == NULL || sliding_box->move_steps_remaining <= 0)
        return;

    box_animate_step(sliding_box);
    render_actors();

    if (sliding_box->move_steps_remaining <= 0)
        finish_box_slide();
}
#endif

// The only phase that reads the d-pad.
static void update_idle(void) {
    int8_t dx = 0, dy = 0;

    if (KEY_TICKED(J_START)) {
#ifdef FEAT_BOXES
        box_flush_all_to_bg();
#endif
        current_state = STATE_PAUSE_MENU;
        return;
    }

    if      (KEY_PRESSED(J_UP))    { dy = -1; player.direction = DIR_UP; }
    else if (KEY_PRESSED(J_DOWN))  { dy =  1; player.direction = DIR_DOWN; }
    else if (KEY_PRESSED(J_LEFT))  { dx = -1; player.direction = DIR_LEFT; }
    else if (KEY_PRESSED(J_RIGHT)) { dx =  1; player.direction = DIR_RIGHT; }

    if (dx != 0 || dy != 0) {
        if (try_move_player(dx, dy, TRUE)) {
            count_turn();
#if defined(FEAT_SND_BOX_PUSH) || defined(FEAT_SND_PLAYER_MOVE)
            play_step_sound();
#endif
            camera_follow_player();
        } else {
            animate_blocked_push();
        }
    } else {
#ifdef FEAT_SND_PLAYER_BLOCKED
        last_blocked_dir  = INVALID_DIR;
#endif
        player.anim_mode  = PLAYER_ANIM_WALK;
        player.anim_frame = 0;
    }

#ifdef FEAT_BOXES
    box_flush_all_to_bg();
#endif
    render_actors();
}

// ====================================================================
// Public API
// ====================================================================

// Positions belong to the loaders.
void turn_reset(void) {
    move_count       = 0;
    game_phase       = PHASE_IDLE;
#ifdef FEAT_SND_PLAYER_BLOCKED
    last_blocked_dir = INVALID_DIR;
#endif
#ifdef FEAT_BOXES
    pushing_box      = NULL;
#ifdef FEAT_PULL
    pulling_box      = NULL;
#endif
#ifdef FEAT_SLIDING
    sliding_box                 = NULL;
    slide_carries_player_motion = FALSE;
#endif
#endif
#ifdef FEAT_CRUMBLE
    landing_reset();
#endif
}

void turn_update(void) {
    switch (game_phase) {
    case PHASE_PLAYER_MOVING:
#ifdef FEAT_SLIDING
    case PHASE_PLAYER_SLIDING:
#endif
        update_player_motion();
        return;
#if defined(FEAT_SLIDING) && defined(FEAT_BOXES)
    case PHASE_BOX_SLIDING:
        update_box_slide();
        return;
#endif
    default:
        update_idle();
        return;
    }
}
