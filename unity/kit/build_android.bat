@echo off
rem Platypus Reclayed - Android port: builds PlatypusReclayed.apk from the project made by setup_project.bat.
rem   build_android.bat                                   finds Unity 2022.3.62f2 through Unity Hub
rem   build_android.bat "C:\...\2022.3.62f2\Editor\Unity.exe"
rem   build_android.bat -Astc 4x4                         sharper textures, bigger APK (default 6x6)
setlocal
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\build.ps1" %*
set ERR=%ERRORLEVEL%
echo.
if not "%ERR%"=="0" echo Build stopped with an error ^(see above^).
pause
exit /b %ERR%
