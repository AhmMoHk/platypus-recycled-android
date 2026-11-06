@echo off
rem Platypus Reclayed - Android port: makes the Unity project from your copy of the game.
rem   setup_project.bat                                  finds the game in your Steam libraries (or asks)
rem   setup_project.bat "E:\Games\Platypus Reclayed"     uses that folder
rem   setup_project.bat -GameDir "..." -OutDir "D:\PRA" -Force
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\setup.ps1" %*
set ERR=%ERRORLEVEL%
echo.
if not "%ERR%"=="0" echo Setup stopped with an error ^(see above^).
pause
exit /b %ERR%
