@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
where javac >nul 2>&1
if errorlevel 1 (echo Instale JDK 21 y configure PATH. & exit /b 1)
for /f "tokens=2" %%v in ('javac -version 2^>^&1') do set "JDK_VERSION=%%v"
for /f "tokens=1 delims=." %%v in ("%JDK_VERSION%") do if not "%%v"=="21" (echo Se requiere JDK 21. & exit /b 1)
if not exist build\server mkdir build\server
if not exist build\client mkdir build\client
if not exist build\tests mkdir build\tests
(for /r sistema\src %%f in (*.java) do (
  set "SOURCE_PATH=%%f"
  echo "!SOURCE_PATH:\=/!"
)) > build\server-sources.txt
javac --release 21 -encoding UTF-8 -d build\server @build\server-sources.txt
if errorlevel 1 exit /b 1
javac --release 21 -encoding UTF-8 -d build\client clientes\java\ClienteConsola.java
if errorlevel 1 exit /b 1
echo Compilacion completa: build/server y build/client
