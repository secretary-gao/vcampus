/*
 * UserDAO
 *
 * Version 1.0
 *
 * 2026-08-28
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import vcampus.common.vo.User;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 用户表（tblUser）的数据访问类，封装对用户表的查询与写入操作。
 * 对上层业务服务层屏蔽具体的 SQL 语句与数据库细节，连接统一由
 * {@link DbHelper} 提供。
 */
public class UserDAO {

    /**
     * 根据登录ID查询用户（用于登录验证）。
     *
     * @param uId 登录ID
     * @return 查询到的用户对象；若不存在则返回 {@code null}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public User findByUId(String uId) throws SQLException, IOException {
        String sql = "SELECT uId, uName, uAge, uSex, uPwd, uRole, uStatus FROM tblUser WHERE uId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * 插入一个新用户（用于注册）。
     *
     * @param user 待插入的用户对象
     * @return 插入成功返回 {@code true}，否则返回 {@code false}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean insert(User user) throws SQLException, IOException {
        String sql = "INSERT INTO tblUser (uId, uName, uAge, uSex, uPwd, uRole, uStatus) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUId());
            pstmt.setString(2, user.getUName());
            if (user.getUAge() == null) {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            } else {
                pstmt.setInt(3, user.getUAge());
            }
            pstmt.setString(4, user.getUSex());
            pstmt.setString(5, user.getUPwd());
            pstmt.setString(6, user.getURole());
            pstmt.setString(7, user.getUStatus());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 按账号状态查询用户列表（用于管理员查看"待审核"账号列表）。
     *
     * @param status 账号状态，见 {@link User#STATUS_NORMAL}/
     *               {@link User#STATUS_DISABLED}/{@link User#STATUS_PENDING}
     * @return 该状态下的全部用户，按登录ID排序
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public java.util.List<User> findByStatus(String status) throws SQLException, IOException {
        String sql = "SELECT uId, uName, uAge, uSex, uPwd, uRole, uStatus FROM tblUser "
                + "WHERE uStatus = ? ORDER BY uId";
        java.util.List<User> result = new java.util.ArrayList<>();

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    /**
     * 将结果集当前行映射为 User 对象。
     *
     * @param rs 指向当前行的结果集
     * @return 映射后的 User 对象
     * @throws SQLException 读取结果集时发生异常
     */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUId(rs.getString("uId"));
        user.setUName(rs.getString("uName"));

        int age = rs.getInt("uAge");
        user.setUAge(rs.wasNull() ? null : age);

        user.setUSex(rs.getString("uSex"));
        user.setUPwd(rs.getString("uPwd"));
        user.setURole(rs.getString("uRole"));
        user.setUStatus(rs.getString("uStatus"));
        return user;
    }

    /**
     * 更新指定用户的账号状态（正常/禁用），供管理员禁用/启用账号使用。
     *
     * @param uId    目标用户的登录ID
     * @param status 新状态，取值见 {@link User#STATUS_NORMAL}/{@link User#STATUS_DISABLED}
     * @return 更新成功（该用户存在且被更新）返回 {@code true}
     * @throws SQLException 数据库操作异常
     * @throws IOException  数据库配置文件读取异常
     */
    public boolean updateStatus(String uId, String status) throws SQLException, IOException {
        String sql = "UPDATE tblUser SET uStatus = ? WHERE uId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setString(2, uId);

            return pstmt.executeUpdate() > 0;
        }
    }
}
