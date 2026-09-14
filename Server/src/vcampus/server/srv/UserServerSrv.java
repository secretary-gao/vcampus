/*
 * UserServerSrv
 *
 * Version 1.0
 *
 * 2026-08-30
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.vo.User;
import vcampus.server.dao.UserDAO;

import java.io.IOException;
import java.sql.SQLException;

/**
 * {@link IUserServerSrv} 的实现类，承载用户管理模块的业务逻辑，
 * 具体的数据库读写委托给 {@link UserDAO}。
 */
public class UserServerSrv implements IUserServerSrv {

    /** 用户表数据访问对象。 */
    private final UserDAO _userDAO = new UserDAO();

    /**
     * {@inheritDoc}
     */
    @Override
    public User login(User loginUser)
            throws SQLException, IOException, UserDisabledException, UserPendingApprovalException {
        if (loginUser == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        loginUser.setUId(validateUserId(loginUser.getUId()));
        User found = _userDAO.findByUId(loginUser.getUId());
        if (found == null) {
            return null;
        }
        if (found.getUPwd() == null || !found.getUPwd().equals(loginUser.getUPwd())) {
            return null;
        }
        if (User.STATUS_DISABLED.equals(found.getUStatus())) {
            throw new UserDisabledException("账号已被管理员禁用：" + found.getUId());
        }
        if (User.STATUS_PENDING.equals(found.getUStatus())) {
            throw new UserPendingApprovalException("账号还在等待管理员审核，暂时无法登录：" + found.getUId());
        }
        return found;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean register(User newUser) throws SQLException, IOException, UserExistsException {
        if (newUser == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        newUser.setUId(validateUserId(newUser.getUId()));
        User existing = _userDAO.findByUId(newUser.getUId());
        if (existing != null) {
            throw new UserExistsException("登录ID已被注册：" + newUser.getUId());
        }
        // 不管客户端传来的 uStatus 是什么，注册一律强制设为"待审核"，
        // 必须管理员在账号管理里手动审核通过才能登录。
        newUser.setUStatus(User.STATUS_PENDING);
        return _userDAO.insert(newUser);
    }

    /**
     * 校验并规范化登录 ID，保证它符合 tblUser.uId 的 CHAR(8) 约束。
     *
     * @param value 原始登录 ID
     * @return 去除首尾空格后的登录 ID
     */
    static String validateUserId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("登录ID不能为空");
        }
        String normalized = value.trim();
        if (normalized.length() != 8) {
            throw new IllegalArgumentException("登录ID必须为8位");
        }
        return normalized;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean logout(User currentUser) throws SQLException, IOException {
        return currentUser != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setUserStatus(String operatorUId, String targetUId, String newStatus)
            throws SQLException, IOException, PermissionDeniedException {
        if (operatorUId == null || targetUId == null || newStatus == null) {
            throw new IllegalArgumentException("操作者ID、目标ID、新状态均不能为空");
        }

        User operator = _userDAO.findByUId(operatorUId);
        if (operator == null || !"管理员".equals(operator.getURole())) {
            throw new PermissionDeniedException("无权限执行该操作，仅管理员可禁用/启用账号");
        }

        User target = _userDAO.findByUId(targetUId);
        if (target == null) {
            throw new IllegalArgumentException("目标用户不存在：" + targetUId);
        }

        return _userDAO.updateStatus(targetUId, newStatus);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.List<User> listPendingUsers(String operatorUId)
            throws SQLException, IOException, PermissionDeniedException {
        if (operatorUId == null) {
            throw new IllegalArgumentException("操作者ID不能为空");
        }

        User operator = _userDAO.findByUId(operatorUId);
        if (operator == null || !"管理员".equals(operator.getURole())) {
            throw new PermissionDeniedException("无权限执行该操作，仅管理员可查看待审核账号");
        }

        return _userDAO.findByStatus(User.STATUS_PENDING);
    }
}
