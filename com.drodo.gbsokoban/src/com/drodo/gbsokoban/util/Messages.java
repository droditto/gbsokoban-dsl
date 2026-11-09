package com.drodo.gbsokoban.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public final class Messages {

	public static final Locale LOCALE = Locale.forLanguageTag("es");
	// public static final Locale LOCALE = Locale.ENGLISH;

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("com.drodo.gbsokoban.util.messages",
			LOCALE, Messages.class.getClassLoader(),
			ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));

	private Messages() {
	}

	public static boolean has(String key) {
		return BUNDLE.containsKey(key);
	}

	public static String get(String key, Object... args) {
		return new MessageFormat(BUNDLE.getString(key), LOCALE).format(args);
	}
}
