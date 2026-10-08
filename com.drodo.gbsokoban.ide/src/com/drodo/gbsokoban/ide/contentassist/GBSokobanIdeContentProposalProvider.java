package com.drodo.gbsokoban.ide.contentassist;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ide.editor.contentassist.ContentAssistEntry;
import org.eclipse.xtext.ide.editor.contentassist.IIdeContentProposalAcceptor;
import org.eclipse.xtext.ide.editor.contentassist.IdeContentProposalProvider;

import com.drodo.gbsokoban.gBSokoban.Animation;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.Sound;
import com.drodo.gbsokoban.gBSokoban.SoundChannel;
import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.model.AnimationChain;
import com.drodo.gbsokoban.model.Directions;
import com.drodo.gbsokoban.model.GameTextures;
import com.drodo.gbsokoban.model.SymbolPool;
import com.drodo.gbsokoban.services.GBSokobanGrammarAccess;
import com.drodo.gbsokoban.util.CellGeometry;
import com.drodo.gbsokoban.util.Speeds;
import com.google.inject.Inject;

/** The same proposals as the Eclipse provider, for the language server. */
public class GBSokobanIdeContentProposalProvider extends IdeContentProposalProvider {

	@Inject
	private GBSokobanGrammarAccess grammar;

	@Override
	protected void _createProposals(Assignment assignment, ContentAssistContext context,
			IIdeContentProposalAcceptor acceptor) {
		if (assignment.getTerminal() instanceof CrossReference)
			super._createProposals(assignment, context, acceptor);
		EObject model = context.getCurrentModel();
		Game game = model == null ? null : EcoreUtil2.getContainerOfType(model, Game.class);

		if (assignment == grammar.getTileDefAccess().getSymbolAssignment_1()
				|| assignment == grammar.getLegendEntryAccess().getSymbolAssignment_0())
			proposeFreeSymbol(model, game, context, acceptor);
		else if (assignment == grammar.getGameAccess().getMoveSpeedAssignment_0_2_1())
			proposeMoveSpeed(game, context, acceptor);
		else if (assignment == grammar.getGameAccess().getAnimSpeedAssignment_0_3_1())
			proposeAnimSpeed(game, context, acceptor);
		else if (assignment == grammar.getAnimationAccess().getMirrorAssignment_2_1())
			proposeMirror(model, context, acceptor);
		else if (assignment == grammar.getSoundAccess().getR0Assignment_2())
			proposeFirstSoundValue(model, context, acceptor);
		else if (assignment == grammar.getTextureAccess().getRowsAssignment_3())
			proposeGrids(model, game, context, acceptor);
	}

	private void proposeFreeSymbol(EObject model, Game game, ContentAssistContext context,
			IIdeContentProposalAcceptor acceptor) {
		if (game == null)
			return;
		String name = model instanceof TileDef ? ((TileDef) model).getName() : "";
		String free = SymbolPool.firstUnusedFor(name, SymbolPool.usedIn(game));
		if (free != null)
			propose("\"" + free + "\"", "\"" + free + "\" (not used yet)", context, acceptor);
	}

	private void proposeMoveSpeed(Game game, ContentAssistContext context, IIdeContentProposalAcceptor acceptor) {
		int speed = Speeds.defaultMoveSpeed(GameTextures.cellPx(game));
		propose(String.valueOf(speed), speed + " (the default)", context, acceptor);
	}

	private void proposeAnimSpeed(Game game, ContentAssistContext context, IIdeContentProposalAcceptor acceptor) {
		if (game == null)
			return;
		int frames = AnimationChain.frameCount(game.getPlayer());
		if (frames == 0)
			return;
		int cellPx = GameTextures.cellPx(game);
		int moveSpeed = game.getMoveSpeed() > 0 ? game.getMoveSpeed() : Speeds.defaultMoveSpeed(cellPx);
		int derived = Speeds.animSpeedFor(frames, moveSpeed, cellPx);
		propose(String.valueOf(derived), derived + " (matches MOVE_SPEED)", context, acceptor);
	}

	private void proposeMirror(EObject model, ContentAssistContext context, IIdeContentProposalAcceptor acceptor) {
		if (!(model instanceof Animation) || ((Animation) model).getDirection() == null)
			return;
		String opposite = Directions.opposite(((Animation) model).getDirection()).getLiteral();
		propose("MIRROR " + opposite, "MIRROR " + opposite + " (reuses " + opposite + ", flipped)", context, acceptor);
	}

	private void proposeFirstSoundValue(EObject model, ContentAssistContext context,
			IIdeContentProposalAcceptor acceptor) {
		if (!(model instanceof Sound) || ((Sound) model).getChannel() == null)
			return;
		SoundChannel channel = ((Sound) model).getChannel();
		if (channel == SoundChannel.NR3)
			propose("128", "128 (turns NR3 on)", context, acceptor);
		else if (channel == SoundChannel.NR1)
			propose("0", "0 (no frequency sweep)", context, acceptor);
		else
			propose("0", "0 (ignored by this channel)", context, acceptor);
	}

	private void proposeGrids(EObject model, Game game, ContentAssistContext context,
			IIdeContentProposalAcceptor acceptor) {
		if (model instanceof Texture && !((Texture) model).getRows().isEmpty())
			return;
		int stated = game == null ? 0 : GameTextures.cellPx(game);
		if (CellGeometry.SUPPORTED_PX.contains(stated))
			proposeGrid(stated, context, acceptor);
		else
			for (int side : CellGeometry.SUPPORTED_PX)
				proposeGrid(side, context, acceptor);
	}

	private void proposeGrid(int side, ContentAssistContext context, IIdeContentProposalAcceptor acceptor) {
		StringBuilder grid = new StringBuilder();
		for (int y = 0; y < side; y++) {
			if (y > 0)
				grid.append('\n');
			grid.append('"').append(".".repeat(side)).append('"');
		}
		propose(grid.toString(), side + "x" + side + " grid (empty texture)", context, acceptor);
	}

	private void propose(String text, String label, ContentAssistContext context,
			IIdeContentProposalAcceptor acceptor) {
		ContentAssistEntry entry = getProposalCreator().createProposal(text, context, (e) -> e.setLabel(label));
		if (entry != null)
			acceptor.accept(entry, getProposalPriorities().getDefaultPriority(entry));
	}
}
