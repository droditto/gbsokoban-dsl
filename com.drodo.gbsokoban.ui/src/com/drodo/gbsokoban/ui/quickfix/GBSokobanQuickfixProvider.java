package com.drodo.gbsokoban.ui.quickfix;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.editor.model.edit.IModification;
import org.eclipse.xtext.ui.editor.model.edit.IModificationContext;
import org.eclipse.xtext.ui.editor.quickfix.DefaultQuickfixProvider;
import org.eclipse.xtext.ui.editor.quickfix.Fix;
import org.eclipse.xtext.ui.editor.quickfix.IssueResolutionAcceptor;
import org.eclipse.xtext.validation.Issue;

import com.drodo.gbsokoban.gBSokoban.Level;
import com.drodo.gbsokoban.gBSokoban.PaletteDef;
import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.validation.GBSokobanValidator;
import com.drodo.gbsokoban.validation.QuickFixes;

public class GBSokobanQuickfixProvider extends DefaultQuickfixProvider {

	@Fix(GBSokobanValidator.ISSUE_DUPLICATE_SYMBOL)
	public void replaceDuplicateSymbol(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Use a free character", "Replaces it with a character nothing else uses.", null,
				(IModification) context -> replaceAt(issue, context, issue.getLength(), QuickFixes::freeSymbol));
	}

	@Fix(GBSokobanValidator.ISSUE_SYMBOL_LENGTH)
	public void trimSymbol(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Keep only the first character", "Deletes everything after the first character.", null,
				(IModification) context -> {
					IXtextDocument doc = context.getXtextDocument();
					String fixed = QuickFixes.firstCharacter(doc.get(issue.getOffset(), issue.getLength()));
					if (fixed != null)
						doc.replace(issue.getOffset(), issue.getLength(), fixed);
				});
	}

	@Fix(GBSokobanValidator.ISSUE_PULL_REQUIRES_CAN_PULL)
	public void allowPulling(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Add PLAYER_CAN_PULL", "Adds PLAYER_CAN_PULL to the game options.", null,
				(IModification) context -> context.getXtextDocument().replace(0, 0, QuickFixes.CAN_PULL_LINE));
	}

	@Fix(GBSokobanValidator.ISSUE_LEGEND_NO_PLAYER)
	public void addPlayerToLegend(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Add a character for the player", "Adds a legend entry that puts the player on a plain tile.",
				null, (IModification) context -> replaceAt(issue, context, 0, QuickFixes::playerLegendLine));
	}

	@Fix(GBSokobanValidator.ISSUE_LEVEL_TOO_MANY_PLAYERS)
	public void keepFirstPlayer(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Keep only the first player",
				"Replaces the extra players with a tile they can stand on.", null,
				(IModification) context -> rewrite(issue, context, Level.class, QuickFixes::keepFirstPlayer));
	}

	@Fix(GBSokobanValidator.ISSUE_TEXTURE_SIZE)
	public void fitTextureToSize(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Resize the texture", "Pads short rows with dots and deletes the extra ones.", null,
				(IModification) context -> rewrite(issue, context, Texture.class, QuickFixes::fitTexture));
	}

	@Fix(GBSokobanValidator.ISSUE_PALETTE_SIZE)
	public void fitPaletteToRegister(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Resize the palette to 4 colors", "Deletes the extra colors or repeats the last one.", null,
				(IModification) context -> rewrite(issue, context, PaletteDef.class,
						(host, text) -> QuickFixes.fitPalette(text)));
	}

	@Fix(GBSokobanValidator.ISSUE_PULL_WITHOUT_OBJECTS)
	public void stopPulling(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Remove PLAYER_CAN_PULL", "Deletes it: nothing in this game can be pulled.", null,
				(IModification) context -> {
					IXtextDocument doc = context.getXtextDocument();
					deleteLineAt(doc, doc.get().indexOf("PLAYER_CAN_PULL"));
				});
	}

	@Fix(GBSokobanValidator.ISSUE_SOUND_NEVER_PLAYS)
	public void removeUnreachableSound(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue, "Remove the sound", "Deletes it: nothing in this game triggers this event.", null,
				(IModification) context -> deleteLineAt(context.getXtextDocument(), issue.getOffset()));
	}

	private static void replaceAt(Issue issue, IModificationContext context, int length,
			Function<EObject, String> text) throws Exception {
		IXtextDocument doc = context.getXtextDocument();
		String fixed = doc.readOnly(resource -> text.apply(resource.getEObject(issue.getUriToProblem().fragment())));
		if (fixed != null)
			doc.replace(issue.getOffset(), length, fixed);
	}

	/** Replaces the text of the object the issue points at, computed from what is there now. */
	private static <T extends EObject> void rewrite(Issue issue, IModificationContext context, Class<T> hostType,
			BiFunction<T, String, String> edit) throws Exception {
		IXtextDocument doc = context.getXtextDocument();
		int[] region = new int[2];
		String updated = doc.readOnly(resource -> {
			EObject target = resource.getEObject(issue.getUriToProblem().fragment());
			T host = EcoreUtil2.getContainerOfType(target, hostType);
			ICompositeNode node = host == null ? null : NodeModelUtils.findActualNodeFor(host);
			if (node == null)
				return null;
			region[0] = node.getOffset();
			region[1] = node.getLength();
			return edit.apply(host, node.getRootNode().getText().substring(node.getOffset(), node.getEndOffset()));
		});
		if (updated != null)
			doc.replace(region[0], region[1], updated);
	}

	/** Removes the whole line the offset falls on, line break included. */
	private static void deleteLineAt(IXtextDocument doc, int offset) throws BadLocationException {
		if (offset < 0)
			return;
		int line = doc.getLineOfOffset(offset);
		doc.replace(doc.getLineOffset(line), doc.getLineLength(line), "");
	}
}
