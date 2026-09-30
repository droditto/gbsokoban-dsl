import { Decoration, ViewPlugin, WidgetType } from '@codemirror/view';
import { RangeSetBuilder } from '@codemirror/state';

import { SECTIONS } from './gbsoko.js';

const SHADES = {
	DMG_WHITE: '#ffffff',
	DMG_LITE_GRAY: '#a9a9a9',
	DMG_DARK_GRAY: '#545454',
	DMG_BLACK: '#000000'
};

// gbdk's gb/cgb.h values, 5 bits per channel.
const COLOURS = {
	RGB_RED: [31, 0, 0], RGB_DARKRED: [15, 0, 0],
	RGB_GREEN: [0, 31, 0], RGB_DARKGREEN: [0, 15, 0],
	RGB_BLUE: [0, 0, 31], RGB_DARKBLUE: [0, 0, 15],
	RGB_YELLOW: [31, 31, 0], RGB_DARKYELLOW: [21, 21, 0],
	RGB_CYAN: [0, 31, 31], RGB_AQUA: [28, 5, 22],
	RGB_PINK: [31, 0, 31], RGB_PURPLE: [21, 0, 21],
	RGB_BLACK: [0, 0, 0], RGB_DARKGRAY: [10, 10, 10],
	RGB_LIGHTGRAY: [21, 21, 21], RGB_WHITE: [31, 31, 31],
	RGB_LIGHTFLESH: [30, 20, 15], RGB_BROWN: [10, 10, 0],
	RGB_ORANGE: [30, 20, 0], RGB_TEAL: [15, 15, 0]
};

// Not black, which is a real palette colour.
const HOLE = '#f3b2b1';

function colourOf(word) {
	if (SHADES[word]) return SHADES[word];
	if (COLOURS[word]) {
		const channels = COLOURS[word].map((value) => Math.round((value * 255) / 31));
		return `#${channels.map((value) => value.toString(16).padStart(2, '0')).join('')}`;
	}
	return /^#[0-9a-fA-F]{6}$/.test(word) ? word.toLowerCase() : null;
}

class Swatch extends WidgetType {
	constructor(colour) {
		super();
		this.colour = colour;
	}

	eq(other) {
		return other.colour === this.colour;
	}

	toDOM() {
		const box = document.createElement('span');
		box.className = 'cm-swatch';
		box.style.background = this.colour;
		return box;
	}
}

/** Spans, not a canvas: Safari draws a dark edge on a scaled 1px canvas. */
class Row extends WidgetType {
	constructor(pixels, colours) {
		super();
		this.pixels = pixels;
		this.colours = colours;
	}

	eq(other) {
		return other.pixels === this.pixels && other.colours.join() === this.colours.join();
	}

	toDOM() {
		const strip = document.createElement('span');
		strip.className = 'cm-row';
		for (const pixel of this.pixels) {
			const dot = document.createElement('span');
			dot.style.background = pixel === '.' ? HOLE : this.colours[Number(pixel)] ?? 'transparent';
			strip.append(dot);
		}
		return strip;
	}
}

function withoutComments(text) {
	return text.replace(/"(?:\\.|[^"\\\n])*"|\/\/[^\n]*|\/\*[\s\S]*?(?:\*\/|$)/g,
		(match) => (match.startsWith('"') ? match : match.replace(/[^\n]/g, ' ')));
}

function scan(doc) {
	const palettes = new Map();
	const widgets = [];
	const lines = withoutComments(doc.toString()).split('\n');
	let section = null;
	let colours = null;

	for (let number = 1; number <= doc.lines; number++) {
		const line = doc.line(number);
		const text = lines[number - 1];
		const trimmed = text.trim();

		if (SECTIONS.has(trimmed)) {
			section = trimmed;
			colours = null;
			continue;
		}
		if (!trimmed || /^=+$/.test(trimmed)) continue;

		if (section === 'PALETTES') {
			const words = [...text.matchAll(/\S+/g)];
			const named = [];
			for (const word of words.slice(1)) {
				const colour = colourOf(word[0]);
				if (!colour) continue;
				named.push(colour);
				widgets.push({ at: line.from + word.index, widget: new Swatch(colour) });
			}
			if (named.length) palettes.set(words[0][0], named);
			continue;
		}

		if (section !== 'TEXTURES') continue;

		const header = /^\s*\w+\s+USES\s+(\w+)/.exec(text);
		if (header) {
			colours = palettes.get(header[1]) ?? null;
			continue;
		}

		const row = /"([^"]*)"/.exec(text);
		if (row?.[1] && colours) widgets.push({ at: line.to, widget: new Row(row[1], colours) });
	}

	return widgets;
}

function decorate(view) {
	const builder = new RangeSetBuilder();
	for (const found of scan(view.state.doc)) {
		builder.add(found.at, found.at, Decoration.widget({ widget: found.widget, side: 1 }));
	}
	return builder.finish();
}

export const previews = ViewPlugin.fromClass(
	class {
		constructor(view) {
			this.measure(view);
			this.decorations = decorate(view);
		}

		update(update) {
			if (update.geometryChanged) this.measure(update.view);
			if (update.docChanged) this.decorations = decorate(update.view);
		}

		/** A guessed row height leaves hairlines between rows. */
		measure(view) {
			view.dom.style.setProperty('--row', `${view.defaultLineHeight}px`);
		}
	},
	{ decorations: (plugin) => plugin.decorations }
);
