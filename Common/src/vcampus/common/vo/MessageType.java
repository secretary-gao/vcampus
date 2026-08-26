/*
 * MessageType
 *
 * Version 1.0
 *
 * 2026-08-26
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.vo;

/**
 * 消息类型枚举，对应共享说明书中 Message 类 {@code type} 字段的"命令|数据"分类。
 *
 * <p>当前仅提供 {@link #COMMAND} 与 {@link #DATA} 两个占位取值。各业务模块
 * （如用户管理、图书馆等）在实现具体的请求/响应协议时，可以在此基础上扩展更
 * 细粒度的类型，或者约定用 {@link Message#getStatusCode()} /
 * {@link Message#getData()} 承载具体的业务动作标识。</p>
 */
public enum MessageType {

    /** 命令类消息，例如登录、注册、登出等操作请求。 */
    COMMAND,

    /** 数据类消息，例如查询结果、业务数据的传输。 */
    DATA
}
