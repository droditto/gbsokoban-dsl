import { EditorView, basicSetup } from 'codemirror';
import { tooltips } from '@codemirror/view';
import { EditorState } from '@codemirror/state';
import { oneDark } from '@codemirror/theme-one-dark';
import { languageServer } from '@marimo-team/codemirror-languageserver';

import { gbsoko } from './gbsoko.js';
import { previews } from './preview.js';
import { play } from './emulator.js';
import { makeResizable } from './splitter.js';
import { readFromUrl, linkTo } from './share.js';
import './style.css';

const status = document.querySelector('#status');
const log = document.querySelector('#log');
const screen = document.querySelector('#screen');
const playButton = document.querySelector('#play');
const downloadButton = document.querySelector('#download');
const shareButton = document.querySelector('#share');
const hint = document.querySelector('.hint');
const pullHint = document.querySelector('#pull');

let rom = null;
let romName;

function say(text, failed = false) {
	status.textContent = text;
	status.classList.toggle('warning', failed);
}

async function start() {
	makeResizable(document.querySelector('#splitter'));

	say('Abriendo la sesión');
	const session = await fetch('/api/session').then((response) => response.json());

	const scheme = location.protocol === 'https:' ? 'wss' : 'ws';

	const server = languageServer({
		serverUri: `${scheme}://${location.host}/lsp?session=${session.session}`,
		rootUri: session.rootUri,
		documentUri: session.documentUri,
		languageId: 'gbsoko',
		workspaceFolders: [{ uri: session.rootUri, name: 'gbsokoban' }]
	});

	const view = new EditorView({
		state: EditorState.create({
			doc: (await readFromUrl()) ?? session.source,
			extensions: [
				basicSetup,
				oneDark,
				gbsoko,
				previews,
				server,
				// On document.body, so tooltips on the first line are not clipped.
				tooltips({ position: 'fixed', parent: document.body })
			]
		}),
		parent: document.querySelector('#editor')
	});

	say('Pulsa Compilar para probar tu juego');

	playButton.addEventListener('click', async () => {
		playButton.disabled = true;
		downloadButton.disabled = true;
		say('Compilando');
		log.textContent = '';
		try {
			const source = view.state.doc.toString();
			const response = await fetch('/api/build', {
				method: 'POST',
				headers: { 'content-type': 'application/json' },
				body: JSON.stringify({ session: session.session, source })
			});
			const body = await response.json();
			if (!response.ok) {
				say(body.error ?? 'No se ha podido compilar', true);
				log.textContent = body.issues ?? body.log ?? '';
				return;
			}
			rom = Uint8Array.from(atob(body.bytes), (c) => c.charCodeAt(0));
			romName = body.name;
			say('');
			hint.hidden = false;
			pullHint.hidden = !/^\s*PLAYER_CAN_PULL\b/m.test(source);
			downloadButton.disabled = false;
			await play(screen, rom);
		} catch (error) {
			say(String(error?.message ?? error), true);
		} finally {
			playButton.disabled = false;
		}
	});

	shareButton.addEventListener('click', async () => {
		const link = linkTo(view.state.doc.toString());
		try {
			// Safari only lets a page write during the click, so the clipboard gets the promise at once.
			const text = link.then((url) => new Blob([url], { type: 'text/plain' }));
			await navigator.clipboard.write([new ClipboardItem({ 'text/plain': text })]);
			say('Enlace copiado al portapapeles');
		} catch {
			// Without HTTPS there is no clipboard, so the link goes in the address bar.
			history.replaceState(null, '', await link);
			say('El enlace está en la barra de direcciones');
		}
	});

	downloadButton.addEventListener('click', () => {
		const anchor = document.createElement('a');
		anchor.href = URL.createObjectURL(new Blob([rom], { type: 'application/octet-stream' }));
		anchor.download = romName;
		anchor.click();
		URL.revokeObjectURL(anchor.href);
	});
}

start().catch((error) => say(String(error?.message ?? error), true));
