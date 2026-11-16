#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
sh compilar.sh
mkdir -p evidencias
javac --release 21 -encoding UTF-8 -cp build/server -d build/tests pruebas/StoreTest.java
java -cp build/server:build/tests StoreTest > evidencias/persistencia.txt
cat evidencias/persistencia.txt
node pruebas/integracion.mjs
