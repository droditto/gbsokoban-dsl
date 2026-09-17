package com.drodo.gbsokoban.validation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.PaletteDef;
import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.model.GameTextures;
import com.drodo.gbsokoban.util.Palettes;

public class PaletteValidator extends AbstractDeclarativeValidator {

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	/** A palette register holds four entries and art draws with all four. */
	@Check
	public void checkPaletteFitsRegister(PaletteDef palette) {
		if (palette.getColors().isEmpty() && palette.getShades().isEmpty())
			return;
		int named = palette.getColors().isEmpty()
				? palette.getShades().size()
				: palette.getColors().size();
		if (named != Palettes.ENTRIES)
			error("A palette needs exactly " + Palettes.ENTRIES + " colors. This one has " + named,
					GBSokobanPackage.Literals.PALETTE_DEF__NAME, GBSokobanValidator.ISSUE_PALETTE_SIZE);
	}

	@Check
	public void checkPaletteFormIsConsistent(Game game) {
		if (!anyWrittenInShades(game.getPalettes()))
			return;
		for (PaletteDef palette : game.getPalettes())
			if (!palette.getColors().isEmpty())
				warning("This game mixes shades and colors. Pick one or the other",
						palette, GBSokobanPackage.Literals.PALETTE_DEF__NAME, -1);
	}

	private static boolean anyWrittenInShades(List<PaletteDef> palettes) {
		for (PaletteDef palette : palettes)
			if (!palette.getShades().isEmpty())
				return true;
		return false;
	}

	private static Set<PaletteDef> palettesOf(List<Texture> textures) {
		Set<PaletteDef> used = new LinkedHashSet<>();
		for (Texture texture : textures)
			if (texture.getPalette() != null)
				used.add(texture.getPalette());
		return used;
	}

	private static boolean anyWrittenInColor(List<PaletteDef> palettes) {
		for (PaletteDef palette : palettes)
			if (!palette.getColors().isEmpty())
				return true;
		return false;
	}

	@Check
	public void checkPaletteBudget(Game game) {
		List<PaletteDef> background = new ArrayList<>(palettesOf(GameTextures.background(game)));
		List<PaletteDef> sprites = new ArrayList<>(palettesOf(GameTextures.sprites(game)));
		boolean color = anyWrittenInColor(background) || anyWrittenInColor(sprites);

		int allowed = color ? Palettes.CGB_BKG_PALETTES : Palettes.DMG_BKG_PALETTES;
		for (int i = allowed; i < background.size(); i++)
			error(color
					? "This game needs " + background.size() + " palettes for its tiles and objects. A "
							+ "Game Boy Color allows " + Palettes.CGB_BKG_PALETTES
					: "An original Game Boy allows " + Palettes.DMG_BKG_PALETTES + " palette for tiles "
							+ "and objects and this game needs " + background.size()
							+ ". Use '" + background.get(0).getName()
							+ "' everywhere or write the game in color",
					background.get(i), GBSokobanPackage.Literals.PALETTE_DEF__NAME, INSIGNIFICANT_INDEX);

		int allowedSprites = color ? Palettes.CGB_OBJECT_PALETTES : Palettes.DMG_OBJECT_PALETTES;
		for (int i = allowedSprites; i < sprites.size(); i++)
			error(color
					? "This game needs " + sprites.size() + " palettes for the player and the moving "
							+ "objects. A Game Boy Color allows " + Palettes.CGB_OBJECT_PALETTES
					: "An original Game Boy allows " + Palettes.DMG_OBJECT_PALETTES + " palettes for the "
							+ "player and the moving objects and this game needs " + sprites.size()
							+ ". Use '" + sprites.get(0).getName() + "' or '" + sprites.get(1).getName()
							+ "' or write the game in color",
					sprites.get(i), GBSokobanPackage.Literals.PALETTE_DEF__NAME, INSIGNIFICANT_INDEX);
	}
}
