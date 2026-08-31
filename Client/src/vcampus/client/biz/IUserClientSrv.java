/*
 * IUserClientSrv
 *
 * Version 1.0
 *
 * 2026-08-30
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.biz;

import vcampus.common.vo.Message;
import vcampus.common.vo.User;

import java.io.IOException;

/**
 * 客户端用户业务服务接口，对应共享说明书中"用户管理模块"的 IUserClientSrv。
 * 负责把界面层（{@code view}）的操作封装成网络请求发给服务器，并把服务器
 * 的响应原样返回给界面层解析（成功/失败、具体数据都在响应 {@link Message} 中）。
 */
public interface IUserClientSrv {

    /**
     * 发起登录请求。
     *
     * @param loginUser 登录信息（uId + 已做 MD5 摘要的 uPwd）
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message login(User loginUser) throws IOException, ClassNotFoundException;

    /**
     * 发起注册请求。
     *
     * @param newUser 待注册的用户信息（uPwd 已做 MD5 摘要）
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message register(User newUser) throws IOException, ClassNotFoundException;
}
