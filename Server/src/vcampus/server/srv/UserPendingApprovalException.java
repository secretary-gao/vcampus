/*
 * UserPendingApprovalException
 *
 * Version 1.0
 *
 * 2026-09-11
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

/**
 * 登录时账号还处于"待审核"状态时抛出的业务异常。新注册的账号默认是
 * 待审核状态，需要管理员在"账号管理"页面手动确认后才能正常登录，
 * 跟"密码错误"、"账号已被禁用"分开处理，方便客户端给出明确提示。
 */
public class UserPendingApprovalException extends Exception {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造方法。
     *
     * @param message 异常描述信息
     */
    public UserPendingApprovalException(String message) {
        super(message);
    }
}
