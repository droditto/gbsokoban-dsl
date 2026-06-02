package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.util.Feature
import java.util.Set

/**
 * Emits save.h and save.c: battery-backed SRAM with a RAM mirror so reads
 * don't have to toggle ENABLE_RAM. Layout is a 2-byte signature followed by
 * best_moves[TOTAL_LEVELS]. best_moves[i] == 0 means level i is unbeaten.
 */
class SaveGen {

    def String saveH(Set<Feature> features) '''
    #ifndef SAVE_H
    #define SAVE_H

    #include "common.h"

    // Low 12 bits embed the level count, so a save from a differently-sized
    // game fails the signature check and is discarded.
    #define SAVE_SIGNATURE (0x5000u | TOTAL_LEVELS)

    void     save_init(void);

    // First index with best_moves == 0, or 0 if all levels are beaten;
    // call save_all_levels_beaten() to tell the two cases apart.
    uint8_t  save_get_resume_level(void);
    uint16_t save_get_best_moves(uint8_t level);
    «IF needsAllBeaten(features)»
    uint8_t  save_all_levels_beaten(void);
    «ENDIF»
    void     save_complete_level(uint8_t level, uint16_t moves);

    #endif // SAVE_H
    '''

    def String saveC(Set<Feature> features) '''
    #include "save.h"
    #include "common.h"
    #include <gb/gb.h>
    #include <string.h>

    // ====================================================================
    // State
    // ====================================================================

    // External SRAM window. The cart battery preserves it across power cycles.
    #define SRAM_BASE 0xA000

    typedef struct {
        uint16_t signature;
        uint16_t best_moves[TOTAL_LEVELS];
    } SaveData;

    static SaveData save_data;

    // ====================================================================
    // Public API
    // ====================================================================

    void save_init(void) {
        volatile SaveData *sram = (volatile SaveData *)SRAM_BASE;
        ENABLE_RAM;

        if (sram->signature == SAVE_SIGNATURE) {
            // Byte loop keeps the volatile qualifier on the SRAM side.
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
        if (level < TOTAL_LEVELS) return save_data.best_moves[level];
        return 0;
    }

    «IF needsAllBeaten(features)»
    uint8_t save_all_levels_beaten(void) {
        uint8_t i;
        for (i = 0; i < TOTAL_LEVELS; i++)
            if (save_data.best_moves[i] == 0)
                return FALSE;
        return TRUE;
    }

    «ENDIF»
    void save_complete_level(uint8_t level, uint16_t moves) {
        volatile SaveData *sram = (volatile SaveData *)SRAM_BASE;

        if (level >= TOTAL_LEVELS || moves == 0)
            return;
        if (save_data.best_moves[level] != 0 && moves >= save_data.best_moves[level])
            return;

        save_data.best_moves[level] = moves;
        ENABLE_RAM;
        sram->best_moves[level] = moves;
        DISABLE_RAM;
    }
    '''

    /** Whether the emitted save module needs save_all_levels_beaten(). */
    private def boolean needsAllBeaten(Set<Feature> features) {
        features.contains(Feature.END_SCREEN) ||
        (features.contains(Feature.TITLE_SCREEN) && features.contains(Feature.MULTI_LEVEL))
    }
}
