@echo off
rem Puts this kit into the project you already made (no new export). Then run build_android.bat.
powershell -ExecutionPolicy Bypass -File "%~dp0tools\setup.ps1" -Update %*
pause
