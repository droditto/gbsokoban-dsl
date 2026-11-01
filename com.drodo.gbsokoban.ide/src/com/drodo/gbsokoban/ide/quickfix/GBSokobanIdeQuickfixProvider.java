package com.drodo.gbsokoban.ide.quickfix;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.ide.editor.quickfix.AbstractDeclarativeIdeQuickfixProvider;
import org.eclipse.xtext.ide.editor.quickfix.DiagnosticResolutionAcceptor;
import org.eclipse.xtext.ide.editor.quickfix.ITextModification;
import org.eclipse.xtext.ide.editor.quickfix.QuickFix;
import org.eclipse.xtext.ide.server.Document;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com.drodo.gbsokoban.gBSokoban.Level;
import com.drodo.gbsokoban.gBSokoban.PaletteDef;
import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.validation.GBSokobanValidator;
import com.drodo.gbsokoban.validation.QuickFixes;

/** The same nine fixes as the Eclipse provider, as LSP code actions. */
public class GBSokobanIdeQuickfixProvider extends AbstractDeclarativeIdeQuickfixProvider {

	@QuickFix(GBSokobanValidator.ISSUE_DUPLICATE_SYMBOL)
	public void replaceDuplicateSymbol(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Use a free character", (ITextModification) (diagnostic, object, document) ->
				one(diagnostic.getRange(), QuickFixes.freeSymbol(object)));
	}

	@QuickFix(GBSokobanValidator.ISSUE_SYMBOL_LENGTH)
	public void trimSymbol(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Keep only the first character", (ITextModification) (diagnostic, object, document) ->
				one(diagnostic.getRange(), QuickFixes.firstCharacter(document.getSubstring(diagnostic.getRange()))));
	}

	@QuickFix(GBSokobanValidator.ISSUE_PULL_REQUIRES_CAN_PULL)
	public void allowPulling(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Add PLAYER_CAN_PULL", (ITextModification) (diagnostic, object, document) -> {
			Position start = new Position(0, 0);
			return one(new Range(start, start), QuickFixes.CAN_PULL_LINE);
		});
	}

	@QuickFix(GBSokobanValidator.ISSUE_LEGEND_NO_PLAYER)
	public void addPlayerToLegend(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Add a character for the player", (ITextModification) (diagnostic, object, document) -> {
			Position start = diagnostic.getRange().getStart();
			return one(new Range(start, start), QuickFixes.playerLegendLine(object));
		});
	}

	@QuickFix(GBSokobanValidator.ISSUE_LEVEL_TOO_MANY_PLAYERS)
	public void keepFirstPlayer(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Keep only the first player", rewrite(Level.class, QuickFixes::keepFirstPlayer));
	}

	@QuickFix(GBSokobanValidator.ISSUE_TEXTURE_SIZE)
	public void fitTextureToSize(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Resize the texture", rewrite(Texture.class, QuickFixes::fitTexture));
	}

	@QuickFix(GBSokobanValidator.ISSUE_PALETTE_SIZE)
	public void fitPaletteToRegister(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Resize the palette to 4 colors",
				rewrite(PaletteDef.class, (host, text) -> QuickFixes.fitPalette(text)));
	}

	@QuickFix(GBSokobanValidator.ISSUE_PULL_WITHOUT_OBJECTS)
	public void stopPulling(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Remove PLAYER_CAN_PULL",
				(ITextModification) (diagnostic, object, document) -> deleteLine(document,
						diagnostic.getRange().getStart().getLine()));
	}

	@QuickFix(GBSokobanValidator.ISSUE_SOUND_NEVER_PLAYS)
	public void removeUnreachableSound(DiagnosticResolutionAcceptor acceptor) {
		acceptor.accept("Remove the sound",
				(ITextModification) (diagnostic, object, document) -> deleteLine(document,
						diagnostic.getRange().getStart().getLine()));
	}

	/** Rewrites the host's text; getText() would include the comments above it. */
	private static <T extends EObject> ITextModification rewrite(Class<T> hostType,
			BiFunction<T, String, String> edit) {
		return (diagnostic, object, document) -> {
			T host = EcoreUtil2.getContainerOfType(object, hostType);
			ICompositeNode node = host == null ? null : NodeModelUtils.findActualNodeFor(host);
			if (node == null)
				return none();
			String updated = edit.apply(host, document.getContents().substring(node.getOffset(), node.getEndOffset()));
			Range range = new Range(document.getPosition(node.getOffset()), document.getPosition(node.getEndOffset()));
			return one(range, updated);
		};
	}

	private static List<TextEdit> deleteLine(Document document, int line) {
		Position start = new Position(line, 0);
		Position end = line + 1 < document.getLineCount() ? new Position(line + 1, 0)
				: new Position(line, document.getLineContent(line).length());
		return one(new Range(start, end), "");
	}

	private static List<TextEdit> one(Range range, String text) {
		return text == null ? none() : Collections.singletonList(new TextEdit(range, text));
	}

	private static List<TextEdit> none() {
		return Collections.emptyList();
	}
}
