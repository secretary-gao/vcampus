import os
import subprocess
import unittest
from unittest.mock import patch

import codex_agent_server


class RunCodexTest(unittest.TestCase):

    def test_requires_dashscope_key(self):
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaisesRegex(codex_agent_server.AgentFailure, "千问 API Key"):
                codex_agent_server.run_codex("你好")

    def test_invokes_isolated_read_only_codex(self):
        result = subprocess.CompletedProcess([], 0, stdout="校园回答\n", stderr="")
        with patch.dict(os.environ, {"DASHSCOPE_API_KEY": "test-key"}, clear=True):
            with patch.object(codex_agent_server.subprocess, "run", return_value=result) as run:
                answer = codex_agent_server.run_codex("图书馆几点关门？")

        self.assertEqual("校园回答", answer)
        command = run.call_args.args[0]
        self.assertIn("--ephemeral", command)
        self.assertIn("--ignore-rules", command)
        self.assertIn("read-only", command)
        self.assertIn("图书馆几点关门？", run.call_args.kwargs["input"])


if __name__ == "__main__":
    unittest.main()
