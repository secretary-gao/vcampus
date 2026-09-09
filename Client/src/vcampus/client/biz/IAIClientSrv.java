/*
 * IAIClientSrv
 *
 * Version 1.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.vo.Message;

import java.io.IOException;

/**
 * 客户端 AI 问答业务服务接口，把界面层的提问操作封装成网络请求发给
 * 服务器，服务器再转发给通义千问，回答通过响应 {@link Message} 传回来。
 */
public interface IAIClientSrv {

    /**
     * 发起一次问答请求。
     *
     * @param question 问题文本
     * @return 服务器返回的响应消息（{@code data} 为 AI 回答文本或错误提示）
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message ask(String question) throws IOException, ClassNotFoundException;
}
