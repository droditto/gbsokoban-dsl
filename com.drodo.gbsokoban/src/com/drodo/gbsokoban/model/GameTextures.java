package com.drodo.gbsokoban.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.drodo.gbsokoban.gBSokoban.Animation;
import com.drodo.gbsokoban.gBSokoban.Game;
import com.drodo.gbsokoban.gBSokoban.ObjectDef;
import com.drodo.gbsokoban.gBSokoban.Texture;
import com.drodo.gbsokoban.gBSokoban.TileDef;
import com.drodo.gbsokoban.util.CellGeometry;

public final class GameTextures {

	private GameTextures() {
	}

	public static int cellPx(Game game) {
		if (game == null)
			return 0;
		for (Texture texture : game.getTextures()) {
			int rows = texture.getRows().size();
			if (rows != 0)
				return CellGeometry.SUPPORTED_PX.contains(rows) ? rows : 0;
		}
		return CellGeometry.DEFAULT_PX;
	}

	public static List<Texture> background(Game game) {
		Set<Texture> out = new LinkedHashSet<>();
		for (TileDef tile : game.getTiles())
			add(out, tile.getTexture());
		for (ObjectDef object : game.getObjects()) {
			add(out, object.getTexture());
			add(out, object.getGoalTexture());
		}
		return new ArrayList<>(out);
	}

	public static List<Texture> sprites(Game game) {
		Set<Texture> out = new LinkedHashSet<>();
		if (game.getPlayer() != null)
			for (Animation anim : game.getPlayer().getAnims())
				if (anim.getFrames() != null)
					for (Texture frame : anim.getFrames().getTextures())
						add(out, frame);
		for (ObjectDef object : game.getObjects())
			add(out, object.getTexture());
		return new ArrayList<>(out);
	}

	private static void add(Set<Texture> out, Texture texture) {
		if (texture != null)
			out.add(texture);
	}
}
