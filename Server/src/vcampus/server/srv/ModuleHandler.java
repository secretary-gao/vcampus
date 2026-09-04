/*
 * ModuleHandler
 *
 * Version 1.0
 *
 * 2026-09-01
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.vo.Message;

import java.util.Set;

/**
 * 服务端业务模块处理器接口，用于把各业务模块的请求处理逻辑从
 * {@link ServerThread} 中解耦出来。
 *
 * <p>每个业务模块提供一个实现了本接口的处理器类（例如商店模块的
 * {@link StoreModuleHandler}），声明自己支持哪些消息名，并负责对这几类
 * 消息进行业务分发。这样新增模块只需新增一个处理器类并注册，而无需再改动
 * {@link ServerThread} 本身，能最大程度避免多人同时改共享文件造成的冲突。</p>
 */
public interface ModuleHandler {

    /**
     * 本处理器支持的消息名集合（对应 {@link Message#getName()}）。
     *
     * @return 支持的消息名集合
     */
    Set<String> supportedMessages();

    /**
     * 处理一条请求消息。
     *
     * @param request 客户端发来的请求消息
     * @return 处理结果对应的响应消息
     */
    Message handle(Message request);
}
