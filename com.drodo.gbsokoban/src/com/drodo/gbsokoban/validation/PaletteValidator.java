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
import com.drodo.gbsokoban.util.Messages;
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
			error(Messages.get("palette.size", Palettes.ENTRIES),
					GBSokobanPackage.Literals.PALETTE_DEF__NAME, GBSokobanValidator.ISSUE_PALETTE_SIZE);
	}

	@Check
	public void checkPaletteFormIsConsistent(Game game) {
		if (!anyWrittenInShades(game.getPalettes()))
			return;
		for (PaletteDef palette : game.getPalettes())
			if (!palette.getColors().isEmpty())
				warning(Messages.get("palette.mixed"),
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
					? Messages.get("palette.tooMany", Palettes.CGB_BKG_PALETTES)
					: Messages.get("palette.dmgTooMany"),
					background.get(i), GBSokobanPackage.Literals.PALETTE_DEF__NAME, INSIGNIFICANT_INDEX);

		int allowedSprites = color ? Palettes.CGB_OBJECT_PALETTES : Palettes.DMG_OBJECT_PALETTES;
		for (int i = allowedSprites; i < sprites.size(); i++)
			error(color
					? Messages.get("palette.tooMany", Palettes.CGB_OBJECT_PALETTES)
					: Messages.get("palette.dmgTooMany"),
					sprites.get(i), GBSokobanPackage.Literals.PALETTE_DEF__NAME, INSIGNIFICANT_INDEX);
	}
}
