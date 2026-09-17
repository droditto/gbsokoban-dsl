#include "render.h"
#include "config.h"
#ifdef FEAT_BOXES
#include "box.h"
#endif
#include "common.h"
#include "player.h"
#include <gb/gb.h>
#include <gb/metasprites.h>

// Hide the rest: a box just committed to BG would linger over it.
void render_actors(void) {
    uint8_t oam = player_draw(0);
#ifdef FEAT_BOXES
    oam = box_draw_sprites(oam);
#endif
    hide_sprites_range(oam, MAX_HARDWARE_SPRITES);
}
