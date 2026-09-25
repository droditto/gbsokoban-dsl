import { defineConfig } from 'vite';

// mdBook's output is in public/docs; /docs has to reach its index.
const docsIndex = {
	name: 'docs-index',
	configureServer(server) {
		server.middlewares.use((request, response, next) => {
			if (request.url === '/docs' || request.url === '/docs/') request.url = '/docs/index.html';
			next();
		});
	}
};

export default defineConfig({
	plugins: [docsIndex],
	server: {
		proxy: {
			'/lsp': { target: 'ws://localhost:3000', ws: true },
			'/api': { target: 'http://localhost:3000' }
		}
	}
});
