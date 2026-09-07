/*
 * PermissionDeniedException
 *
 * Version 1.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

/**
 * 操作者权限不足时抛出的业务异常（例如非管理员尝试禁用/启用他人账号）。
 * 权限判断以服务器根据操作者登录ID重新查库得到的真实角色为准，
 * 不信任客户端在请求里附带的角色信息，避免被伪造角色绕过校验。
 */
public class PermissionDeniedException extends Exception {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造方法。
     *
     * @param message 异常描述信息
     */
    public PermissionDeniedException(String message) {
        super(message);
    }
}
