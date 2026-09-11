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
import vcampus.common.vo.Student;
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

    /**
     * 发起登出请求。
     *
     * @param currentUser 当前用户
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message logout(User currentUser) throws IOException, ClassNotFoundException;

    /**
     * 发起禁用/启用账号请求，仅管理员操作有效（服务器端会重新校验操作者
     * 的真实角色，不是客户端说自己是管理员就能生效）。
     *
     * @param operatorUId 发起操作的登录ID（当前登录的管理员）
     * @param targetUId   被操作的目标用户登录ID
     * @param newStatus   新状态，取值见 {@link User#STATUS_NORMAL}/
     *                    {@link User#STATUS_DISABLED}
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message setUserStatus(String operatorUId, String targetUId, String newStatus)
            throws IOException, ClassNotFoundException;

    /**
     * 查询全部"待审核"账号，仅管理员操作有效（服务器端会重新校验操作者
     * 的真实角色）。
     *
     * @param operatorUId 发起查询的登录ID（当前登录的管理员）
     * @return 服务器返回的响应消息，成功时 {@code data} 为
     *         {@code java.util.List<User>}
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message listPendingUsers(String operatorUId) throws IOException, ClassNotFoundException;

    /**
     * 发起"学生"角色的自助注册请求：除了创建登录账号，还会同步创建一条
     * 对应的学籍记录（{@code tblStudent}），不然新注册的学生账号进学籍/
     * 选课模块会因为查不到学籍记录而不能用。
     *
     * @param newUser 待注册的用户信息（uPwd 已做 MD5 摘要，uRole 应为"学生"）
     * @param profile 学籍档案（班级/专业/年级等，{@code userId} 会由服务器
     *                用 {@code newUser.getUId()} 覆盖，不用提前填）
     * @return 服务器返回的响应消息
     * @throws IOException            网络连接异常（如服务器未启动）
     * @throws ClassNotFoundException 反序列化响应对象失败
     */
    Message registerStudent(User newUser, Student profile)
            throws IOException, ClassNotFoundException;
}
