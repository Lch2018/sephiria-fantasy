@echo off
rem Double-clickable launcher for the development client.
rem
rem Plain gradlew.bat does nothing useful on its own -- the game only starts with the
rem "runClient" task, and a double-clicked batch window closes as soon as the command
rem finishes, so any error would flash past unseen. This keeps the window open.
rem
rem The first launch takes a while (Gradle daemon + mod loading); the game window shows
rem up after the console prints the runClient task line.

cd /d "%~dp0"

echo Starting the SEPHIRIA development client...
echo (first start can take a minute while Gradle warms up)
echo.

call gradlew.bat runClient

echo.
echo The game has exited.
pause
