@echo off
rem Build and start the Vcampus server. Double-click this file, no typing needed.
chcp 65001 >nul
setlocal
cd /d "%~dp0"
title Vcampus Server

call "%~dp0build.bat"
if errorlevel 1 (
  echo.
  echo Server not started: build failed.
  pause
  exit /b 1
)

echo.
echo Starting server on port 8888...
echo Close this window to stop the server.
echo.
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.srv.Server

echo.
echo Server exited.
pause
