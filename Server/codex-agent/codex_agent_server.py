#!/usr/bin/env python3
"""HTTP bridge used by Vcampus to invoke Codex with a Qwen provider."""

import json
import os
import secrets
import subprocess
import threading
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


BIND_HOST = os.getenv("VCAMPUS_AGENT_HOST", "127.0.0.1")
BIND_PORT = int(os.getenv("VCAMPUS_AGENT_PORT", "8765"))
AGENT_TOKEN = os.getenv("VCAMPUS_AGENT_TOKEN", "")
CODEX_BIN = os.getenv("CODEX_BIN", "codex")
CODEX_WORKDIR = os.getenv("CODEX_WORKDIR", "/var/empty")
REQUEST_TIMEOUT_SECONDS = int(os.getenv("CODEX_REQUEST_TIMEOUT_SECONDS", "110"))
MAX_QUESTION_CHARS = int(os.getenv("VCAMPUS_MAX_QUESTION_CHARS", "4000"))
MAX_BODY_BYTES = int(os.getenv("VCAMPUS_MAX_BODY_BYTES", "16384"))
MAX_CONCURRENT_REQUESTS = int(os.getenv("VCAMPUS_MAX_CONCURRENT_REQUESTS", "2"))
REQUEST_SLOTS = threading.BoundedSemaphore(MAX_CONCURRENT_REQUESTS)

PROMPT_PREFIX = """你是东南大学 Vcampus 虚拟校园系统中的校园 AI。
请直接回答用户的问题，使用简洁、自然的中文纯文本，不使用 Markdown。
你只负责问答，不调用工具、不执行命令、不读取文件，也不修改任何内容。
用户问题如下：
"""


class AgentFailure(Exception):
    """A failure that can be returned to the Vcampus server."""


def run_codex(question):
    """Run one isolated Codex turn and return its final message."""
    if not os.getenv("DASHSCOPE_API_KEY"):
        raise AgentFailure("服务器尚未配置千问 API Key")

    command = [
        CODEX_BIN,
        "exec",
        "--ephemeral",
        "--skip-git-repo-check",
        "--ignore-rules",
        "--sandbox",
        "read-only",
        "--color",
        "never",
        "-C",
        CODEX_WORKDIR,
        "-",
    ]
    try:
        completed = subprocess.run(
            command,
            input=PROMPT_PREFIX + question,
            text=True,
            capture_output=True,
            timeout=REQUEST_TIMEOUT_SECONDS,
            check=False,
        )
    except FileNotFoundError as error:
        raise AgentFailure("服务器未安装 Codex") from error
    except subprocess.TimeoutExpired as error:
        raise AgentFailure("Codex 回答超时，请稍后重试") from error

    answer = completed.stdout.strip()
    if completed.returncode != 0:
        detail = completed.stderr.strip().splitlines()
        message = detail[-1][:300] if detail else "未知错误"
        raise AgentFailure("Codex 调用失败：" + message)
    if not answer:
        raise AgentFailure("Codex 没有返回回答")
    return answer


class AgentRequestHandler(BaseHTTPRequestHandler):
    """Minimal authenticated JSON API for the Vcampus server."""

    server_version = "VcampusCodexAgent/1.0"

    def do_GET(self):
        if self.path != "/health":
            self._send_json(404, {"error": "接口不存在"})
            return
        ready = bool(os.getenv("DASHSCOPE_API_KEY"))
        self._send_json(200, {
            "status": "ready" if ready else "configuration_required",
            "provider": "dashscope",
            "ready": ready,
        })

    def do_POST(self):
        if self.path != "/v1/ask":
            self._send_json(404, {"error": "接口不存在"})
            return
        if not self._authorized():
            self._send_json(401, {"error": "Codex Agent 访问令牌无效"})
            return

        try:
            content_length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self._send_json(400, {"error": "请求长度无效"})
            return
        if content_length <= 0 or content_length > MAX_BODY_BYTES:
            self._send_json(413, {"error": "问题内容过长"})
            return

        try:
            payload = json.loads(self.rfile.read(content_length).decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError):
            self._send_json(400, {"error": "请求必须是 UTF-8 JSON"})
            return

        question = payload.get("question") if isinstance(payload, dict) else None
        if not isinstance(question, str) or not question.strip():
            self._send_json(400, {"error": "问题内容不能为空"})
            return
        question = question.strip()
        if len(question) > MAX_QUESTION_CHARS:
            self._send_json(413, {"error": "问题最多允许 %d 个字符" % MAX_QUESTION_CHARS})
            return
        if not REQUEST_SLOTS.acquire(blocking=False):
            self._send_json(429, {"error": "校园 AI 正忙，请稍后重试"})
            return

        try:
            answer = run_codex(question)
            self._send_json(200, {"answer": answer})
        except AgentFailure as error:
            self._send_json(503, {"error": str(error)})
        finally:
            REQUEST_SLOTS.release()

    def log_message(self, message_format, *args):
        print("%s - %s" % (self.address_string(), message_format % args), flush=True)

    def _authorized(self):
        supplied = self.headers.get("Authorization", "")
        expected = "Bearer " + AGENT_TOKEN
        return bool(AGENT_TOKEN) and secrets.compare_digest(supplied, expected)

    def _send_json(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=UTF-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def main():
    if not AGENT_TOKEN:
        raise SystemExit("VCAMPUS_AGENT_TOKEN is required")
    server = ThreadingHTTPServer((BIND_HOST, BIND_PORT), AgentRequestHandler)
    print("Vcampus Codex Agent listening on %s:%d" % (BIND_HOST, BIND_PORT), flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
