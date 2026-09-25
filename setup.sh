#!/bin/sh
# Builds the language, fetches GBDK and sets up the web editor. Safe to rerun.
set -e

REPO=$(cd "$(dirname "$0")" && pwd)
GBDK_VERSION=4.4.0
GBDK_HOME=${GBDK_HOME:-$HOME/gbdk}

say() { printf '\n==> %s\n' "$1"; }

need() {
	command -v "$1" >/dev/null 2>&1 || { echo "Missing $1" >&2; exit 1; }
}

need java
need mvn
need node
need make

say "Building the language and the language server"
mvn -B -q -f "$REPO/pom.xml" -DskipTests package

if [ -x "$GBDK_HOME/bin/lcc" ]; then
	say "GBDK found in $GBDK_HOME"
else
	say "Downloading GBDK $GBDK_VERSION to $GBDK_HOME"
	case $(uname -s)-$(uname -m) in
		Darwin-*)      file=gbdk-macos-$(uname -m).tar.gz ;;
		Linux-aarch64) file=gbdk-linux-arm64.tar.gz ;;
		Linux-*)       file=gbdk-linux64.tar.gz ;;
		*) echo "No GBDK download for $(uname -s). Install GBDK and set GBDK_HOME." >&2; exit 1 ;;
	esac
	tmp=$(mktemp -d)
	curl -fsSL "https://github.com/gbdk-2020/gbdk-2020/releases/download/$GBDK_VERSION/$file" -o "$tmp/gbdk.tar.gz"
	mkdir -p "$(dirname "$GBDK_HOME")"
	tar -xzf "$tmp/gbdk.tar.gz" -C "$(dirname "$GBDK_HOME")"
	rm -rf "$tmp"
fi

say "Installing the web editor dependencies"
cd "$REPO/web"
npm install --silent

if command -v mdbook >/dev/null 2>&1; then
	say "Building the documentation"
	npm run docs --silent
else
	echo "Missing mdbook, so the documentation has not been built" >&2
fi

say "Done. Start the editor with: cd web && npm run dev"
