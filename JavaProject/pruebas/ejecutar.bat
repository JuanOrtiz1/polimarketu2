@echo off
setlocal
cd /d "%~dp0.."
call compilar.bat
if errorlevel 1 exit /b 1
if not exist evidencias mkdir evidencias
javac --release 21 -encoding UTF-8 -cp build/server -d build/tests pruebas/StoreTest.java
if errorlevel 1 exit /b 1
java -cp "build/server;build/tests" StoreTest > evidencias/persistencia.txt
if errorlevel 1 exit /b 1
type evidencias\persistencia.txt
node pruebas/integracion.mjs
if errorlevel 1 exit /b 1
