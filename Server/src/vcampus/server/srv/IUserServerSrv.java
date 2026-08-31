/*
 * IUserServerSrv
 *
 * Version 1.0
 *
 * 2026-08-30
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.srv;

import vcampus.common.vo.User;

import java.io.IOException;
import java.sql.SQLException;

/**
 * 服务器端用户业务服务接口，对应共享说明书中"用户管理模块"的 IUserServerSrv。
 * 由 {@link ServerThread} 调用，屏蔽 {@link vcampus.server.dao.UserDAO} 的具体
 * 数据库细节，只暴露登录、注册两个业务动作。
 */
public interface IUserServerSrv {

    /**
     * 用户登录：根据登录ID查询用户，并校验密码（密码以 MD5 摘要形式比对）。
     *
     * @param loginUser 客户端传来的登录信息（仅需 uId、uPwd 两个字段有效）
     * @return 登录成功时返回数据库中的完整用户信息；登录ID不存在或密码错误
     *         时统一返回 {@code null}（不区分两种失败原因，避免暴露用户名是否存在）
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    User login(User loginUser) throws SQLException, IOException;

    /**
     * 用户注册：登录ID不存在时插入新用户。
     *
     * @param newUser 待注册的用户信息
     * @return 插入成功返回 {@code true}
     * @throws SQLException        数据库操作异常
     * @throws IOException         数据库配置文件读取异常
     * @throws UserExistsException 当登录ID已被注册时抛出
     */
    boolean register(User newUser) throws SQLException, IOException, UserExistsException;
}
