#include "sound.h"

#ifdef FEAT_SOUND

#include <gb/gb.h>

#include "sfx_data.h"

void sound_init(void) {
    // Triangle waveform used as the default for wave RAM (0xFF30..0xFF3F).
    static const uint8_t wave[] = {
        0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF,
        0xFE, 0xDC, 0xBA, 0x98, 0x76, 0x54, 0x32, 0x10
    };

    NR52_REG = 0x80;  // master enable
    NR50_REG = 0x77;  // L/R volume max, VIN off
    NR51_REG = 0xFF;  // every channel to both outputs

    // GBC requires the DAC off while writing wave RAM, and memcpy can't target
    // volatile memory, so copy byte by byte.
    NR30_REG = 0x00;
    {
        volatile uint8_t *wave_ram = (volatile uint8_t *)0xFF30;
        uint8_t i;
        for (i = 0; i < 16; i++) wave_ram[i] = wave[i];
    }
}

void sound_play(const SoundDef *snd) {
    switch (snd->channel) {
    case SND_NR1:
        NR10_REG = snd->r0;
        NR11_REG = snd->r1;
        NR12_REG = snd->r2;
        NR13_REG = snd->r3;
        NR14_REG = snd->r4;
        break;
    case SND_NR2:
        NR21_REG = snd->r1;
        NR22_REG = snd->r2;
        NR23_REG = snd->r3;
        NR24_REG = snd->r4;
        break;
    case SND_NR3:
        // Stop the wave channel before reconfiguring, then re-enable the DAC and trigger.
        NR30_REG = 0x00;
        NR31_REG = snd->r1;
        NR32_REG = snd->r2;
        NR33_REG = snd->r3;
        NR30_REG = snd->r0;
        NR34_REG = snd->r4;
        break;
    case SND_NR4:
        NR41_REG = snd->r1;
        NR42_REG = snd->r2;
        NR43_REG = snd->r3;
        NR44_REG = snd->r4;
        break;
    default:
        break;
    }
}

#endif // FEAT_SOUND
