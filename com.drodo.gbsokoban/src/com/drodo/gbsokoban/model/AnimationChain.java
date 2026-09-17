package com.drodo.gbsokoban.model;

import com.drodo.gbsokoban.gBSokoban.AnimKind;
import com.drodo.gbsokoban.gBSokoban.Animation;
import com.drodo.gbsokoban.gBSokoban.Direction;
import com.drodo.gbsokoban.gBSokoban.Player;

public final class AnimationChain {

	public static final int FLIP_X = 1;

	public static final int FLIP_Y = 2;

	public record Resolved(Animation source, int flips) {
	}

	private AnimationChain() {
	}

	public static Resolved follow(Player player, AnimKind kind, Direction direction) {
		return follow(player, kind, direction, 0);
	}

	public static int frameCount(Player player) {
		if (player == null)
			return 0;
		for (Animation anim : player.getAnims())
			if (anim.getKind() == AnimKind.WALK && anim.getFrames() != null
					&& !anim.getFrames().getTextures().isEmpty())
				return anim.getFrames().getTextures().size();
		return 0;
	}

	private static Resolved follow(Player player, AnimKind kind, Direction direction, int depth) {
		if (depth > Direction.values().length)
			return new Resolved(null, 0);
		for (Animation anim : player.getAnims()) {
			if (anim.getKind() != kind || anim.getDirection() != direction)
				continue;
			if (anim.getFrames() != null)
				return new Resolved(anim, 0);
			if (anim.getMirror() == null)
				return new Resolved(null, 0);
			Resolved base = follow(player, kind, anim.getMirror().getSource(), depth + 1);
			return new Resolved(base.source(), base.flips() | flipFacing(direction));
		}
		return new Resolved(null, 0);
	}

	private static int flipFacing(Direction direction) {
		return direction == Direction.LEFT || direction == Direction.RIGHT ? FLIP_X : FLIP_Y;
	}
}
