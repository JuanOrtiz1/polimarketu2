#!/bin/sh
set -eu
cd "$(dirname "$0")"
command -v javac >/dev/null 2>&1 || { echo 'Instale JDK 21 y configure PATH.' >&2; exit 1; }
version=$(javac -version 2>&1)
case "$version" in 'javac 21.'*|'javac 21') ;; *) echo "Se requiere JDK 21; detectado: $version" >&2; exit 1;; esac
mkdir -p build/server build/client build/tests
find sistema/src -name '*.java' > build/server-sources.txt
javac --release 21 -encoding UTF-8 -d build/server @build/server-sources.txt
javac --release 21 -encoding UTF-8 -d build/client clientes/java/ClienteConsola.java
printf '%s\n' 'Compilación completa: build/server y build/client'
