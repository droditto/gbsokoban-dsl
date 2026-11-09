package com.drodo.gbsokoban.validation;

import java.util.List;

import org.eclipse.xtext.validation.AbstractDeclarativeValidator;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.EValidatorRegistrar;

import com.drodo.gbsokoban.gBSokoban.GBSokobanPackage;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.generator.analysis.TilesetAllocator;
import com.drodo.gbsokoban.model.GameTextures;
import com.drodo.gbsokoban.util.CellGeometry;
import com.drodo.gbsokoban.util.Messages;
import com.drodo.gbsokoban.util.Palettes;
import com.drodo.gbsokoban.util.VramLayout;

public class TextureValidator extends AbstractDeclarativeValidator {

	@Override
	public void register(EValidatorRegistrar registrar) {
		// not needed for classes used as ComposedCheck
	}

	@Check
	public void checkTextureIsSquareAndSupported(Texture texture) {
		int rows = texture.getRows().size();
		if (rows == 0)
			return;
		if (!CellGeometry.SUPPORTED_PX.contains(rows)) {
			error(Messages.get("texture.rows", CellGeometry.SUPPORTED_PX.get(0), CellGeometry.SUPPORTED_PX.get(1)),
					GBSokobanPackage.Literals.TEXTURE__NAME, GBSokobanValidator.ISSUE_TEXTURE_SIZE);
			return;
		}
		for (int i = 0; i < rows; i++) {
			String row = texture.getRows().get(i);
			if (row.length() != rows) {
				error(Messages.get("texture.notSquare", rows), texture,
						GBSokobanPackage.Literals.TEXTURE__ROWS, i, GBSokobanValidator.ISSUE_TEXTURE_SIZE);
				return;
			}
			for (int x = 0; x < row.length(); x++) {
				char c = row.charAt(x);
				if (c != '.' && (c < '0' || c - '0' >= Palettes.ENTRIES)) {
					error(Messages.get("texture.badColor", Palettes.ENTRIES - 1),
							texture, GBSokobanPackage.Literals.TEXTURE__ROWS, i);
					return;
				}
			}
		}
	}

	@Check
	public void checkAllTexturesSameSize(Game game) {
		int expected = GameTextures.cellPx(game);
		if (expected == 0)
			return;
		for (Texture texture : game.getTextures()) {
			int size = texture.getRows().size();
			if (size != expected && (size == 8 || size == 16))
				error(Messages.get("texture.sameSize", expected), texture,
						GBSokobanPackage.Literals.TEXTURE__NAME, -1, GBSokobanValidator.ISSUE_TEXTURE_SIZE);
		}
	}

	@Check
	public void checkVramBudget(Game game) {
		int cellPx = GameTextures.cellPx(game);
		if (cellPx == 0)
			return;

		List<Texture> bkg = GameTextures.background(game);
		int bkgTiles = TilesetAllocator.allocateBackground(bkg, cellPx).tiles().size();
		if (bkgTiles > VramLayout.BKG_TILE_BUDGET)
			reportWhereRoomRunsOut(bkg, cellPx, VramLayout.BKG_TILE_BUDGET, false,
					Messages.get("texture.bkgMemory"));

		List<Texture> sprites = GameTextures.sprites(game);
		int spriteTiles = TilesetAllocator.allocateSprites(sprites, cellPx).tiles().size();
		if (spriteTiles > VramLayout.SPRITE_TILES)
			reportWhereRoomRunsOut(sprites, cellPx, VramLayout.SPRITE_TILES, true,
					Messages.get("texture.spriteMemory"));
	}

	private void reportWhereRoomRunsOut(List<Texture> textures, int cellPx, int budget,
			boolean sprites, String message) {
		for (int i = 1; i <= textures.size(); i++) {
			List<Texture> upTo = textures.subList(0, i);
			int used = sprites
					? TilesetAllocator.allocateSprites(upTo, cellPx).tiles().size()
					: TilesetAllocator.allocateBackground(upTo, cellPx).tiles().size();
			if (used > budget) {
				error(message, textures.get(i - 1), GBSokobanPackage.Literals.TEXTURE__NAME,
						INSIGNIFICANT_INDEX);
				return;
			}
		}
	}
}
