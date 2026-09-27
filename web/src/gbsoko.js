import { StreamLanguage } from '@codemirror/language';

export const SECTIONS = new Set(['PALETTES', 'TEXTURES', 'TILES', 'OBJECTS', 'PLAYER', 'LEGEND',
	'WIN', 'SOUNDS', 'LEVELS', 'LEVEL']);

const SETTINGS = new Set(['TITLE', 'AUTHOR', 'MOVE_SPEED', 'ANIM_SPEED', 'ENDING', 'PLAYER_CAN_PULL',
	'NO_LEVEL_SELECT']);

const KEYWORDS = new Set(['USES', 'ON', 'ON_GOAL', 'BLOCKS', 'SLIDES', 'DESTROYS', 'COLLAPSES', 'INTO',
	'FILLS', 'CARRIES', 'MIRROR', 'ALL', 'SOME', 'NO']);

const VALUES = new Set(['PLAYER', 'WALK', 'PUSH', 'PULL', 'DOWN', 'UP', 'LEFT', 'RIGHT', 'NR1', 'NR2', 'NR3', 'NR4',
	'MOVE', 'BLOCKED', 'DESTROY', 'COMPLETE', 'RESTART', 'COLLAPSE', 'MENU_MOVE', 'MENU_OK']);

// ON_GOAL is art in OBJECTS but an event in SOUNDS.
export const gbsoko = StreamLanguage.define({
	name: 'gbsoko',
	startState: () => ({ section: null, inComment: false }),
	copyState: (state) => ({ section: state.section, inComment: state.inComment }),
	token(stream, state) {
		if (state.inComment || stream.match('/*')) {
			state.inComment = !stream.skipTo('*/');
			if (state.inComment) stream.skipToEnd();
			else stream.match('*/');
			return 'comment';
		}
		if (stream.eatSpace()) return null;

		if (stream.match('//')) {
			stream.skipToEnd();
			return 'comment';
		}
		if (stream.match(/^==+\s*$/)) return 'meta';

		if (stream.match(/^"[^"]*"?/)) return 'string';
		if (stream.match(/^#[0-9a-fA-F]{6}/)) return 'number';
		if (stream.match(/^\d+/)) return 'number';
		if (stream.match(/^=/)) return 'operator';

		const word = stream.match(/^[A-Za-z_][A-Za-z0-9_]*/);
		if (word) {
			const text = word[0];
			if (SECTIONS.has(text) && stream.match(/^\s*$/, false)) {
				state.section = text;
				return 'heading';
			}
			if (text === 'ON_GOAL') return state.section === 'SOUNDS' ? 'atom' : 'keyword';
			if (SETTINGS.has(text)) return 'definitionKeyword';
			if (KEYWORDS.has(text)) return 'keyword';
			if (VALUES.has(text) || text.startsWith('DMG_') || text.startsWith('RGB_')) return 'atom';
			return 'variableName';
		}

		stream.next();
		return null;
	},
	languageData: {
		commentTokens: { line: '//', block: { open: '/*', close: '*/' } }
	}
});
