/*
 * IConstant
 *
 * Version 1.0
 *
 * 2026-08-28
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.common.constant;

/**
 * 系统公共常量接口，集中定义客户端与服务器端共用的常量，包括服务器地址、
 * 监听端口，以及通信状态码。接口中的字段隐式为 {@code public static final}。
 *
 * <p>
 * 客户端与服务器端都必须引用这里的常量，确保两侧使用相同的端口和状态码
 * 约定，避免硬编码造成的不一致。
 * </p>
 */
public interface IConstant {

    /** 服务器地址（本机测试时用回环地址）。 */
    String SERVER_HOST = "127.0.0.1";

    /** 服务器监听端口。 */
    int SERVER_PORT = 8888;

    /** 状态码：操作成功。 */
    String STATUS_SUCCESS = "200";

    /** 状态码：登录失败（密码错误）。 */
    String STATUS_LOGIN_FAIL = "401";

    /** 状态码：用户不存在。 */
    String STATUS_USER_NOT_FOUND = "404";

    /** 状态码：请求参数不符合业务要求。 */
    String STATUS_BAD_REQUEST = "400";

    /** 状态码：注册时用户名已存在。 */
    String STATUS_USER_EXISTS = "409";

    /** 状态码：服务器内部异常。 */
    String STATUS_ERROR = "500";

    /** 消息名：登录请求，对应 {@link vcampus.common.vo.Message#getName()}。 */
    String MSG_LOGIN = "login";

    /** 消息名：注册请求，对应 {@link vcampus.common.vo.Message#getName()}。 */
    String MSG_REGISTER = "register";

    /** 消息名：登出请求，对应 {@link vcampus.common.vo.Message#getName()}。 */
    String MSG_LOGOUT = "logout";

    // ========== 图书馆模块消息类型 ==========
    String MSG_QUERY_BOOKS = "queryBooks";
    String MSG_BORROW_BOOK = "borrowBook";
    String MSG_RETURN_BOOK = "returnBook";
    String MSG_GET_BORROW_RECORDS = "getBorrowRecords";
}
