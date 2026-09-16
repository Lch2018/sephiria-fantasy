@echo off
rem Double-clickable build + install: compiles the mod and copies the jar into the PCL
rem instance's mods folder (same thing tools\deploy.ps1 does).

cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "tools\deploy.ps1"

echo.
pause
