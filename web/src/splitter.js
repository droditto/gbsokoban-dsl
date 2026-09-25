export function makeResizable(splitter) {
	splitter.addEventListener('pointerdown', (event) => {
		event.preventDefault();
		splitter.setPointerCapture(event.pointerId);
		document.body.classList.add('resizing');

		const move = (moved) =>
			document.documentElement.style.setProperty('--panel-width', `${window.innerWidth - moved.clientX}px`);
		const release = () => {
			document.body.classList.remove('resizing');
			splitter.removeEventListener('pointermove', move);
		};

		splitter.addEventListener('pointermove', move);
		splitter.addEventListener('pointerup', release, { once: true });
		splitter.addEventListener('pointercancel', release, { once: true });
	});
}
