#!/bin/sh
# Builds the language, fetches GBDK and mdBook, and sets up the web editor. Safe to rerun.
set -eu

REPO=$(cd "$(dirname "$0")" && pwd)
GBDK_VERSION=4.4.0
MDBOOK_VERSION=v0.5.4
GBDK_HOME=${GBDK_HOME:-$HOME/gbdk}

echo "Building the language and the language server"
mvn -B -q -f "$REPO/pom.xml" -DskipTests package

if [ ! -x "$GBDK_HOME/bin/lcc" ]; then
	echo "Downloading GBDK $GBDK_VERSION to $GBDK_HOME"
	case $(uname -s)-$(uname -m) in
		Darwin-*)      file=gbdk-macos-$(uname -m).tar.gz ;;
		Linux-aarch64) file=gbdk-linux-arm64.tar.gz ;;
		Linux-*)       file=gbdk-linux64.tar.gz ;;
		*) echo "No GBDK download for $(uname -s). Install GBDK and set GBDK_HOME." >&2; exit 1 ;;
	esac
	mkdir -p "$(dirname "$GBDK_HOME")"
	curl -fsSL "https://github.com/gbdk-2020/gbdk-2020/releases/download/$GBDK_VERSION/$file" | tar -xz -C "$(dirname "$GBDK_HOME")"
fi

echo "Installing the web editor dependencies"
cd "$REPO/web"
npm install --silent

MDBOOK=$(command -v mdbook || echo "$REPO/build/bin/mdbook")
if [ ! -x "$MDBOOK" ]; then
	echo "Downloading mdBook $MDBOOK_VERSION"
	case $(uname -s)-$(uname -m) in
		Darwin-arm64)  file=aarch64-apple-darwin ;;
		Darwin-*)      file=x86_64-apple-darwin ;;
		Linux-aarch64) file=aarch64-unknown-linux-musl ;;
		Linux-*)       file=x86_64-unknown-linux-musl ;;
		*) echo "No mdBook download for $(uname -s). Install mdBook." >&2; exit 1 ;;
	esac
	mkdir -p "$REPO/build/bin"
	curl -fsSL "https://github.com/rust-lang/mdBook/releases/download/$MDBOOK_VERSION/mdbook-$MDBOOK_VERSION-$file.tar.gz" | tar -xz -C "$REPO/build/bin"
fi

echo "Building the documentation"
"$MDBOOK" build docs

echo "Done. Start the editor with: cd web && npm run dev"
