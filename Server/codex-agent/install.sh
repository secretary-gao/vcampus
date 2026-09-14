#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AGENT_DIR="$HOME/.local/share/vcampus-codex-agent"
CODEX_DIR="$HOME/.local/share/vcampus-codex-home"
SYSTEMD_DIR="$HOME/.config/systemd/user"
ENV_FILE="$HOME/.config/vcampus-codex-agent.env"

if ! command -v python3 >/dev/null 2>&1; then
    echo "Python 3 is required." >&2
    exit 1
fi
if ! command -v codex >/dev/null 2>&1; then
    echo "Codex CLI is required." >&2
    exit 1
fi
if ! command -v systemctl >/dev/null 2>&1; then
    echo "systemd is required." >&2
    exit 1
fi

if [[ -z "${DASHSCOPE_API_KEY:-}" ]]; then
    read -r -s -p "DashScope API Key: " DASHSCOPE_API_KEY
    echo
fi
if [[ -z "$DASHSCOPE_API_KEY" ]]; then
    echo "DashScope API Key cannot be empty." >&2
    exit 1
fi

VCAMPUS_AGENT_TOKEN="${VCAMPUS_AGENT_TOKEN:-$(python3 -c 'import secrets; print(secrets.token_hex(32))')}"
CODEX_BIN="$(command -v codex)"

install -d -m 700 "$AGENT_DIR" "$AGENT_DIR/workspace" "$CODEX_DIR" "$SYSTEMD_DIR"
install -m 700 "$SCRIPT_DIR/codex_agent_server.py" "$AGENT_DIR/codex_agent_server.py"
install -m 600 "$SCRIPT_DIR/config.toml.example" "$CODEX_DIR/config.toml"
sed "s|Environment=CODEX_BIN=.*|Environment=CODEX_BIN=$CODEX_BIN|" \
    "$SCRIPT_DIR/vcampus-codex-agent.service.example" \
    > "$SYSTEMD_DIR/vcampus-codex-agent.service"
chmod 600 "$SYSTEMD_DIR/vcampus-codex-agent.service"

umask 077
printf 'DASHSCOPE_API_KEY=%s\nVCAMPUS_AGENT_TOKEN=%s\n' \
    "$DASHSCOPE_API_KEY" "$VCAMPUS_AGENT_TOKEN" > "$ENV_FILE"

systemctl --user daemon-reload
systemctl --user enable --now vcampus-codex-agent

echo
echo "Codex Agent installed. Vcampus configuration:"
echo "codex.agent-url=http://127.0.0.1:18765/v1/ask"
echo "codex.agent-token=$VCAMPUS_AGENT_TOKEN"
echo "codex.request-timeout-seconds=120"
