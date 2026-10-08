import { createServer } from 'node:http';
import { spawn } from 'node:child_process';
import { readFile, writeFile, appendFile, mkdir, rm, readdir } from 'node:fs/promises';
import { existsSync, createReadStream, statSync, readFileSync, writeFileSync } from 'node:fs';
import { randomUUID } from 'node:crypto';
import { tmpdir } from 'node:os';
import { join, dirname, extname } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { WebSocketServer } from 'ws';

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO = join(HERE, '..');
const LIB = join(REPO, 'build', 'lib');
const PORT = Number(process.env.PORT ?? 3000);
const SERVE_DIST = process.argv.includes('--serve-dist');

const SOURCE_NAME = 'game.gbsoko';
const BUILD_TIMEOUT_MS = 60_000;
const MAX_SOURCE_BYTES = 256 * 1024;
const SESSION_TTL_MS = 2 * 60 * 60 * 1000;
const DATA = join(HERE, 'data');
const EVENTS = new Set(['assist', 'completion', 'fix']);

if (!existsSync(LIB)) {
	console.error('Falta build/lib. Ejecuta primero ./setup.sh');
	process.exit(1);
}

// Java expands the * to every jar in build/lib.
const classpath = join(LIB, '*');

const sessions = new Map();
const ROOT = join(tmpdir(), 'gbsokoban-web');
await mkdir(DATA, { recursive: true });

async function openSession() {
	const id = randomUUID();
	const dir = join(ROOT, id);
	await mkdir(dir, { recursive: true });
	const starter = await readFile(join(HERE, 'templates', 'starter.gbsoko'), 'utf8');
	await writeFile(join(dir, SOURCE_NAME), starter);
	const session = { id, dir, file: join(dir, SOURCE_NAME), building: false, seen: Date.now() };
	sessions.set(id, session);
	return { session, starter };
}

const CONTENT_TYPES = {
	'.html': 'text/html; charset=utf-8',
	'.js': 'text/javascript; charset=utf-8',
	'.css': 'text/css; charset=utf-8',
	'.wasm': 'application/wasm',
	'.json': 'application/json',
	'.png': 'image/png',
	'.gif': 'image/gif',
	'.svg': 'image/svg+xml',
	'.woff2': 'font/woff2'
};

const server = createServer(async (request, response) => {
	try {
		const url = new URL(request.url, 'http://localhost');

		if (url.pathname === '/api/session' && request.method === 'GET') {
			const { session, starter } = await openSession();
			return json(response, 200, {
				session: session.id,
				rootUri: pathToFileURL(session.dir).href,
				documentUri: pathToFileURL(session.file).href,
				source: starter
			});
		}

		if (url.pathname === '/api/build' && request.method === 'POST') {
			return await build(request, response);
		}

		if (url.pathname === '/api/participate' && request.method === 'POST') {
			const body = await readBody(request);
			const session = sessions.get(body.session);
			if (!session) return json(response, 410, { error: 'La sesión ha caducado. Recarga la página.' });
			const resumed = Number.isInteger(body.number) && body.number > 0 && body.number <= lastParticipant();
			session.participant = resumed ? body.number : nextParticipant();
			record(session, { type: resumed ? 'resume' : 'start' });
			// The documentation is plain pages: the cookie tells whose visit each one is.
			response.setHeader('set-cookie', `participant=${session.participant}; Path=/; HttpOnly; SameSite=Strict`);
			return json(response, 200, { number: session.participant });
		}

		if (url.pathname === '/api/event' && request.method === 'POST') {
			const body = await readBody(request);
			const session = sessions.get(body.session);
			if (session && EVENTS.has(body.type))
				record(session, { type: body.type, detail: String(body.detail ?? '').slice(0, 120) });
			return response.writeHead(204).end();
		}

		if (SERVE_DIST) {
			const participant = Number(/(?:^|;\s*)participant=(\d+)/.exec(request.headers.cookie ?? '')?.[1]);
			if (participant && /^\/docs\/(\w+\.html)?$/.test(url.pathname))
				record({ participant }, { type: 'docs', detail: url.pathname.slice(6) || 'index.html' });
			return serveStatic(url.pathname, request, response);
		}

		response.writeHead(404).end();
	} catch (error) {
		console.error(error);
		json(response, 500, { error: String(error?.message ?? error) });
	}
});

function lastParticipant() {
	const file = join(DATA, 'participants');
	return existsSync(file) ? Number(readFileSync(file, 'utf8')) : 0;
}

function nextParticipant() {
	const next = lastParticipant() + 1;
	writeFileSync(join(DATA, 'participants'), String(next));
	return next;
}

function record(session, event) {
	if (!session.participant) return;
	const line = JSON.stringify({ time: new Date().toISOString(), participant: session.participant, ...event });
	appendFile(join(DATA, 'events.jsonl'), line + '\n').catch((error) => console.error(error));
}

function json(response, code, body) {
	response.writeHead(code, { 'content-type': 'application/json; charset=utf-8' }).end(JSON.stringify(body));
}

function serveStatic(pathname, request, response, root = join(HERE, 'dist')) {
	let file = join(root, pathname.slice(1));
	if (existsSync(file) && statSync(file).isDirectory()) {
		if (!pathname.endsWith('/')) return response.writeHead(301, { location: pathname + '/' }).end();
		file = join(file, 'index.html');
	}
	if (!file.startsWith(root) || !existsSync(file)) {
		return response.writeHead(404).end();
	}
	const modified = statSync(file).mtime.toUTCString();
	const headers = {
		'content-type': CONTENT_TYPES[extname(file)] ?? 'application/octet-stream',
		'cache-control': /^\/assets\/|-[0-9a-f]{8}\.[\w.]+$/.test(pathname) ? 'public, max-age=31536000, immutable' : 'no-cache',
		'last-modified': modified
	};
	if (request.headers['if-modified-since'] === modified) return response.writeHead(304, headers).end();
	response.writeHead(200, headers);
	createReadStream(file).pipe(response);
}

async function build(request, response) {
	const body = await readBody(request);
	const session = sessions.get(body.session);
	if (!session) return json(response, 410, { error: 'La sesión ha caducado. Recarga la página.' });
	if (typeof body.source !== 'string' || body.source.length > MAX_SOURCE_BYTES) {
		return json(response, 400, { error: 'El código es demasiado largo.' });
	}
	if (session.building) return json(response, 429, { error: 'Ya hay una compilación en marcha.' });

	session.building = true;
	session.seen = Date.now();
	const recordBuild = (result) => record(session, { type: 'build', result, source: body.source });
	const out = join(session.dir, 'build');
	try {
		await rm(out, { recursive: true, force: true });
		await writeFile(session.file, body.source);

		const generated = await run('java', ['-cp', classpath, 'com.drodo.gbsokoban.generator.GenerateCommand',
			session.file, out], session.dir);
		// ERROR lines mean the program is wrong; otherwise the generator failed.
		if (generated.code !== 0 && /^ERROR\|/m.test(generated.stdout)) {
			recordBuild('errors');
			return json(response, 422, { error: 'Tu juego tiene errores y no se puede compilar.' });
		}
		if (generated.code !== 0) {
			recordBuild('failed');
			return json(response, 500, { error: 'Error interno al generar el proyecto.', log: tail(generated.stderr) });
		}

		const project = (await readdir(out)).find((name) => existsSync(join(out, name, 'Makefile')));
		const dir = join(out, project);
		const made = await run('make', [], dir);
		// The Makefile's own size check: the program is at fault, not the server.
		if (made.code !== 0 && /can be reached/.test(made.stdout)) {
			recordBuild('too-big');
			const error = 'Tu juego es demasiado grande. Quita niveles, hazlos más pequeños o usa menos texturas.';
			return json(response, 422, { error, log: tail(made.stdout) });
		}
		if (made.code !== 0) {
			recordBuild('failed');
			return json(response, 500, { error: 'No se ha podido generar la ROM.', log: tail(made.stdout + made.stderr) });
		}

		const name = (await readdir(dir)).find((file) => file.endsWith('.gb') || file.endsWith('.gbc'));
		const rom = await readFile(join(dir, name));
		await rm(out, { recursive: true, force: true });
		recordBuild('ok');
		json(response, 200, {
			name: 'gbsokoban' + extname(name),
			bytes: rom.toString('base64')
		});
	} finally {
		session.building = false;
	}
}

function readBody(request) {
	return new Promise((resolve, reject) => {
		let text = '';
		request.setEncoding('utf8');
		request.on('data', (chunk) => {
			text += chunk;
			if (text.length > MAX_SOURCE_BYTES * 2) request.destroy();
		});
		request.on('end', () => {
			try {
				resolve(JSON.parse(text));
			} catch (error) {
				reject(error);
			}
		});
		request.on('error', reject);
	});
}

function run(command, args, cwd) {
	return new Promise((resolve) => {
		const child = spawn(command, args, { cwd });
		let stdout = '';
		let stderr = '';
		const timer = setTimeout(() => child.kill('SIGKILL'), BUILD_TIMEOUT_MS);
		child.stdout.setEncoding('utf8').on('data', (chunk) => (stdout += chunk));
		child.stderr.setEncoding('utf8').on('data', (chunk) => (stderr += chunk));
		child.on('error', (error) => {
			clearTimeout(timer);
			resolve({ code: -1, stdout, stderr: String(error) });
		});
		child.on('close', (code) => {
			clearTimeout(timer);
			resolve({ code, stdout, stderr });
		});
	});
}

const tail = (text) => text.split('\n').slice(-25).join('\n');

const websockets = new WebSocketServer({ server, path: '/lsp' });

websockets.on('connection', (socket, request) => {
	const id = new URL(request.url, 'http://localhost').searchParams.get('session');
	const session = sessions.get(id);
	if (!session) return socket.close(4004, 'sesión desconocida');
	session.seen = Date.now();

	const language = spawn('java', ['-cp', classpath, 'org.eclipse.xtext.ide.server.ServerLauncher'], {
		cwd: session.dir
	});
	language.stderr.on('data', (chunk) => {
		const text = String(chunk);
		if (/SEVERE|Exception/.test(text)) console.error('[language]', text.split('\n')[0]);
	});

	// The language server expects a Content-Length header.
	socket.on('message', (data) => {
		session.seen = Date.now();
		const body = data.toString();
		language.stdin.write(`Content-Length: ${Buffer.byteLength(body)}\r\n\r\n${body}`);
	});

	let pending = Buffer.alloc(0);
	language.stdout.on('data', (chunk) => {
		pending = Buffer.concat([pending, chunk]);
		for (;;) {
			const split = pending.indexOf('\r\n\r\n');
			if (split < 0) return;
			const length = Number(/Content-Length: (\d+)/.exec(pending.subarray(0, split).toString())?.[1]);
			if (!length || pending.length < split + 4 + length) return;
			const message = pending.subarray(split + 4, split + 4 + length).toString();
			socket.send(message);
			if (session.participant && message.includes('"textDocument/publishDiagnostics"'))
				recordDiagnostics(session, JSON.parse(message).params.diagnostics);
			pending = pending.subarray(split + 4 + length);
		}
	});

	// The session outlives the socket; the sweep below removes it.
	socket.on('close', () => language.kill());
	language.on('close', () => socket.close());
	language.on('error', () => socket.close());
	language.stdin.on('error', () => socket.close());
});

function recordDiagnostics(session, diagnostics) {
	const errors = diagnostics.filter((d) => d.severity === 1).map((d) => d.message).sort();
	const warnings = diagnostics.filter((d) => d.severity === 2).map((d) => d.message).sort();
	const summary = JSON.stringify([errors, warnings]);
	if (summary === session.diagnostics) return;
	session.diagnostics = summary;
	record(session, { type: 'diagnostics', errors, warnings });
}

setInterval(() => {
	const limit = Date.now() - SESSION_TTL_MS;
	for (const session of sessions.values()) {
		if (session.seen < limit && !session.building) {
			sessions.delete(session.id);
			rm(session.dir, { recursive: true, force: true }).catch(() => {});
		}
	}
}, 10 * 60_000).unref();

server.listen(PORT);
