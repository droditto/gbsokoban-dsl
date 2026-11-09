package com.drodo.gbsokoban.validation;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.drodo.gbsokoban.gBSokoban.AnimKind;
import com.drodo.gbsokoban.gBSokoban.Animation;
import com.drodo.gbsokoban.gBSokoban.Direction;
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.Player;
import com.drodo.gbsokoban.model.AnimationChain;
import com.drodo.gbsokoban.model.Directions;
import com.drodo.gbsokoban.util.Messages;
import com.drodo.gbsokoban.util.Speeds;

public class PlayerValidator extends AbstractDeclarativeValidator {

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	@Check
	public void checkWalkAnimationExists(Player player) {
		for (Animation anim : player.getAnims())
			if (anim.getKind() == AnimKind.WALK)
				return;
		error(Messages.get("player.noWalk"), player, GBSokobanPackage.Literals.PLAYER__ANIMS, -1);
	}

	@Check
	public void checkPullHasObjects(Game game) {
		if (game.isCanPull() && game.getObjects().isEmpty())
			warning(Messages.get("player.pullWithoutObjects"),
					GBSokobanPackage.Literals.GAME__CAN_PULL, GBSokobanValidator.ISSUE_PULL_WITHOUT_OBJECTS);
	}

	@Check
	public void checkPullRequiresCanPull(Player player) {
		Game game = EcoreUtil2.getContainerOfType(player, Game.class);
		if (game == null || game.isCanPull())
			return;
		for (Animation anim : player.getAnims())
			if (anim.getKind() == AnimKind.PULL) {
				error(Messages.get("player.pullNeedsCanPull"),
						anim, GBSokobanPackage.Literals.ANIMATION__KIND, -1, GBSokobanValidator.ISSUE_PULL_REQUIRES_CAN_PULL);
				return;
			}
	}

	/**
	 * The engine indexes the tables by direction and draws all four, so a direction leading
	 * nowhere emits an empty C initializer.
	 */
	@Check
	public void checkEveryDirectionReachesFrames(Player player) {
		Set<AnimKind> declared = new LinkedHashSet<>();
		for (Animation anim : player.getAnims())
			declared.add(anim.getKind());
		for (AnimKind kind : declared)
			for (Direction direction : Direction.values()) {
				if (AnimationChain.follow(player, kind, direction).source() != null)
					continue;
				String message = Messages.get("player.noFrames", kind.getLiteral() + " " + direction.getLiteral());
				Animation anim = declarationOf(player, kind, direction);
				if (anim == null)
					error(message, player, GBSokobanPackage.Literals.PLAYER__ANIMS, -1);
				else
					error(message, anim, GBSokobanPackage.Literals.ANIMATION__MIRROR, -1);
			}
	}

	private static Animation declarationOf(Player player, AnimKind kind, Direction direction) {
		for (Animation anim : player.getAnims())
			if (anim.getKind() == kind && anim.getDirection() == direction)
				return anim;
		return null;
	}

	@Check
	public void checkMirrorIsOpposite(Animation anim) {
		if (anim.getMirror() == null || anim.getDirection() == null)
			return;
		Direction source = anim.getMirror().getSource();
		Direction wanted = Directions.opposite(anim.getDirection());
		if (source != wanted)
			error(Messages.get("player.mirrorOpposite", wanted.getLiteral()),
					anim, GBSokobanPackage.Literals.ANIMATION__MIRROR, -1);
	}

	@Check
	public void checkFrameCountRange(Player player) {
		for (Animation anim : player.getAnims()) {
			if (anim.getFrames() == null)
				continue;
			int count = anim.getFrames().getTextures().size();
			if (count > Speeds.MAX_ANIM_FRAMES)
				error(Messages.get("player.tooManyFrames", Speeds.MAX_ANIM_FRAMES),
						anim, GBSokobanPackage.Literals.ANIMATION__FRAMES, -1);
		}
	}

	@Check
	public void checkFrameCountsMatch(Player player) {
		// The engine uses a single PLAYER_ANIM_FRAMES count for every direction.
		int expected = -1;
		for (Animation anim : player.getAnims()) {
			if (anim.getFrames() == null)
				continue;
			int count = anim.getFrames().getTextures().size();
			if (expected == -1)
				expected = count;
			else if (count != expected)
				error(Messages.get("player.framesMismatch", expected),
						anim, GBSokobanPackage.Literals.ANIMATION__FRAMES, -1);
		}
	}
}
