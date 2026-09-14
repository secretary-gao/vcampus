/*
 * AIServerSrv
 *
 * Version 2.0
 *
 * 2026-09-14
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * {@link IAIServerSrv} 的实现类。Vcampus 服务端把问题交给独立部署的
 * Codex Agent，Agent 再通过 DashScope Responses API 使用千问模型回答。
 * 客户端和 Vcampus 服务端都不直接持有千问 API Key。
 *
 * <p>Agent 地址和访问令牌从项目根目录下的
 * {@code Server/ai.properties} 读取。该文件不纳入版本管理，配置模板见
 * {@code Server/ai.properties.example}。</p>
 */
public class AIServerSrv implements IAIServerSrv {

    /** ai.properties 相对项目根目录的路径。 */
    private static final String CONFIG_PATH = "Server/ai.properties";

    /** 默认单次请求超时时间，Codex Agent 首次生成可能需要较长时间。 */
    private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 120;

    /** 缓存已加载的配置，避免每次提问都重新读文件。 */
    private static Properties properties;

    /** 复用的 HTTP 客户端。 */
    private final HttpClient _httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * {@inheritDoc}
     */
    @Override
    public String ask(String question) throws IOException {
        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("问题内容不能为空");
        }

        Properties config = loadProperties();
        String agentUrl = requireProperty(config, "codex.agent-url");
        String agentToken = requireProperty(config, "codex.agent-token");
        int timeoutSeconds = readPositiveInt(config, "codex.request-timeout-seconds",
                DEFAULT_REQUEST_TIMEOUT_SECONDS);

        String requestBody = "{\"question\":\"" + escapeJson(question.trim()) + "\"}";
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(agentUrl))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + agentToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException e) {
            throw new IOException("Codex Agent 地址配置不正确：" + agentUrl, e);
        }

        HttpResponse<String> response;
        try {
            response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Codex Agent 请求被中断", e);
        }

        if (response.statusCode() != 200) {
            String detail = extractJsonStringField(response.body(), "error");
            if (detail == null || detail.isBlank()) {
                detail = "HTTP " + response.statusCode();
            }
            throw new IOException("Codex Agent 暂时不可用：" + detail);
        }

        String answer = extractJsonStringField(response.body(), "answer");
        if (answer == null || answer.isBlank()) {
            throw new IOException("Codex Agent 响应格式不符合预期，未找到 answer 字段");
        }
        return stripMarkdown(answer);
    }

    /**
     * 加载 {@code Server/ai.properties} 配置文件。
     *
     * @return AI 服务配置
     * @throws IOException 配置文件不存在或读取失败时抛出
     */
    private static synchronized Properties loadProperties() throws IOException {
        if (properties == null) {
            Properties loaded = new Properties();
            try (InputStream in = new FileInputStream(CONFIG_PATH)) {
                loaded.load(in);
            } catch (IOException e) {
                throw new IOException("未找到 AI 配置文件：" + CONFIG_PATH
                        + "，请根据 Server/ai.properties.example 配置 Codex Agent。", e);
            }
            properties = loaded;
        }
        return properties;
    }

    /** 读取必填配置项。 */
    private static String requireProperty(Properties config, String key) throws IOException {
        String value = config.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IOException("未配置 " + key + "，请检查 " + CONFIG_PATH);
        }
        return value.trim();
    }

    /** 读取正整数配置项。 */
    private static int readPositiveInt(Properties config, String key, int defaultValue) throws IOException {
        String value = config.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed <= 0) {
                throw new NumberFormatException("必须大于 0");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IOException(key + " 必须是正整数", e);
        }
    }

    /** 去掉客户端普通 Label 无法渲染的常见 Markdown 标记。 */
    private static String stripMarkdown(String text) {
        String result = text;
        result = result.replaceAll("(?m)^#{1,6}\\s*", "");
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        result = result.replaceAll("__(.+?)__", "$1");
        result = result.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "$1");
        result = result.replaceAll("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)", "$1");
        result = result.replace("`", "");
        result = result.replaceAll("(?m)^\\s*[-*+]\\s+", "• ");
        return result.trim();
    }

    /** 转义 JSON 字符串中的特殊字符。 */
    private static String escapeJson(String value) {
        StringBuilder result = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '\\' -> result.append("\\\\");
                case '"' -> result.append("\\\"");
                case '\n' -> result.append("\\n");
                case '\r' -> result.append("\\r");
                case '\t' -> result.append("\\t");
                default -> result.append(current);
            }
        }
        return result.toString();
    }

    /** 从固定结构的 JSON 响应中读取一个字符串字段并完成常见反转义。 */
    private static String extractJsonStringField(String json, String fieldName) {
        String marker = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return null;
        }
        int colonIndex = json.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return null;
        }
        int valueStart = json.indexOf('"', colonIndex + 1);
        if (valueStart < 0) {
            return null;
        }
        StringBuilder value = new StringBuilder();
        int index = valueStart + 1;
        while (index < json.length()) {
            char current = json.charAt(index);
            if (current == '\\' && index + 1 < json.length()) {
                char next = json.charAt(index + 1);
                switch (next) {
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    default -> value.append(next);
                }
                index += 2;
            } else if (current == '"') {
                return value.toString();
            } else {
                value.append(current);
                index++;
            }
        }
        return null;
    }
}
