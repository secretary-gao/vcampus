@echo off
rem Forward the local Vcampus AI port to the Codex Agent on codex-server.
chcp 65001 >nul
setlocal
title Vcampus Codex Tunnel

set "CODEX_SSH_HOST=%~1"
if "%CODEX_SSH_HOST%"=="" set "CODEX_SSH_HOST=codex-server"

echo Connecting Vcampus to the remote Codex Agent...
echo SSH host: %CODEX_SSH_HOST%
echo Keep this window open while using Campus AI.
echo.
ssh -N -o ExitOnForwardFailure=yes -L 18765:127.0.0.1:8765 "%CODEX_SSH_HOST%"

echo.
echo Codex Agent connection closed.
pause
