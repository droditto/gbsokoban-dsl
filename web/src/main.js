import { EditorView, basicSetup } from 'codemirror';
import { tooltips } from '@codemirror/view';
import { EditorState } from '@codemirror/state';
import { oneDark } from '@codemirror/theme-one-dark';
import { languageServer } from '@marimo-team/codemirror-languageserver';

import { gbsoko } from './gbsoko.js';
import { previews } from './preview.js';
import { play, stop } from './emulator.js';
import { makeResizable } from './splitter.js';
import { readFromUrl, linkTo } from './share.js';
import './style.css';

const consent = document.querySelector('#consent');
const welcome = document.querySelector('#welcome');
const study = document.querySelectorAll('.study');
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
let joining = null;

const opened = fetch('/api/session').then((response) => response.json());
const post = (path, body) =>
	fetch(path, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) });

function say(text, failed = false) {
	status.textContent = text;
	status.classList.toggle('warning', failed);
}

function participate(number) {
	joining = opened.then(async (session) => {
		const response = await post('/api/participate', { session: session.session, number });
		const body = await response.json();
		if (!response.ok) throw new Error(body.error);
		sessionStorage.setItem('participant', body.number);
		for (const label of document.querySelectorAll('.participant')) label.textContent = body.number;
		for (const element of study) element.hidden = false;
		if (!number) welcome.showModal();
	});
	joining.catch((error) => say(String(error?.message ?? error), true));
}

function track(type, detail) {
	joining?.then(() => opened).then((session) => post('/api/event', { session: session.session, type, detail }));
}

async function start() {
	makeResizable(document.querySelector('#splitter'));

	const session = await opened;

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
			doc: sessionStorage.getItem('source') ?? (await readFromUrl()) ?? session.source,
			extensions: [
				basicSetup,
				EditorView.theme({ '.cm-tooltip-autocomplete > ul > li[aria-selected]': { backgroundColor: '#347', color: 'white' } }),
				oneDark,
				gbsoko,
				previews,
				server,
				// On document.body, so tooltips on the first line are not clipped.
				tooltips({ position: 'fixed', parent: document.body }),
				EditorView.updateListener.of((update) => {
					if (update.docChanged) sessionStorage.setItem('source', update.state.doc.toString());
					for (const transaction of update.transactions)
						if (transaction.isUserEvent('input.complete'))
							transaction.changes.iterChanges((fromA, toA, fromB, toB, text) => track('completion', text.line(1).text));
				})
			]
		}),
		parent: document.querySelector('#editor')
	});

	say('Pulsa Compilar para probar tu juego');

	view.dom.addEventListener('keydown', (event) => {
		if (event.ctrlKey && event.code === 'Space') track('assist');
	});
	document.addEventListener('mousedown', (event) => {
		const fix = event.target.closest('.cm-diagnosticAction');
		if (fix) track('fix', fix.textContent);
	}, true);

	playButton.addEventListener('click', async () => {
		playButton.disabled = true;
		downloadButton.disabled = true;
		hint.hidden = true;
		stop(screen);
		say('Compilando');
		log.textContent = '';
		try {
			const source = view.state.doc.toString();
			const response = await post('/api/build', { session: session.session, source });
			const body = await response.json();
			if (!response.ok) {
				say(body.error ?? 'No se ha podido compilar', true);
				log.textContent = body.log ?? '';
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
			say('Copia el enlace de la barra de direcciones');
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

consent.addEventListener('close', () => {
	if (consent.returnValue !== 'yes' && consent.returnValue !== 'no') return consent.showModal();
	sessionStorage.setItem('consent', consent.returnValue);
	if (consent.returnValue === 'yes') participate();
});
if (!sessionStorage.getItem('consent')) consent.showModal();
else if (sessionStorage.getItem('consent') === 'yes') participate(Number(sessionStorage.getItem('participant')));

start().catch((error) => say(String(error?.message ?? error), true));
