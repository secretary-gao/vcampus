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
        String sql = "SELECT uId, uName, uAge, uSex, uPwd, uRole FROM tblUser WHERE uId = ?";

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
        String sql = "INSERT INTO tblUser (uId, uName, uAge, uSex, uPwd, uRole) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

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

            return pstmt.executeUpdate() > 0;
        }
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
        return user;
    }
}
