package com.drodo.gbsokoban.validation;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.drodo.gbsokoban.gBSokoban.Crumble;
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.Sound;
import com.drodo.gbsokoban.gBSokoban.SoundChannel;
import com.drodo.gbsokoban.gBSokoban.SoundEvent;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.model.GoalTiles;

public class SoundValidator extends AbstractDeclarativeValidator {

	/** NR30 bit 7 is the wave channel's DAC, and a channel whose DAC is off is silent. */
	private static final int WAVE_DAC_ON = 0x80;

	private static final int MAX_REGISTER_VALUE = 0xFF;

	/** NRx2 bits 7-4 are the starting volume, so this is the lowest audible setting. */
	private static final int MIN_AUDIBLE_VOLUME = 0x10;

	/** NRx2 bit 3 set means the envelope climbs instead of fading. */
	private static final int ENVELOPE_CLIMBS = 0x08;

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	@Check
	public void checkOneSoundPerEvent(Game game) {
		Set<SoundEvent> declared = EnumSet.noneOf(SoundEvent.class);
		for (Sound sound : game.getSounds())
			if (sound.getEvent() != null && !declared.add(sound.getEvent()))
				error("'" + sound.getEvent().getLiteral() + "' already has a sound",
						sound, GBSokobanPackage.Literals.SOUND__EVENT, INSIGNIFICANT_INDEX);
	}

	@Check
	public void checkSoundEventCanHappen(Sound sound) {
		Game game = EcoreUtil2.getContainerOfType(sound, Game.class);
		if (game == null || sound.getEvent() == null)
			return;
		String missing = null;
		switch (sound.getEvent()) {
			case PUSH:
				if (game.getObjects().isEmpty()) missing = "no objects to push";
				break;
			case ON_GOAL:
				if (GoalTiles.owners(game).isEmpty()) missing = "no goals for an object to reach";
				break;
			case DESTROY:
				if (game.getObjects().isEmpty() || !GBSokobanValidator.destroysObjects(game))
					missing = "nothing that destroys objects";
				break;
			case COLLAPSE:
				if (!collapses(game)) missing = "no tile that collapses";
				break;
			default:
				break;
		}
		if (missing != null)
			warning("This game has " + missing + ", so '" + sound.getEvent().getLiteral()
					+ "' never plays", GBSokobanPackage.Literals.SOUND__EVENT,
					GBSokobanValidator.ISSUE_SOUND_NEVER_PLAYS);
	}

	private static boolean collapses(Game game) {
		for (TileDef tile : game.getTiles())
			if (tile.getBehaviour() instanceof Crumble)
				return true;
		return false;
	}

	/** Each value goes straight into an APU register, which holds a byte. */
	@Check
	public void checkSoundRegisters(Sound sound) {
		checkRegisterFitsByte(sound, sound.getR0(), GBSokobanPackage.Literals.SOUND__R0);
		checkRegisterFitsByte(sound, sound.getR1(), GBSokobanPackage.Literals.SOUND__R1);
		checkRegisterFitsByte(sound, sound.getR2(), GBSokobanPackage.Literals.SOUND__R2);
		checkRegisterFitsByte(sound, sound.getR3(), GBSokobanPackage.Literals.SOUND__R3);
		checkRegisterFitsByte(sound, sound.getR4(), GBSokobanPackage.Literals.SOUND__R4);
	}

	private void checkRegisterFitsByte(Sound sound, int value, EStructuralFeature feature) {
		if (value > MAX_REGISTER_VALUE)
			error("Each sound value must be between 0 and " + MAX_REGISTER_VALUE, sound, feature, -1);
	}

	/** Only pulse 1 and the wave channel have an NRx0: the sweep and the DAC switch. */
	@Check
	public void checkFirstRegisterIsUsed(Sound sound) {
		SoundChannel channel = sound.getChannel();
		if ((channel == SoundChannel.NR2 || channel == SoundChannel.NR4) && sound.getR0() != 0)
			warning("Channel " + channel.getLiteral() + " ignores the first value. Use 0",
					sound, GBSokobanPackage.Literals.SOUND__R0, -1);
	}

	/**
	 * NRx2 is a volume envelope: bits 7-4 the starting volume, bit 3 the direction. Zero
	 * heading down leaves nothing to hear.
	 */
	@Check
	public void checkEnvelopeIsAudible(Sound sound) {
		SoundChannel channel = sound.getChannel();
		if (channel != SoundChannel.NR1 && channel != SoundChannel.NR2 && channel != SoundChannel.NR4)
			return;
		boolean silent = sound.getR2() < MIN_AUDIBLE_VOLUME && (sound.getR2() & ENVELOPE_CLIMBS) == 0;
		if (silent)
			warning("This sound is silent. Raise the third value to " + MIN_AUDIBLE_VOLUME + " or more",
					sound, GBSokobanPackage.Literals.SOUND__R2, -1);
	}

	@Check
	public void checkWaveChannelIsAudible(Sound sound) {
		if (sound.getChannel() == SoundChannel.NR3 && (sound.getR0() & WAVE_DAC_ON) == 0)
			warning("NR3 is silent unless its first value is " + WAVE_DAC_ON + " or more",
					sound, GBSokobanPackage.Literals.SOUND__R0, -1);
	}
}
