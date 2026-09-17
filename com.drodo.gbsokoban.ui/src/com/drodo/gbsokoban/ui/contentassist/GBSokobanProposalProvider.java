package com.drodo.gbsokoban.ui.contentassist;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext;
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor;

import com.drodo.gbsokoban.gBSokoban.Animation;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.Sound;
import com.drodo.gbsokoban.gBSokoban.SoundChannel;
import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.model.AnimationChain;
import com.drodo.gbsokoban.model.Directions;
import com.drodo.gbsokoban.model.GameTextures;

import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.model.SymbolPool;
import com.drodo.gbsokoban.util.CellGeometry;
import com.drodo.gbsokoban.util.Speeds;

public class GBSokobanProposalProvider extends AbstractGBSokobanProposalProvider {

	@Override
	public void completeTileDef_Symbol(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		proposeFreeSymbol(model, context, acceptor);
	}

	@Override
	public void completeLegendEntry_Symbol(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		proposeFreeSymbol(model, context, acceptor);
	}

	@Override
	public void completeGame_MoveSpeed(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeGame_MoveSpeed(model, assignment, context, acceptor);
		propose(String.valueOf(Speeds.DEFAULT_MOVE_SPEED), Speeds.DEFAULT_MOVE_SPEED + " (the default)", context, acceptor);
	}

	@Override
	public void completeGame_AnimSpeed(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeGame_AnimSpeed(model, assignment, context, acceptor);
		Game game = EcoreUtil2.getContainerOfType(model, Game.class);
		if (game == null)
			return;
		int frames = AnimationChain.frameCount(game.getPlayer());
		if (frames == 0)
			return;
		int moveSpeed = game.getMoveSpeed() > 0 ? game.getMoveSpeed() : Speeds.DEFAULT_MOVE_SPEED;
		int derived = Speeds.animSpeedFor(frames, moveSpeed, GameTextures.cellPx(game));
		propose(String.valueOf(derived), derived + " (suggested)", context, acceptor);
	}

	@Override
	public void completeAnimation_Mirror(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeAnimation_Mirror(model, assignment, context, acceptor);
		if (!(model instanceof Animation) || ((Animation) model).getDirection() == null)
			return;
		String opposite = Directions.opposite(((Animation) model).getDirection()).getLiteral();
		propose("MIRROR " + opposite, "MIRROR " + opposite + " (flip the opposite direction)",
				context, acceptor);
	}

	@Override
	public void completeSound_R0(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeSound_R0(model, assignment, context, acceptor);
		if (!(model instanceof Sound) || ((Sound) model).getChannel() == null)
			return;
		SoundChannel channel = ((Sound) model).getChannel();
		if (channel == SoundChannel.NR3)
			propose("128", "128 (turns the wave channel on)", context, acceptor);
		else if (channel == SoundChannel.NR1)
			propose("0", "0 (no frequency sweep)", context, acceptor);
		else
			propose("0", "0 (this channel ignores it)", context, acceptor);
	}

	@Override
	public void completeTexture_Rows(EObject model, Assignment assignment,
			ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.completeTexture_Rows(model, assignment, context, acceptor);
		if (model instanceof Texture && !((Texture) model).getRows().isEmpty())
			return;
		Game game = EcoreUtil2.getContainerOfType(model, Game.class);
		int stated = game == null ? 0 : GameTextures.cellPx(game);
		if (CellGeometry.SUPPORTED_PX.contains(stated))
			proposeGrid(stated, context, acceptor);
		else
			for (int side : CellGeometry.SUPPORTED_PX)
				proposeGrid(side, context, acceptor);
	}

	private void proposeGrid(int side, ContentAssistContext context,
			ICompletionProposalAcceptor acceptor) {
		StringBuilder grid = new StringBuilder();
		for (int y = 0; y < side; y++) {
			if (y > 0)
				grid.append('\n');
			grid.append('"').append(".".repeat(side)).append('"');
		}
		propose(grid.toString(), "Insert " + side + "x" + side + " grid", context, acceptor);
	}

	private void proposeFreeSymbol(EObject model, ContentAssistContext context,
			ICompletionProposalAcceptor acceptor) {
		Game game = EcoreUtil2.getContainerOfType(model, Game.class);
		if (game == null)
			return;
		String name = model instanceof TileDef ? ((TileDef) model).getName() : "";
		String free = SymbolPool.firstUnusedFor(name, SymbolPool.usedIn(game));
		if (free != null)
			acceptor.accept(createCompletionProposal("\"" + free + "\"", context));
	}

	private void propose(String text, String display, ContentAssistContext context,
			ICompletionProposalAcceptor acceptor) {
		ICompletionProposal proposal = createCompletionProposal(text, display, null, context);
		if (proposal != null)
			acceptor.accept(proposal);
	}
}
