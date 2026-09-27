// The editor's highlighting (web/src/gbsoko.js) for ```gbsoko blocks.
hljs.registerLanguage('gbsoko', () => ({
	contains: [
		hljs.C_LINE_COMMENT_MODE,
		hljs.C_BLOCK_COMMENT_MODE,
		{ className: 'meta', begin: /^==+\s*$/ },
		{ className: 'section', begin: /^(PALETTES|TEXTURES|TILES|OBJECTS|PLAYER|LEGEND|WIN|SOUNDS|LEVELS|LEVEL)\s*$/ },
		{ className: 'string', begin: /"/, end: /"|$/ },
		{ className: 'number', begin: /#[0-9a-fA-F]{6}|\b\d+\b/ },
		{ className: 'operator', begin: /=/ },
		// ON_GOAL is art in OBJECTS but an event in SOUNDS.
		{ className: 'literal', begin: /\bON_GOAL(?=\s+NR)/ },
		{ className: 'keyword', begin: /\b(TITLE|AUTHOR|MOVE_SPEED|ANIM_SPEED|ENDING|PLAYER_CAN_PULL|NO_LEVEL_SELECT|USES|ON|ON_GOAL|BLOCKS|SLIDES|DESTROYS|COLLAPSES|INTO|FILLS|CARRIES|MIRROR|ALL|SOME|NO)\b/ },
		{ className: 'literal', begin: /\b(PLAYER|WALK|PUSH|PULL|DOWN|UP|LEFT|RIGHT|NR[1-4]|MOVE|BLOCKED|DESTROY|COMPLETE|RESTART|COLLAPSE|MENU_MOVE|MENU_OK|DMG_\w+|RGB_\w+)\b/ },
		{ className: 'variable', begin: /\b[A-Za-z_]\w*\b/ }
	]
}));

// book.js has already highlighted every block, before this language existed.
document.querySelectorAll('code.language-gbsoko').forEach((block) => {
	block.innerHTML = hljs.highlight('gbsoko', block.textContent).value;
});
