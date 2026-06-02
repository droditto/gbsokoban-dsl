package com.drodo.gbsokoban.generator

import com.drodo.gbsokoban.gBSokoban.Game
import com.drodo.gbsokoban.gBSokoban.SoundBlock
import com.drodo.gbsokoban.gBSokoban.SoundChannel
import com.drodo.gbsokoban.gBSokoban.SoundSpec
import com.drodo.gbsokoban.util.Feature
import java.util.List
import java.util.Set

/**
 * Emits sound.h and sound.c: the SFX bank from the DSL `sounds` block plus
 * the hardware playback engine. Each role is optional. Only declared roles
 * emit a SoundDef. The module is gated by the SOUND feature flag.
 */
class SoundGen {

	def String soundH(Game game, Set<Feature> features) {
		val sounds = if (features.contains(Feature.SOUND)) game.sounds else null
		val present = presentSounds(sounds)
        '''
        #ifndef SOUND_H
        #define SOUND_H

        #include <stdint.h>

        // ====================================================================
        // Channel IDs (Game Boy sound channels)
        // ====================================================================

        #define SND_NR1 1  // pulse 1 (sweep + envelope)
        #define SND_NR2 2  // pulse 2 (envelope only)
        #define SND_NR3 3  // wave
        #define SND_NR4 4  // noise

        // ====================================================================
        // Type and API
        // ====================================================================

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
        «IF !present.empty»

        // ====================================================================
        // SFX bank
        // ====================================================================

        «FOR role : present»
        extern const SoundDef sfx_«role.key»;
        «ENDFOR»
        «ENDIF»

        #endif // SOUND_H
        '''
	}

	def String soundC(Game game, Set<Feature> features) {
		val sounds = if (features.contains(Feature.SOUND)) game.sounds else null
		val present = presentSounds(sounds)
        '''
        #include "sound.h"
        #include <gb/gb.h>
        «IF !present.empty»

        // ====================================================================
        // SFX bank
        // ====================================================================

        «FOR role : present»
        const SoundDef sfx_«role.key» = {«specDef(role.value)»};
        «ENDFOR»
        «ENDIF»

        // ====================================================================
        // Playback engine
        // ====================================================================

        void sound_init(void) {
            // Triangle waveform used as the default for wave RAM (0xFF30..0xFF3F).
            static const uint8_t wave[] = {
                0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF,
                0xFE, 0xDC, 0xBA, 0x98, 0x76, 0x54, 0x32, 0x10
            };

            NR52_REG = 0x80;  // master enable
            NR50_REG = 0x77;  // L/R volume max, VIN off
            NR51_REG = 0xFF;  // every channel to both outputs

            // GBC requires the DAC off while writing wave RAM, and memcpy
            // can't target volatile memory, so copy byte by byte.
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
                // Stop the wave channel before reconfiguring, then re-enable
                // the DAC and trigger.
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
        '''
	}

	/** Roles declared in `sounds {...}`, in canonical order, paired with their SoundSpec. */
	private def List<Pair<String, SoundSpec>> presentSounds(SoundBlock sounds) {
		val out = newArrayList
		if (sounds === null) return out
		if (sounds.playerMove !== null) out.add("player_move" -> sounds.playerMove)
		if (sounds.playerBlocked !== null) out.add("player_blocked" -> sounds.playerBlocked)
		if (sounds.boxPush !== null) out.add("box_push" -> sounds.boxPush)
		if (sounds.boxOnGoal !== null) out.add("box_on_goal" -> sounds.boxOnGoal)
		if (sounds.boxDestroyed !== null) out.add("box_destroyed" -> sounds.boxDestroyed)
		if (sounds.levelComplete !== null) out.add("level_complete" -> sounds.levelComplete)
		if (sounds.levelRestart !== null) out.add("level_restart" -> sounds.levelRestart)
		if (sounds.crumble !== null) out.add("crumble" -> sounds.crumble)
		if (sounds.menuMove !== null) out.add("menu_move" -> sounds.menuMove)
		if (sounds.menuSelect !== null) out.add("menu_select" -> sounds.menuSelect)
		out
	}

	private def String specDef(SoundSpec spec) {
        '''«channelId(spec.channel)», «String.format("0x%02X", spec.r0)», «String.format("0x%02X", spec.r1)», «String.format("0x%02X", spec.r2)», «String.format("0x%02X", spec.r3)», «String.format("0x%02X", spec.r4)»'''
	}

	private def String channelId(SoundChannel channel) {
		switch channel {
			case SoundChannel.NR1: "SND_NR1"
			case SoundChannel.NR2: "SND_NR2"
			case SoundChannel.NR3: "SND_NR3"
			default: "SND_NR4"
		}
	}
}
