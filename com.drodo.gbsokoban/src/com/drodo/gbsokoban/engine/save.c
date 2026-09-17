#include "save.h"
#include "common.h"
#include <gb/gb.h>
#include <string.h>

// External SRAM window. The cart battery preserves it across power cycles.
#define SRAM_BASE 0xA000

typedef struct {
    uint16_t signature;
    uint16_t best_moves[TOTAL_LEVELS];
} SaveData;

static SaveData save_data;

void save_init(void) {
    volatile SaveData *sram = (volatile SaveData *)SRAM_BASE;
    // One bank is enough, and GBDK wants it chosen after ENABLE_RAM.
    ENABLE_RAM;
    SWITCH_RAM(0);

    if (sram->signature == SAVE_SIGNATURE) {
        // A byte loop keeps the volatile qualifier on the SRAM side.
        const volatile uint8_t *src = (const volatile uint8_t *)sram;
        uint8_t  *dst = (uint8_t *)&save_data;
        uint16_t  i;
        for (i = 0; i < sizeof(SaveData); i++)
            dst[i] = src[i];
    } else {
        // No valid save: initialise SRAM and the RAM mirror together.
        const uint8_t *src;
        uint16_t       i;
        save_data.signature = SAVE_SIGNATURE;
        memset(save_data.best_moves, 0, sizeof(save_data.best_moves));
        src = (const uint8_t *)&save_data;
        for (i = 0; i < sizeof(SaveData); i++)
            ((volatile uint8_t *)sram)[i] = src[i];
    }

    DISABLE_RAM;
}

uint8_t save_get_resume_level(void) {
    uint8_t i;
    for (i = 0; i < TOTAL_LEVELS; i++)
        if (save_data.best_moves[i] == 0)
            return i;
    return 0;
}

uint16_t save_get_best_moves(uint8_t level) {
    return save_data.best_moves[level];
}

#ifdef FEAT_SAVE_ALL_BEATEN
uint8_t save_all_levels_beaten(void) {
    uint8_t i;
    for (i = 0; i < TOTAL_LEVELS; i++)
        if (save_data.best_moves[i] == 0)
            return FALSE;
    return TRUE;
}
#endif

void save_complete_level(uint8_t level, uint16_t moves) {
    volatile SaveData *sram = (volatile SaveData *)SRAM_BASE;

    // 0 moves is possible: a level can be solved on arrival.
    if (moves == 0)
        return;
    if (save_data.best_moves[level] != 0 && moves >= save_data.best_moves[level])
        return;

    save_data.best_moves[level] = moves;
    ENABLE_RAM;
    SWITCH_RAM(0);
    sram->best_moves[level] = moves;
    DISABLE_RAM;
}
