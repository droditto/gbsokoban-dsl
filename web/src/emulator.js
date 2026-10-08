// Based on binjgb's minimal example.
const SCREEN_WIDTH = 160;
const SCREEN_HEIGHT = 144;
const TICKS_PER_SECOND = 4194304;
const MAX_UPDATE_SEC = 5 / 60;

const EVENT_AUDIO_BUFFER_FULL = 2;
const EVENT_UNTIL_TICKS = 4;

const AUDIO_FRAMES = 4096;
const AUDIO_LATENCY_SEC = 0.1;
// No colour correction.
const COLOUR_CURVE = 0;

// No Select: the engine never reads it.
const KEYS = {
	ArrowUp: '_set_joyp_up',
	ArrowDown: '_set_joyp_down',
	ArrowLeft: '_set_joyp_left',
	ArrowRight: '_set_joyp_right',
	KeyZ: '_set_joyp_B',
	KeyX: '_set_joyp_A',
	Enter: '_set_joyp_start'
};

let factory = null;
let running = null;
let sound = null;

/** Browsers only resume audio inside a user gesture. */
function openSound() {
	if (sound) return sound;
	sound = new AudioContext();
	const wake = () => sound.resume();
	document.addEventListener('pointerdown', wake);
	document.addEventListener('keydown', wake);
	return sound;
}

function loadBinjgb() {
	const here = '/binjgb/';
	factory ??= new Promise((done, failed) => {
		const script = document.createElement('script');
		script.src = `${here}binjgb.js`;
		script.onload = () => done(window.Binjgb({ locateFile: (file) => here + file }));
		script.onerror = () => failed(new Error('No se pudo cargar el emulador.'));
		document.head.append(script);
	});
	return factory;
}

function wasmBytes(module, pointer, size) {
	return new Uint8Array(module.HEAP8.buffer, pointer, size);
}

class Emulator {
	constructor(module, rom, canvas) {
		this.module = module;

		// binjgb wants the ROM padded to 32 KB banks.
		const size = (rom.byteLength + 0x7fff) & ~0x7fff;
		this.romPointer = module._malloc(size);
		wasmBytes(module, this.romPointer, size).fill(0).set(rom);

		this.emulator = module._emulator_new_simple(this.romPointer, size, openSound().sampleRate,
			AUDIO_FRAMES, COLOUR_CURVE);
		if (this.emulator === 0) throw new Error('La ROM no es válida.');

		// Without a joypad, the _set_joyp_* calls are ignored.
		this.joypad = module._joypad_new();
		module._emulator_set_default_joypad_callback(this.emulator, this.joypad);

		this.screen = canvas.getContext('2d');
		this.image = this.screen.createImageData(SCREEN_WIDTH, SCREEN_HEIGHT);
		this.pixels = wasmBytes(module, module._get_frame_buffer_ptr(this.emulator),
			module._get_frame_buffer_size(this.emulator));
		this.samples = wasmBytes(module, module._get_audio_buffer_ptr(this.emulator),
			module._get_audio_buffer_capacity(this.emulator));

		this.playFrom = 0;
		this.lastSecond = 0;
		this.leftoverTicks = 0;

		this.onKey = (event) => {
			const set = KEYS[event.code];
			if (!set || document.activeElement?.closest('.cm-editor, button, a')) return;
			event.preventDefault();
			this.module[set](this.emulator, event.type === 'keydown');
		};
		document.addEventListener('keydown', this.onKey);
		document.addEventListener('keyup', this.onKey);

		this.frame = this.frame.bind(this);
		this.pending = requestAnimationFrame(this.frame);
	}

	stop() {
		cancelAnimationFrame(this.pending);
		document.removeEventListener('keydown', this.onKey);
		document.removeEventListener('keyup', this.onKey);
		this.module._emulator_delete(this.emulator);
		this.module._joypad_delete(this.joypad);
		this.module._free(this.romPointer);
	}

	frame(nowMs) {
		this.pending = requestAnimationFrame(this.frame);

		const nowSec = nowMs / 1000;
		const elapsed = Math.min(Math.max(nowSec - (this.lastSecond || nowSec), 0), MAX_UPDATE_SEC);
		this.lastSecond = nowSec;

		const target = this.ticks + elapsed * TICKS_PER_SECOND - this.leftoverTicks;
		this.runUntil(target);
		this.leftoverTicks = (this.ticks - target) | 0;

		this.image.data.set(this.pixels);
		this.screen.putImageData(this.image, 0, 0);
	}

	get ticks() {
		return this.module._emulator_get_ticks_f64(this.emulator);
	}

	runUntil(ticks) {
		for (;;) {
			const event = this.module._emulator_run_until_f64(this.emulator, ticks);
			if (event & EVENT_AUDIO_BUFFER_FULL) this.pushSound();
			if (event & EVENT_UNTIL_TICKS) return;
		}
	}

	pushSound() {
		if (sound.state !== 'running') return;

		const now = sound.currentTime;
		if (this.playFrom < now) this.playFrom = now + AUDIO_LATENCY_SEC;

		const buffer = sound.createBuffer(2, AUDIO_FRAMES, sound.sampleRate);
		const left = buffer.getChannelData(0);
		const right = buffer.getChannelData(1);
		// Not centred: an offset would click whenever the sound stops.
		for (let i = 0; i < AUDIO_FRAMES; i++) {
			left[i] = this.samples[2 * i] / 255;
			right[i] = this.samples[2 * i + 1] / 255;
		}

		const source = sound.createBufferSource();
		source.buffer = buffer;
		source.connect(sound.destination);
		source.start(this.playFrom);
		this.playFrom += AUDIO_FRAMES / sound.sampleRate;
	}
}

export function stop(canvas) {
	running?.stop();
	running = null;
	canvas.getContext('2d').clearRect(0, 0, canvas.width, canvas.height);
}

export async function play(canvas, rom) {
	const module = await loadBinjgb();
	// Cleared first, so a rejected ROM is not stopped twice.
	stop(canvas);
	running = new Emulator(module, rom, canvas);
	canvas.focus();
}
