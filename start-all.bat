@echo off
rem One-click launch: build once, then open two new windows for server and client.
rem Double-click this file, no typing in a terminal needed at all.
chcp 65001 >nul
setlocal
cd /d "%~dp0"

call "%~dp0build.bat"
if errorlevel 1 (
  echo.
  echo Aborted: build failed.
  pause
  exit /b 1
)

echo.
echo Starting server window...
start "Vcampus Server" cmd /k "cd /d %~dp0 && java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.srv.Server"

rem Give the server a moment to bind its port before the client tries to connect.
timeout /t 2 /nobreak >nul

echo Starting client window...
start "Vcampus Client" cmd /k "cd /d %~dp0 && java -cp "bin;lib\mysql-connector-j-9.7.0.jar" --module-path "lib\javafx\lib" --add-modules javafx.controls,javafx.fxml vcampus.client.view.LoginFrame"

echo.
echo Server and client launched in separate windows.
