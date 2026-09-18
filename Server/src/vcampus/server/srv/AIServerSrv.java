/*
 * AIServerSrv
 *
 * Version 1.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * {@link IAIServerSrv} 的实现类，调用通义千问的原生文本生成接口
 * （{@code /services/aigc/text-generation/generation}）完成问答。项目
 * 没有引入任何 JSON 库（没用 Maven，依赖都是手动放 jar），所以这里手写
 * 了两个最小够用的小函数：一个转义问题文本里的特殊字符拼请求体，一个从
 * 响应体里把指定字段的值抠出来（这里用来取 {@code output.text}）——只
 * 处理这一个固定字段，不是通用 JSON 解析器，够这个场景用就行。
 *
 * <p>API Key 等配置从项目根目录下的 {@code Server/ai.properties} 读取，
 * 该文件不纳入版本管理（见 .gitignore），需要各开发者根据
 * {@code Server/ai.properties.example} 自行复制并填写自己申请的 API Key。</p>
 */
public class AIServerSrv implements IAIServerSrv {

    /**
     * ai.properties 候选路径，按顺序尝试：先找运行目录下的 {@code ai.properties}
     * （打包后的部署形态——jar 和配置文件平铺在同一个目录下），找不到再退回开发
     * 环境里的 {@code Server/ai.properties}（IDE 里从项目根目录运行时用的路径）。
     */
    private static final String[] CONFIG_PATHS = {"ai.properties", "Server/ai.properties"};

    /**
     * 系统提示词：告诉模型它是谁、嵌在什么系统里，并要求用纯文本回答
     * （不带 Markdown 标记），因为客户端界面是 JavaFX 的普通 {@code Label}，
     * 不会渲染 Markdown，直接显示 {@code ###}/{@code **} 这些符号会很难看。
     */
    private static final String SYSTEM_PROMPT = "你叫\"校园AI\"，是东南大学 Vcampus 虚拟校园系统里内置的智能助手，"
            + "通过该系统的服务器程序转发问题、调用你来回答，服务对象是登录系统的学生、教师和管理员，"
            + "可以帮忙解答学习、校园生活、图书馆/学籍/选课/医院/商店等系统功能相关的问题，也能闲聊。"
            + "回答时只用纯文本自然语言，不要使用任何 Markdown 标记"
            + "（不要出现 #、##、###、**、*、-、` 这类符号，不要用星号加粗，不要用井号做标题），"
            + "需要分点时直接用「一、二、三」或者「1、2、3」这种文字加换行来表达。";

    /** 单次请求超时时间。 */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

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
        String apiHost = config.getProperty("dashscope.api-host");
        String apiKey = config.getProperty("dashscope.api-key");
        String model = config.getProperty("dashscope.model", "qwen3-coder-plus");

        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IOException("未配置通义千问 API Key，请在 ai.properties"
                    + " 里填写 dashscope.api-key（参考 ai.properties.example 里的申请说明）");
        }
        if (apiHost == null || apiHost.trim().isEmpty()) {
            throw new IOException("未配置 dashscope.api-host，请在 ai.properties 里填写服务地址");
        }
        // 去掉末尾斜杠，拼上通义千问原生文本生成接口路径（不是 OpenAI 兼容格式，
        // 实测过：兼容模式的 /chat/completions 在这个网关上是 404，走原生格式才通）。
        String endpoint = (apiHost.endsWith("/") ? apiHost.substring(0, apiHost.length() - 1) : apiHost)
                + "/services/aigc/text-generation/generation";

        String requestBody = "{\"model\":\"" + escapeJson(model)
                + "\",\"input\":{\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(SYSTEM_PROMPT) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(question) + "\"}"
                + "]},\"parameters\":{}}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, java.nio.charset.StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("请求被中断：" + e.getMessage(), e);
        }

        if (response.statusCode() != 200) {
            throw new IOException("AI 服务返回异常状态码 " + response.statusCode() + "（请求地址：" + endpoint
                    + "）：" + response.body());
        }

        // 原生格式的回答文本在 output.text（不是 OpenAI 格式的 choices[0].message.content）。
        String text = extractJsonStringField(response.body(), "text");
        if (text == null) {
            throw new IOException("AI 服务响应格式不符合预期，未找到 text 字段：" + response.body());
        }
        // 系统提示词已经要求模型不要用 Markdown，这里再兜底清洗一遍——大模型不是
        // 100%听话，万一还是带了 #/**/- 这些标记，界面用的是普通 Label，不会渲染
        // Markdown，直接显示符号会很难看，所以再做一层保险。
        return stripMarkdown(text);
    }

    /**
     * 加载 {@code Server/ai.properties} 配置文件（只在首次调用时读取）。
     *
     * @return AI 服务配置
     * @throws IOException 当配置文件不存在或读取失败时抛出
     */
    private static synchronized Properties loadProperties() throws IOException {
        if (properties == null) {
            for (String path : CONFIG_PATHS) {
                File file = new File(path);
                if (file.isFile()) {
                    Properties p = new Properties();
                    try (InputStream in = new FileInputStream(file)) {
                        p.load(in);
                    }
                    properties = p;
                    break;
                }
            }
            if (properties == null) {
                throw new IOException("未找到 AI 配置文件（依次尝试过："
                        + String.join("、", CONFIG_PATHS)
                        + "），请复制 ai.properties.example 为 ai.properties 并填写你申请的 API Key，"
                        + "放在服务器程序运行时的当前目录下。");
            }
        }
        return properties;
    }

    /**
     * 把回答文本里常见的 Markdown 标记去掉，改成客户端 {@code Label} 能
     * 正常显示的纯文本。不追求完美还原排版，只求不出现裸露的符号。
     *
     * @param text 原始回答文本
     * @return 去掉 Markdown 标记后的纯文本
     */
    private static String stripMarkdown(String text) {
        String result = text;
        // 标题：行首连续的 # 去掉（可能带一个空格）。
        result = result.replaceAll("(?m)^#{1,6}\\s*", "");
        // 加粗/斜体：**文字**、__文字__、*文字*、_文字_ 只保留文字本身。
        result = result.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        result = result.replaceAll("__(.+?)__", "$1");
        result = result.replaceAll("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)", "$1");
        result = result.replaceAll("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)", "$1");
        // 行内代码/代码块的反引号直接去掉。
        result = result.replace("`", "");
        // 无序列表的行首 -/* 换成中文顿号，看起来还是个列表。
        result = result.replaceAll("(?m)^\\s*[-*+]\\s+", "• ");
        return result.trim();
    }

    /**
     * 转义字符串中会破坏 JSON 结构的字符，拼请求体时用。只处理最常见的
     * 几种（反斜杠、双引号、换行、回车、制表符），够聊天问答场景用。
     *
     * @param value 原始文本
     * @return 转义后可以安全放进 JSON 字符串字面量的文本
     */
    private static String escapeJson(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 从 JSON 文本里把指定字段名对应的字符串值抠出来，并反转义。不是通用
     * JSON 解析器——只找第一处 {@code "字段名":"..."} 的位置，DashScope
     * 单轮非流式响应里 {@code content} 只出现一次，够用。
     *
     * @param json     JSON 文本
     * @param fieldName 字段名
     * @return 字段值（已反转义）；找不到时返回 {@code null}
     */
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
        int i = valueStart + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    default -> value.append(next);
                }
                i += 2;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
                i++;
            }
        }
        return null;
    }
}
