#ifndef CAMERA_H
#define CAMERA_H

#include <stdint.h>
#include "config.h"

extern int16_t camera_x, camera_y;

#define camera_to_screen_x(wx) ((int16_t)(wx) - camera_x)
#define camera_to_screen_y(wy) ((int16_t)(wy) - camera_y)

void camera_init(void);
void camera_set_for_level_dims(uint16_t pixel_w, uint16_t pixel_h,
                               int16_t  player_px, int16_t player_py);
void camera_reset(void);

#if CAMERA_MODE == CAMERA_MODE_CENTER

// One-time centering offset; runtime updates do nothing.
#define camera_flush_scroll()    ((void)0)
#define camera_center_on(px, py) ((void)0)
#define camera_follow(px, py)    ((void)0)

#else

#define CAMERA_DEADZONE_X 40
#define CAMERA_DEADZONE_Y 32

void camera_center_on(int16_t px, int16_t py);
void camera_follow(int16_t px, int16_t py);
void camera_flush_scroll(void);

#endif

#endif // CAMERA_H
