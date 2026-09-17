#ifndef LANDING_H
#define LANDING_H

#include <stdint.h>
#include "config.h"
#ifdef FEAT_BOXES
#include "box.h"
#endif


#ifdef FEAT_BOXES
void landing_box(Box *box);
#endif
#ifdef FEAT_DEADLY
// TILE_KILLS, but the player is reloaded rather than flagged.
uint8_t landing_kills_player(void);
#endif
#ifdef FEAT_CRUMBLE
void landing_player(void);
void landing_reset(void);
#endif

#endif // LANDING_H
