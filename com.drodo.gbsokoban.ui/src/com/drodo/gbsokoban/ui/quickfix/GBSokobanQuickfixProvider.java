package com.drodo.gbsokoban.ui.quickfix;

import static com.drodo.gbsokoban.util.ModelHelpers.legendEntriesOf;
import static com.drodo.gbsokoban.util.ModelHelpers.tilesOf;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.ui.editor.model.edit.ISemanticModification;
import org.eclipse.xtext.ui.editor.quickfix.DefaultQuickfixProvider;
import org.eclipse.xtext.ui.editor.quickfix.Fix;
import org.eclipse.xtext.ui.editor.quickfix.IssueResolutionAcceptor;
import org.eclipse.xtext.validation.Issue;

import com.drodo.gbsokoban.gBSokoban.Entity;
import com.drodo.gbsokoban.gBSokoban.GBSokobanFactory;
import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.LegendEntry;
import com.drodo.gbsokoban.gBSokoban.Level;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.ObjectRef;
import com.drodo.gbsokoban.gBSokoban.PlayerDef;
import com.drodo.gbsokoban.gBSokoban.PlayerRef;
import com.drodo.gbsokoban.gBSokoban.SolidTile;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.util.CharPool;
import com.drodo.gbsokoban.validation.GBSokobanValidator;

public class GBSokobanQuickfixProvider extends DefaultQuickfixProvider {

	@Fix(GBSokobanValidator.ISSUE_TILE_SHEET_NO_PNG)
	public void fixTileSheetExtension(Issue issue, IssueResolutionAcceptor acceptor) {
		appendPng(issue, acceptor, GBSokobanPackage.Literals.GAME__TILE_SHEET);
	}

	@Fix(GBSokobanValidator.ISSUE_SPRITE_SHEET_NO_PNG)
	public void fixSpriteSheetExtension(Issue issue, IssueResolutionAcceptor acceptor) {
		appendPng(issue, acceptor, GBSokobanPackage.Literals.GAME__SPRITE_SHEET);
	}

	@Fix(GBSokobanValidator.ISSUE_TITLE_SCREEN_NO_PNG)
	public void fixTitleScreenExtension(Issue issue, IssueResolutionAcceptor acceptor) {
		appendPng(issue, acceptor, GBSokobanPackage.Literals.GAME__TITLE_SCREEN);
	}

	@Fix(GBSokobanValidator.ISSUE_ENDING_SCREEN_NO_PNG)
	public void fixEndingScreenExtension(Issue issue, IssueResolutionAcceptor acceptor) {
		appendPng(issue, acceptor, GBSokobanPackage.Literals.GAME__ENDING_SCREEN);
	}

	private void appendPng(Issue issue, IssueResolutionAcceptor acceptor, EAttribute pathFeature) {
		acceptor.accept(issue,
				"Append .png extension",
				"Append .png to the " + pathFeature.getName() + " path.",
				null,
				(ISemanticModification) (element, context) -> {
					if (!(element instanceof Game)) return;
					Game game = (Game) element;
					game.eSet(pathFeature, withPngExtension((String) game.eGet(pathFeature)));
				});
	}

	private static String withPngExtension(String path) {
		if (path == null) return null;
		if (path.endsWith(".png")) return path;
		if (path.endsWith(".")) return path + "png";
		return path + ".png";
	}

	@Fix(GBSokobanValidator.ISSUE_LEGEND_NO_PLAYER)
	public void fixLegendNoPlayer(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue,
				"Insert default Player binding",
				"Add a Player legend entry on the first plain tile.",
				null,
				(ISemanticModification) (element, context) -> {
					if (!(element instanceof Game)) return;
					Game game = (Game) element;
					TileDef playerTile = pickPlayerTile(game);
					if (game.getLegend() == null || playerTile == null) return;
					LegendEntry entry = GBSokobanFactory.eINSTANCE.createLegendEntry();
					entry.setChar(pickUnusedChar(game, "Player"));
					entry.setEntity(GBSokobanFactory.eINSTANCE.createPlayerRef());
					entry.setTile(playerTile);
					game.getLegend().getEntries().add(entry);
				});
	}

	// Prefer the first plain tile (no type modifier). Fall back to the first declared tile.
	private static TileDef pickPlayerTile(Game game) {
		List<TileDef> tiles = tilesOf(game);
		for (TileDef tile : tiles)
			if (tile.getType() == null) return tile;
		return tiles.isEmpty() ? null : tiles.get(0);
	}

	// Sokoban-style symbols first (proven by the examples), then digits, then alpha.
	private static final String CHAR_POOL =
			".#~%*!@?+-^<>0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	// Pick a letter from the entity name when possible (Floor -> 'F', Player -> 'P').
	private static String pickUnusedChar(Game game, String preferredName) {
		Set<String> used = CharPool.usedIn(game);
		if (preferredName != null)
			for (int i = 0; i < preferredName.length(); i++) {
				String upper = String.valueOf(Character.toUpperCase(preferredName.charAt(i)));
				if (!used.contains(upper)) return upper;
				String lower = String.valueOf(Character.toLowerCase(preferredName.charAt(i)));
				if (!used.contains(lower)) return lower;
			}
		String fromPool = CharPool.firstUnusedFrom(used, CHAR_POOL);
		return fromPool != null ? fromPool : "?";
	}

	// Tile/object => own name; legend entry => the bound entity's name (Player or object).
	private static String nameOf(EObject element) {
		if (element instanceof TileDef) return ((TileDef) element).getName();
		if (element instanceof ObjectDef) return ((ObjectDef) element).getName();
		if (element instanceof LegendEntry) {
			EObject bound = ((LegendEntry) element).getEntity();
			if (bound instanceof PlayerRef) return "Player";
			if (bound instanceof ObjectRef && ((ObjectRef) bound).getRef() != null)
				return ((ObjectRef) bound).getRef().getName();
		}
		return null;
	}

	private static String currentChar(EObject element) {
		if (element instanceof Entity) return ((Entity) element).getChar();
		if (element instanceof LegendEntry) return ((LegendEntry) element).getChar();
		return null;
	}

	private static void setChar(EObject element, String newChar) {
		if (element instanceof Entity) ((Entity) element).setChar(newChar);
		else if (element instanceof LegendEntry) ((LegendEntry) element).setChar(newChar);
	}

	@Fix(GBSokobanValidator.ISSUE_LEVEL_NOT_RECTANGULAR)
	public void fixLevelNotRectangular(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue,
				"Pad shorter rows to the maximum width",
				"Extend each short row on the right by repeating its rightmost character.",
				null,
				(ISemanticModification) (element, context) -> {
					if (!(element instanceof Level)) return;
					Level level = (Level) element;
					Game game = EcoreUtil2.getContainerOfType(level, Game.class);
					Character fallback = (game != null) ? firstTileChar(game) : null;
					EList<String> rows = level.getRows();
					int targetWidth = 0;
					for (String row : rows) targetWidth = Math.max(targetWidth, row.length());
					for (int i = 0; i < rows.size(); i++) {
						String row = rows.get(i);
						if (row.length() < targetWidth) {
							Character padChar = row.isEmpty() ? fallback : row.charAt(row.length() - 1);
							StringBuilder padded = new StringBuilder(row);
							while (padded.length() < targetWidth) padded.append(padChar.charValue());
							rows.set(i, padded.toString());
						}
					}
				});
	}

	@Fix(GBSokobanValidator.ISSUE_LEVEL_TOO_MANY_PLAYERS)
	public void fixLevelTooManyPlayers(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue,
				"Keep only the first player position",
				"Replace every player character after the first with the first passable tile's character.",
				null,
				(ISemanticModification) (element, context) -> {
					if (!(element instanceof Level)) return;
					Level level = (Level) element;
					Game game = EcoreUtil2.getContainerOfType(level, Game.class);
					if (game == null) return;
					List<String> playerChars = playerCharsOf(game);
					if (playerChars.isEmpty()) return;
					Character fillChar = passableFillChar(game);
					// No passable tile to swap in: leave the level alone rather than inserting walls in its interior.
					if (fillChar == null) return;
					EList<String> rows = level.getRows();
					boolean firstPlayerKept = false;
					for (int i = 0; i < rows.size(); i++) {
						StringBuilder rewrittenRow = new StringBuilder(rows.get(i));
						for (int col = 0; col < rewrittenRow.length(); col++) {
							String character = String.valueOf(rewrittenRow.charAt(col));
							if (!playerChars.contains(character)) continue;
							if (!firstPlayerKept) { firstPlayerKept = true; continue; }
							rewrittenRow.setCharAt(col, fillChar.charValue());
						}
						rows.set(i, rewrittenRow.toString());
					}
				});
	}

	private static List<String> playerCharsOf(Game game) {
		List<String> playerChars = new ArrayList<>();
		for (LegendEntry entry : legendEntriesOf(game)) {
			if (entry.getEntity() instanceof PlayerRef && entry.getChar() != null)
				playerChars.add(entry.getChar());
		}
		return playerChars;
	}

	/** Character of the first declared tile, or null when no tile has a usable char. */
	private static Character firstTileChar(Game game) {
		for (TileDef tile : tilesOf(game))
			if (tile.getChar() != null && !tile.getChar().isEmpty())
				return tile.getChar().charAt(0);
		return null;
	}

	/** Character of the first non-solid tile. Falls back to firstTileChar, or null when nothing usable exists. */
	private static Character passableFillChar(Game game) {
		for (TileDef tile : tilesOf(game))
			if (!(tile.getType() instanceof SolidTile)
					&& tile.getChar() != null && !tile.getChar().isEmpty())
				return tile.getChar().charAt(0);
		return firstTileChar(game);
	}

	@Fix(GBSokobanValidator.ISSUE_DUPLICATE_CHAR)
	public void fixDuplicateChar(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue,
				"Replace with a character derived from the name",
				"Use the first letter of the entity's name (or the next free alnum if all are taken).",
				null,
				(ISemanticModification) (element, context) -> {
					Game game = EcoreUtil2.getContainerOfType(element, Game.class);
					if (game != null) setChar(element, pickUnusedChar(game, nameOf(element)));
				});
	}

	@Fix(GBSokobanValidator.ISSUE_CHAR_LENGTH)
	public void fixCharLength(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue,
				"Replace with a single character",
				"If the string is too long, keep its first character. If empty, take the first letter of the entity's name.",
				null,
				(ISemanticModification) (element, context) -> {
					String c = currentChar(element);
					if (c != null && c.length() > 1) {
						setChar(element, c.substring(0, 1));
						return;
					}
					Game game = EcoreUtil2.getContainerOfType(element, Game.class);
					if (game != null) setChar(element, pickUnusedChar(game, nameOf(element)));
				});
	}

	@Fix(GBSokobanValidator.ISSUE_PULL_REQUIRES_CAN_PULL)
	public void fixPullRequiresCanPull(Issue issue, IssueResolutionAcceptor acceptor) {
		acceptor.accept(issue,
				"Add 'canPull' to the player",
				"Set the canPull flag so the pull animation is reachable.",
				null,
				(ISemanticModification) (element, context) -> {
					if (element instanceof PlayerDef) ((PlayerDef) element).setCanPull(true);
				});
	}
}
