package com.drodo.gbsokoban.ui.contentassist;

import static com.drodo.gbsokoban.util.ModelHelpers.hasExplicit;
import static com.drodo.gbsokoban.util.ModelHelpers.objectsOf;
import static com.drodo.gbsokoban.util.ModelHelpers.tilesOf;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor;

import com.drodo.gbsokoban.gBSokoban.AnimBlock;
import com.drodo.gbsokoban.gBSokoban.DirAnim;
import com.drodo.gbsokoban.gBSokoban.FrameList;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.PlayerDef;
import com.drodo.gbsokoban.gBSokoban.SoundChannel;
import com.drodo.gbsokoban.gBSokoban.SoundSpec;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.util.CharPool;

public class GBSokobanProposalProvider extends AbstractGBSokobanProposalProvider {

	private static final String LEGEND_CHAR_POOL =
			"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	// ==== Tile / sprite indices ====
	// tileIdx and onGoal share the background sheet, spriteIdx and player frames the sprite sheet.

	@Override
	public void completeTileDef_TileIdx(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		proposeFreeBgIdx(model, context, acceptor);
	}

	@Override
	public void completeObjectDef_TileIdx(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		proposeFreeBgIdx(model, context, acceptor);
	}

	@Override
	public void completeObjectDef_OnGoalTileIdx(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		proposeFreeBgIdx(model, context, acceptor);
	}

	@Override
	public void completeObjectDef_SpriteIdx(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		proposeFreeSpriteIdx(model, context, acceptor);
	}

	@Override
	public void completeFrameList_Frames(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeFrameList_Frames(model, assignment, context, acceptor);
		proposeFreeSpriteIdx(model, context, acceptor);
	}

	// ==== Speeds ====

	@Override
	public void completeGame_MovementSpeed(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeGame_MovementSpeed(model, assignment, context, acceptor);
		proposeInt(16, "default", context, acceptor);
	}

	@Override
	public void completeGame_AnimationSpeed(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeGame_AnimationSpeed(model, assignment, context, acceptor);
		proposeInt(2, "default", context, acceptor);
	}

	// ==== Legend characters ====

	@Override
	public void completeLegendEntry_Char(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeLegendEntry_Char(model, assignment, context, acceptor);
		Game game = EcoreUtil2.getContainerOfType(model, Game.class);
		if (game == null) return;
		String unusedChar = firstUnusedChar(game);
		if (unusedChar != null)
			propose("\"" + unusedChar + "\"", "\"" + unusedChar + "\" (unused)",
					context, acceptor);
	}

	// Propose flipX/flipY mirrors only when the opposite side is a FrameList,
	// so the cross-reference resolves.

	@Override
	public void completeAnimBlock_Right(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeAnimBlock_Right(model, assignment, context, acceptor);
		if (model instanceof AnimBlock && ((AnimBlock) model).getLeft() instanceof FrameList)
			propose("flipX left", "flipX left (mirror left)", context, acceptor);
	}

	@Override
	public void completeAnimBlock_Left(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeAnimBlock_Left(model, assignment, context, acceptor);
		if (model instanceof AnimBlock && ((AnimBlock) model).getRight() instanceof FrameList)
			propose("flipX right", "flipX right (mirror right)", context, acceptor);
	}

	@Override
	public void completeAnimBlock_Up(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeAnimBlock_Up(model, assignment, context, acceptor);
		if (model instanceof AnimBlock && ((AnimBlock) model).getDown() instanceof FrameList)
			propose("flipY down", "flipY down (mirror down)", context, acceptor);
	}

	@Override
	public void completeAnimBlock_Down(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeAnimBlock_Down(model, assignment, context, acceptor);
		if (model instanceof AnimBlock && ((AnimBlock) model).getUp() instanceof FrameList)
			propose("flipY up", "flipY up (mirror up)", context, acceptor);
	}

	// r0 maps to a different hardware register per channel. Suggest the safe value for each.

	@Override
	public void completeSoundSpec_R0(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeSoundSpec_R0(model, assignment, context, acceptor);
		if (!(model instanceof SoundSpec)) return;
		SoundChannel channel = ((SoundSpec) model).getChannel();
		if (channel == null) return;
		switch (channel) {
			case NR3:
				proposeInt(128, "DAC enable bit (NR30)", context, acceptor);
				break;
			case NR1:
				proposeInt(0, "no frequency sweep (NR10)", context, acceptor);
				break;
			case NR2:
			case NR4:
				proposeInt(0, "unused on this channel", context, acceptor);
				break;
		}
	}

	// ==== Helpers ====

	private void proposeFreeBgIdx(EObject model, ContentAssistContext context,
			ICompletionProposalAcceptor acceptor) {
		Game game = EcoreUtil2.getContainerOfType(model, Game.class);
		if (game != null)
			proposeInt(firstUnused(usedBgIndices(game)), "first unused background index",
					context, acceptor);
	}

	private void proposeFreeSpriteIdx(EObject model, ContentAssistContext context,
			ICompletionProposalAcceptor acceptor) {
		Game game = EcoreUtil2.getContainerOfType(model, Game.class);
		if (game != null)
			proposeInt(firstUnused(usedSpriteIndices(game)), "first unused sprite index",
					context, acceptor);
	}

	private static Set<Integer> usedBgIndices(Game game) {
		Set<Integer> used = new HashSet<>();
		for (TileDef tile : tilesOf(game))
			if (hasExplicit(tile, GBSokobanPackage.Literals.ENTITY__TILE_IDX))
				used.add(tile.getTileIdx());
		for (ObjectDef object : objectsOf(game)) {
			if (hasExplicit(object, GBSokobanPackage.Literals.ENTITY__TILE_IDX))
				used.add(object.getTileIdx());
			if (hasExplicit(object, GBSokobanPackage.Literals.OBJECT_DEF__ON_GOAL_TILE_IDX))
				used.add(object.getOnGoalTileIdx());
		}
		return used;
	}

	private static Set<Integer> usedSpriteIndices(Game game) {
		Set<Integer> used = new HashSet<>();
		for (ObjectDef object : objectsOf(game))
			if (hasExplicit(object, GBSokobanPackage.Literals.OBJECT_DEF__SPRITE_IDX))
				used.add(object.getSpriteIdx());
		PlayerDef player = game.getPlayer();
		if (player != null) {
			collectFrameIndices(player.getWalk(), used);
			collectFrameIndices(player.getPush(), used);
			collectFrameIndices(player.getPull(), used);
		}
		return used;
	}

	private static void collectFrameIndices(AnimBlock block, Set<Integer> used) {
		if (block == null) return;
		for (DirAnim dir : new DirAnim[]{block.getDown(), block.getUp(), block.getLeft(), block.getRight()})
			if (dir instanceof FrameList)
				for (Integer frame : ((FrameList) dir).getFrames())
					used.add(frame);
	}

	private static int firstUnused(Set<Integer> used) {
		int candidate = 0;
		while (used.contains(candidate)) candidate++;
		return candidate;
	}

	private static String firstUnusedChar(Game game) {
		return CharPool.firstUnusedFrom(CharPool.usedIn(game), LEGEND_CHAR_POOL);
	}

	private void proposeInt(int value, String hint,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		propose(String.valueOf(value), value + " (" + hint + ")", context, acceptor);
	}

	private void propose(String text, String display,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		ICompletionProposal proposal = createCompletionProposal(text, display, null, context);
		if (proposal != null) acceptor.accept(proposal);
	}
}
