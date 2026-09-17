package com.drodo.gbsokoban.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * How free text reaches the screen. The minimal font has a space, ten digits and
 * twenty-six capitals; a row is twenty tiles wide.
 */
public final class ScreenText {

	public static final int COLUMNS = 20;

	public static final int ROWS = 18;

	public static final int PIXEL_WIDTH = COLUMNS * 8;

	public static final int PIXEL_HEIGHT = ROWS * 8;

	/** The window is anchored bottom right, so the HUD can only be a bar along the bottom. */
	private static final int HUD_ROWS = 1;

	public static final int PANEL_ROWS = 2;

	public static final int PLAY_PIXEL_HEIGHT = PIXEL_HEIGHT - HUD_ROWS * 8;

	private ScreenText() {
	}

	public static String printable(String text) {
		if (text == null)
			return "";
		return tidy(text.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 \n]", " "));
	}

	public static boolean losesCharacters(String text) {
		return text != null && !printable(text).equals(tidy(text.toUpperCase(Locale.ROOT)));
	}

	private static String tidy(String text) {
		return text.replaceAll("[ \t]+", " ").replaceAll(" *\n *", "\n")
				.replaceAll("^\n+|\n+$", "").trim();
	}

	public static String fitBlock(String text, int maxRows) {
		List<String> rows = wrap(text);
		return String.join("\n", rows.subList(0, Math.min(rows.size(), maxRows)));
	}

	public static List<String> wrap(String text) {
		List<String> rows = new ArrayList<>();
		for (String paragraph : printable(text).split("\n", -1)) {
			if (paragraph.isEmpty())
				rows.add("");
			else
				wrapInto(rows, paragraph);
		}
		return rows;
	}

	private static void wrapInto(List<String> rows, String text) {
		StringBuilder row = new StringBuilder();
		for (String word : text.split(" ")) {
			if (word.isEmpty())
				continue;
			if (row.length() == 0)
				row.append(word);
			else if (row.length() + 1 + word.length() <= COLUMNS)
				row.append(' ').append(word);
			else {
				rows.add(row.toString());
				row = new StringBuilder(word);
			}
		}
		if (row.length() > 0)
			rows.add(row.toString());
	}
}
