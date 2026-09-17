#include "camera.h"
#include "common.h"
#include "level.h"
#include <gb/gb.h>

int16_t camera_x = 0;
int16_t camera_y = 0;

#if CAMERA_MODE == CAMERA_MODE_CENTER

void camera_reset(void) {
    camera_x = 0;
    camera_y = 0;
    SCX_REG  = 0;
    SCY_REG  = 0;
}

// CENTER mode: every level fits, so player_px and player_py go unused.
void camera_set_for_level_dims(uint16_t pixel_w, uint16_t pixel_h,
                               int16_t  player_px, int16_t player_py) {
    (void)player_px;
    (void)player_py;
    camera_x = CAMERA_CENTER_OFFSET(SCREENWIDTH,  pixel_w);
    camera_y = CAMERA_CENTER_OFFSET(PLAY_H, pixel_h);
    SCX_REG  = (uint8_t)camera_x;
    SCY_REG  = (uint8_t)camera_y;
}

void camera_init(void) {
    camera_set_for_level_dims((uint16_t)level_width  << CELL_SHIFT,
                              (uint16_t)level_height << CELL_SHIFT, 0, 0);
}

#elif CAMERA_MODE == CAMERA_MODE_SCROLL

static uint8_t scrollable_x, scrollable_y;  // set per axis when level > screen
static int16_t cam_max_x,    cam_max_y;     // max scroll before off-level shows

// Called from the main loop right after vsync(). Writing SCX/SCY here (vblank)
// instead of mid-frame avoids tearing on the top scanlines.
void camera_flush_scroll(void) {
    SCX_REG = (uint8_t)camera_x;
    SCY_REG = (uint8_t)camera_y;
}

void camera_reset(void) {
    camera_x = 0;
    camera_y = 0;
}

static void clamp_camera(void) {
    if (scrollable_x) {
        if (camera_x < 0)         camera_x = 0;
        if (camera_x > cam_max_x) camera_x = cam_max_x;
    }
    if (scrollable_y) {
        if (camera_y < 0)         camera_y = 0;
        if (camera_y > cam_max_y) camera_y = cam_max_y;
    }
}

// An axis the level does not fill is centered here and never followed after.
static void set_bounds(uint16_t pixel_w, uint16_t pixel_h) {
    scrollable_x = (pixel_w > SCREENWIDTH);
    scrollable_y = (pixel_h > PLAY_H);
    cam_max_x    = (int16_t)(pixel_w - SCREENWIDTH);
    cam_max_y    = (int16_t)(pixel_h - PLAY_H);
    if (!scrollable_x) camera_x = CAMERA_CENTER_OFFSET(SCREENWIDTH, pixel_w);
    if (!scrollable_y) camera_y = CAMERA_CENTER_OFFSET(PLAY_H, pixel_h);
}

void camera_init(void) {
    set_bounds((uint16_t)level_width  << CELL_SHIFT,
               (uint16_t)level_height << CELL_SHIFT);
    camera_flush_scroll();
}

void camera_center_on(int16_t px, int16_t py) {
    if (scrollable_x) camera_x = (int16_t)(px - (SCREENWIDTH  >> 1) + (CELL_PX >> 1));
    if (scrollable_y) camera_y = (int16_t)(py - (PLAY_H >> 1) + (CELL_PX >> 1));
    clamp_camera();
}

// Scroll only when the player is within CAMERA_DEADZONE_X/Y of a screen edge.
void camera_follow(int16_t px, int16_t py) {
    if (scrollable_x) {
        int16_t player_screen_x = (int16_t)(px - camera_x);
        if (player_screen_x > SCREENWIDTH - CAMERA_DEADZONE_X - CELL_PX)
            camera_x = (int16_t)(px - (SCREENWIDTH - CAMERA_DEADZONE_X - CELL_PX));
        if (player_screen_x < CAMERA_DEADZONE_X)
            camera_x = (int16_t)(px - CAMERA_DEADZONE_X);
    }
    if (scrollable_y) {
        int16_t player_screen_y = (int16_t)(py - camera_y);
        if (player_screen_y > PLAY_H - CAMERA_DEADZONE_Y - CELL_PX)
            camera_y = (int16_t)(py - (PLAY_H - CAMERA_DEADZONE_Y - CELL_PX));
        if (player_screen_y < CAMERA_DEADZONE_Y)
            camera_y = (int16_t)(py - CAMERA_DEADZONE_Y);
    }
    clamp_camera();
}

void camera_set_for_level_dims(uint16_t pixel_w, uint16_t pixel_h,
                               int16_t  player_px, int16_t player_py) {
    set_bounds(pixel_w, pixel_h);
    camera_center_on(player_px, player_py);
    camera_flush_scroll();
}

#endif
