package com.drodo.gbsokoban.generator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The hand-written C engine, read as classloader resources so it works outside an
 * Eclipse runtime too. Listed explicitly because a classloader cannot enumerate a package.
 */
public final class EngineSources {

	private static final String BASE = "/com/drodo/gbsokoban/engine/";

	public static final List<String> FILES = List.of(
			"box.c", "box.h",
			"camera.c", "camera.h",
			"game.c", "game.h",
			"landing.c", "landing.h",
			"level.c", "level.h",
			"main.c",
			"menus.c", "menus.h",
			"player.c", "player.h",
			"render.c", "render.h",
			"save.c", "save.h",
			"sound.c", "sound.h",
			"turn.c", "turn.h",
			"ui.c", "ui.h",
			"win.c", "win.h");

	private EngineSources() {
	}

	public static String read(String name) {
		try (InputStream in = EngineSources.class.getResourceAsStream(BASE + name)) {
			if (in == null)
				throw new IllegalStateException("Engine resource not on the classpath: " + BASE + name);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Could not read engine resource " + name, e);
		}
	}
}
