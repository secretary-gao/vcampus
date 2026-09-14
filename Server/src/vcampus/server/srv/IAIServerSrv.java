/*
 * IAIServerSrv
 *
 * Version 1.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import java.io.IOException;

/**
 * 服务器端 AI 问答业务服务接口。由 {@link ServerThread} 调用，屏蔽
 * {@link AIServerSrv} 内部具体调用哪个大模型服务、走什么协议的细节，
 * 对上只暴露"问一句话、拿一句回答"这一个业务动作。
 */
public interface IAIServerSrv {

    /**
     * 向 AI 提问并返回回答文本。
     *
     * @param question 用户输入的问题文本
     * @return AI 的回答文本
     * @throws IOException Agent 配置错误、网络请求失败或响应格式不符合预期时抛出
     */
    String ask(String question) throws IOException;
}
