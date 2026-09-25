// The program travels in the fragment, which never reaches the server: deflated, then base64url.
const PREFIX = '#code=';

async function pipe(bytes, stream) {
	return new Uint8Array(await new Response(new Blob([bytes]).stream().pipeThrough(stream)).arrayBuffer());
}

export async function readFromUrl() {
	if (!location.hash.startsWith(PREFIX)) return null;
	try {
		const base64 = location.hash.slice(PREFIX.length).replace(/-/g, '+').replace(/_/g, '/');
		const packed = Uint8Array.from(atob(base64), (c) => c.charCodeAt(0));
		return new TextDecoder().decode(await pipe(packed, new DecompressionStream('deflate-raw')));
	} catch {
		return null;
	}
}

export async function linkTo(text) {
	const packed = await pipe(new TextEncoder().encode(text), new CompressionStream('deflate-raw'));
	const base64 = btoa(Array.from(packed, (byte) => String.fromCharCode(byte)).join(''));
	return location.origin + location.pathname + PREFIX
		+ base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}
