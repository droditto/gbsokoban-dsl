#ifndef SOUND_H
#define SOUND_H

#include <stdint.h>
#include "config.h"

#ifdef FEAT_SOUND

#define SND_NR1 1  // pulse 1 (sweep + envelope)
#define SND_NR2 2  // pulse 2 (envelope only)
#define SND_NR3 3  // wave
#define SND_NR4 4  // noise

// r0: NR1 sweep / NR3 DAC enable (0x80 = on); r1..r4 map to NRx1..NRx4.
typedef struct {
    uint8_t channel;
    uint8_t r0;
    uint8_t r1;
    uint8_t r2;
    uint8_t r3;
    uint8_t r4;
} SoundDef;

void sound_init(void);
void sound_play(const SoundDef *snd);

#include "sfx.h"

#endif // FEAT_SOUND

#endif // SOUND_H
