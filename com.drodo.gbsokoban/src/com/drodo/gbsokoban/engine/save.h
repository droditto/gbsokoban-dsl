#ifndef SAVE_H
#define SAVE_H

#include "config.h"
#include "common.h"

// A blank cartridge or a resized game must not read as a save.
#define SAVE_SIGNATURE (0x5000u | TOTAL_LEVELS)

void     save_init(void);

// First index with best_moves == 0, or 0 if all levels are beaten; call
// save_all_levels_beaten() to tell the two cases apart.
uint8_t  save_get_resume_level(void);
uint16_t save_get_best_moves(uint8_t level);
#ifdef FEAT_SAVE_ALL_BEATEN
uint8_t  save_all_levels_beaten(void);
#endif
void     save_complete_level(uint8_t level, uint16_t moves);

#endif // SAVE_H
