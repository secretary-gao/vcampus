# Vcampus Codex Agent

调用链：Vcampus 服务端 -> 本服务 -> Codex CLI -> DashScope Responses API -> 千问。

服务只监听服务器本机 `127.0.0.1:8765`。本地开发通过项目根目录的
`start-codex-tunnel.bat` 建立 SSH 隧道，然后 Vcampus 调用
`http://127.0.0.1:18765/v1/ask`。

## 一键安装

服务器需安装 Python 3、Codex CLI，并支持 `systemctl --user`。把本目录上传到
服务器后运行：

```bash
chmod +x install.sh
./install.sh
```

安装脚本会要求输入 DashScope API Key，自动生成 Agent 访问令牌，并在最后输出
一段 `Server/ai.properties` 配置。把该配置写入运行 Vcampus 服务端的机器即可。

本地建立隧道时，可把 SSH 目标作为参数传入：

```bat
start-codex-tunnel.bat username@server.example.com
```

不传参数时默认使用本机 SSH 配置中的 `codex-server` 别名。

## 手动安装

服务器需要以下文件：

- `~/.local/share/vcampus-codex-agent/codex_agent_server.py`
- `~/.local/share/vcampus-codex-home/config.toml`
- `~/.config/systemd/user/vcampus-codex-agent.service`
- `~/.config/vcampus-codex-agent.env`

环境文件包含两项，权限应为 `600`：

```text
DASHSCOPE_API_KEY=你的千问API Key
VCAMPUS_AGENT_TOKEN=Vcampus访问Agent的随机令牌
```

启动后可在服务器执行：

```bash
systemctl --user daemon-reload
systemctl --user enable --now vcampus-codex-agent
curl http://127.0.0.1:8765/health
```

Codex 使用独立的 `CODEX_HOME`，不会读取服务器上个人 Codex 的模型、插件、
技能或项目配置。模型提供方固定为 DashScope，协议为 Responses API。
