/*
 * UserExistsException
 *
 * Version 1.0
 *
 * 2026-08-30
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

/**
 * 注册时登录ID已被占用抛出的业务异常。与数据库异常（{@link java.sql.SQLException}）
 * 区分开，便于上层根据异常类型返回不同的状态码。
 */
public class UserExistsException extends Exception {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造方法。
     *
     * @param message 异常描述信息
     */
    public UserExistsException(String message) {
        super(message);
    }
}
