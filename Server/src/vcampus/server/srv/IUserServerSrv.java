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
import java.util.List;

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
     * @throws SQLException                  数据库操作异常
     * @throws IOException                   数据库配置文件读取异常
     * @throws UserDisabledException         当账号已被管理员禁用时抛出
     * @throws UserPendingApprovalException  当账号还在等待管理员审核时抛出
     */
    User login(User loginUser)
            throws SQLException, IOException, UserDisabledException, UserPendingApprovalException;

    /**
     * 用户注册：登录ID不存在时插入新用户。新注册的账号状态强制设为
     * {@link User#STATUS_PENDING}（待审核），不管客户端传来的 uStatus
     * 是什么——必须由管理员在"账号管理"里手动审核通过后才能登录。
     *
     * @param newUser 待注册的用户信息
     * @return 插入成功返回 {@code true}
     * @throws SQLException        数据库操作异常
     * @throws IOException         数据库配置文件读取异常
     * @throws UserExistsException 当登录ID已被注册时抛出
     */
    boolean register(User newUser) throws SQLException, IOException, UserExistsException;

    /**
     * 用户登出：当前版本只做协议闭环和业务占位，不涉及数据库持久化状态。
     *
     * @param currentUser 当前登录用户
     * @return 登出成功返回 {@code true}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    boolean logout(User currentUser) throws SQLException, IOException;

    /**
     * 禁用或启用指定用户的账号，仅管理员可操作（对应说明书"管理员可注销/
     * 禁用账号"）。操作者的身份由服务器根据 {@code operatorUId} 重新查库
     * 确认真实角色，不信任客户端传来的角色信息。
     *
     * @param operatorUId 发起操作的登录ID（必须是管理员）
     * @param targetUId   被操作的目标用户登录ID
     * @param newStatus   新状态，取值见 {@link User#STATUS_NORMAL}/
     *                    {@link User#STATUS_DISABLED}
     * @return 更新成功返回 {@code true}
     * @throws SQLException               数据库操作异常
     * @throws IOException                数据库配置文件读取异常
     * @throws PermissionDeniedException  操作者不是管理员或操作者账号不存在时抛出
     */
    boolean setUserStatus(String operatorUId, String targetUId, String newStatus)
            throws SQLException, IOException, PermissionDeniedException;

    /**
     * 查询全部"待审核"账号，仅管理员可操作，供"账号管理"页面展示待审核
     * 列表。审核通过直接复用 {@link #setUserStatus} 把状态改成
     * {@link User#STATUS_NORMAL} 即可，不需要单独的"审核通过"方法。
     *
     * @param operatorUId 发起查询的登录ID（必须是管理员）
     * @return 全部待审核账号列表
     * @throws SQLException               数据库操作异常
     * @throws IOException                数据库配置文件读取异常
     * @throws PermissionDeniedException  操作者不是管理员或操作者账号不存在时抛出
     */
    List<User> listPendingUsers(String operatorUId)
            throws SQLException, IOException, PermissionDeniedException;

    /** 将指定学生账号密码重置为 123456，仅管理员可操作。 */
    boolean resetStudentPassword(String operatorUId, String targetUId)
            throws SQLException, IOException, PermissionDeniedException;
}
