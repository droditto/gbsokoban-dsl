package com.drodo.gbsokoban.generator

/**
 * Emits camera.h and camera.c. Mode is chosen by GBSokobanGenerator from the
 * level dimensions:
 * NONE - every level fits the screen exactly. camera_* collapse to empty macros.
 * CENTER - every level fits but at least one is smaller. camera_init pins a
 * fixed centering offset and runtime does nothing.
 * SCROLL - at least one level overflows the screen. Deadzone follow.
 */
class CameraGen {

	enum Mode { NONE, CENTER, SCROLL }

	def String cameraH(Mode mode) {
		switch mode {
			case NONE: cameraNoneH
			case CENTER: cameraCenterH
			case SCROLL: cameraScrollH
		}
	}

	// MODE.NONE is header-only and must not call this. CENTER and SCROLL emit a .c.
	def String cameraC(Mode mode) {
		switch mode {
			case CENTER: cameraCenterC
			case SCROLL: cameraScrollC
			case NONE:   throw new IllegalArgumentException("cameraC called with Mode.NONE")
		}
	}

	// ====================================================================
	// MODE_NONE - header-only: every camera_* collapses to an empty macro
	// ====================================================================

    private def String cameraNoneH() '''
    #ifndef CAMERA_H
    #define CAMERA_H

    #include <stdint.h>

    #define camera_to_screen_x(wx) ((int16_t)(wx))
    #define camera_to_screen_y(wy) ((int16_t)(wy))
    #define camera_init()                            ((void)0)
    #define camera_center_on_player()                ((void)0)
    #define camera_update()                          ((void)0)
    #define camera_set_for_level_dims(w, h, x, y)    ((void)0)
    #define camera_flush_scroll()                    ((void)0)
    #define camera_reset()                           ((void)0)

    #endif // CAMERA_H
    '''

	// ====================================================================
	// MODE_CENTER - one-time centering offset, runtime updates do nothing
	// ====================================================================

    private def String cameraCenterH() '''
    #ifndef CAMERA_H
    #define CAMERA_H

    #include <stdint.h>

    extern int16_t camera_x, camera_y;

    #define camera_to_screen_x(wx) ((int16_t)(wx) - camera_x)
    #define camera_to_screen_y(wy) ((int16_t)(wy) - camera_y)

    void camera_init(void);
    void camera_set_for_level_dims(uint16_t pixel_w, uint16_t pixel_h,
                                   int16_t  player_px, int16_t player_py);
    void camera_reset(void);

    // CENTER mode: offsets never change after camera_set_for_level_dims,
    // so per-frame scroll flush does nothing (SCX/SCY are written once there).
    #define camera_flush_scroll()                     ((void)0)
    #define camera_center_on_player()                 ((void)0)
    #define camera_update()                           ((void)0)

    #endif // CAMERA_H
    '''

    private def String cameraCenterC() '''
    #include "camera.h"
    #include "common.h"
    #include "level.h"
    #include <gb/gb.h>

    int16_t camera_x = 0;
    int16_t camera_y = 0;

    void camera_reset(void) {
        camera_x = 0;
        camera_y = 0;
        SCX_REG  = 0;
        SCY_REG  = 0;
    }

    void camera_set_for_level_dims(uint16_t pixel_w, uint16_t pixel_h,
                                   int16_t  player_px, int16_t player_py) {
        (void)player_px;
        (void)player_py;
        camera_x = CAMERA_CENTER_OFFSET(SCREENWIDTH,  pixel_w);
        camera_y = CAMERA_CENTER_OFFSET(SCREENHEIGHT, pixel_h);
        SCX_REG  = (uint8_t)camera_x;
        SCY_REG  = (uint8_t)camera_y;
    }

    void camera_init(void) {
        camera_set_for_level_dims((uint16_t)level_width  << CELL_SHIFT,
                                  (uint16_t)level_height << CELL_SHIFT, 0, 0);
    }
    '''

	// ====================================================================
	// MODE_SCROLL - deadzone follow plus on-demand recentering
	// ====================================================================

    private def String cameraScrollH() '''
    #ifndef CAMERA_H
    #define CAMERA_H

    #include <stdint.h>

    extern int16_t camera_x, camera_y;

    #define camera_to_screen_x(wx) ((int16_t)(wx) - camera_x)
    #define camera_to_screen_y(wy) ((int16_t)(wy) - camera_y)

    #define CAMERA_DEADZONE_X 40
    #define CAMERA_DEADZONE_Y 32

    void camera_init(void);
    void camera_center_on_player(void);
    void camera_update(void);
    void camera_set_for_level_dims(uint16_t pixel_w, uint16_t pixel_h,
                                   int16_t  player_px, int16_t player_py);
    void camera_flush_scroll(void);
    void camera_reset(void);

    #endif // CAMERA_H
    '''

    private def String cameraScrollC() '''
    #include "camera.h"
    #include "common.h"
    #include "level.h"
    #include "player.h"
    #include <gb/gb.h>

    // ====================================================================
    // State
    // ====================================================================

    int16_t camera_x = 0;
    int16_t camera_y = 0;

    static uint8_t scrollable_x, scrollable_y;  // set per axis when level > screen
    static int16_t cam_max_x,    cam_max_y;     // max scroll before off-level shows

    // ====================================================================
    // Helpers
    // ====================================================================

    // Called from the main loop right after vsync(). Writing SCX/SCY here
    // (vblank) instead of mid-frame avoids tearing on the top scanlines.
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

    // ====================================================================
    // Public API
    // ====================================================================

    void camera_init(void) {
        uint16_t level_pixel_w = (uint16_t)level_width  << CELL_SHIFT;
        uint16_t level_pixel_h = (uint16_t)level_height << CELL_SHIFT;
        scrollable_x = (level_pixel_w > SCREENWIDTH);
        scrollable_y = (level_pixel_h > SCREENHEIGHT);
        cam_max_x    = (int16_t)(level_pixel_w - SCREENWIDTH);
        cam_max_y    = (int16_t)(level_pixel_h - SCREENHEIGHT);
        if (!scrollable_x) camera_x = CAMERA_CENTER_OFFSET(SCREENWIDTH,  level_pixel_w);
        if (!scrollable_y) camera_y = CAMERA_CENTER_OFFSET(SCREENHEIGHT, level_pixel_h);
        camera_center_on_player();
    }

    void camera_center_on_player(void) {
        int16_t px = player.pixel_x >> SUBPIXEL_SHIFT;
        int16_t py = player.pixel_y >> SUBPIXEL_SHIFT;
        if (scrollable_x) camera_x = (int16_t)(px - (SCREENWIDTH  >> 1) + (CELL_PX >> 1));
        if (scrollable_y) camera_y = (int16_t)(py - (SCREENHEIGHT >> 1) + (CELL_PX >> 1));
        clamp_camera();
    }

    // Scroll only when the player is within DEADZONE_X/Y of a screen edge.
    void camera_update(void) {
        int16_t px = player.pixel_x >> SUBPIXEL_SHIFT;
        int16_t py = player.pixel_y >> SUBPIXEL_SHIFT;
        if (scrollable_x) {
            int16_t player_screen_x = (int16_t)(px - camera_x);
            if (player_screen_x > SCREENWIDTH - CAMERA_DEADZONE_X - CELL_PX)
                camera_x = (int16_t)(px - (SCREENWIDTH - CAMERA_DEADZONE_X - CELL_PX));
            if (player_screen_x < CAMERA_DEADZONE_X)
                camera_x = (int16_t)(px - CAMERA_DEADZONE_X);
        }
        if (scrollable_y) {
            int16_t player_screen_y = (int16_t)(py - camera_y);
            if (player_screen_y > SCREENHEIGHT - CAMERA_DEADZONE_Y - CELL_PX)
                camera_y = (int16_t)(py - (SCREENHEIGHT - CAMERA_DEADZONE_Y - CELL_PX));
            if (player_screen_y < CAMERA_DEADZONE_Y)
                camera_y = (int16_t)(py - CAMERA_DEADZONE_Y);
        }
        clamp_camera();
    }

    void camera_set_for_level_dims(uint16_t pixel_w, uint16_t pixel_h,
                                   int16_t  player_px, int16_t player_py) {
        if (pixel_w > SCREENWIDTH) {
            camera_x = (int16_t)(player_px - (SCREENWIDTH >> 1) + (CELL_PX >> 1));
            if (camera_x < 0) camera_x = 0;
            if (camera_x > (int16_t)(pixel_w - SCREENWIDTH))
                camera_x = (int16_t)(pixel_w - SCREENWIDTH);
        } else {
            camera_x = CAMERA_CENTER_OFFSET(SCREENWIDTH, pixel_w);
        }
        if (pixel_h > SCREENHEIGHT) {
            camera_y = (int16_t)(player_py - (SCREENHEIGHT >> 1) + (CELL_PX >> 1));
            if (camera_y < 0) camera_y = 0;
            if (camera_y > (int16_t)(pixel_h - SCREENHEIGHT))
                camera_y = (int16_t)(pixel_h - SCREENHEIGHT);
        } else {
            camera_y = CAMERA_CENTER_OFFSET(SCREENHEIGHT, pixel_h);
        }
    }
    '''
}
