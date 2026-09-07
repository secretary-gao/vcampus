/*
 * UserDisabledException
 *
 * Version 1.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

/**
 * 登录时账号已被管理员禁用抛出的业务异常。与"密码错误"区分开来，
 * 便于服务器返回明确的错误提示（账号存在且密码正确的前提下，告知
 * "被禁用"不会像"用户名是否存在"那样泄露敏感信息，属于正常的产品反馈）。
 */
public class UserDisabledException extends Exception {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造方法。
     *
     * @param message 异常描述信息
     */
    public UserDisabledException(String message) {
        super(message);
    }
}
