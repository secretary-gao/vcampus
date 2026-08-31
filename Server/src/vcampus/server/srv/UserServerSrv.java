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
    public User login(User loginUser) throws SQLException, IOException {
        User found = _userDAO.findByUId(loginUser.getUId());
        if (found == null) {
            return null;
        }
        if (found.getUPwd() == null || !found.getUPwd().equals(loginUser.getUPwd())) {
            return null;
        }
        return found;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean register(User newUser) throws SQLException, IOException, UserExistsException {
        User existing = _userDAO.findByUId(newUser.getUId());
        if (existing != null) {
            throw new UserExistsException("登录ID已被注册：" + newUser.getUId());
        }
        return _userDAO.insert(newUser);
    }
}
