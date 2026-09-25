FROM debian:trixie-slim

ARG GBDK_VERSION=4.4.0
ARG MDBOOK_VERSION=v0.5.4
ENV GBDK_HOME=/opt/gbdk

RUN apt-get update && apt-get install -y --no-install-recommends \
	openjdk-21-jdk-headless maven nodejs npm make curl ca-certificates \
	&& rm -rf /var/lib/apt/lists/*

RUN if [ "$(uname -m)" = aarch64 ]; then gbdk=gbdk-linux-arm64; mdbook=aarch64-unknown-linux-musl; \
	else gbdk=gbdk-linux64; mdbook=x86_64-unknown-linux-gnu; fi \
	&& curl -fsSL "https://github.com/gbdk-2020/gbdk-2020/releases/download/${GBDK_VERSION}/${gbdk}.tar.gz" | tar -xz -C /opt \
	&& curl -fsSL "https://github.com/rust-lang/mdBook/releases/download/${MDBOOK_VERSION}/mdbook-${MDBOOK_VERSION}-${mdbook}.tar.gz" | tar -xz -C /usr/local/bin

WORKDIR /app
COPY . .

RUN mvn -B -q -DskipTests package

WORKDIR /app/web
RUN npm ci && npm run build

EXPOSE 3000
CMD ["node", "server.mjs", "--serve-dist"]
