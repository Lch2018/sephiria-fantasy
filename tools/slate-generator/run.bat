@echo off
rem Launch the Sephiria slate generator GUI. Closing the window quits the tool.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0slate-generator.ps1"
if errorlevel 1 pause
