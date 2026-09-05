@echo off
rem Build and start the Vcampus client (login window). Double-click this file, no typing needed.
rem NOTE: start the server first (run-server.bat), otherwise the client can't connect.
chcp 65001 >nul
setlocal
cd /d "%~dp0"
title Vcampus Client

call "%~dp0build.bat"
if errorlevel 1 (
  echo.
  echo Client not started: build failed.
  pause
  exit /b 1
)

echo.
echo Starting client...
echo.
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" --module-path "lib\javafx" --add-modules javafx.controls,javafx.fxml vcampus.client.view.LoginFrame

echo.
echo Client exited.
pause
