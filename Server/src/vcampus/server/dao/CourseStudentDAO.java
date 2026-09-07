/*
 * CourseStudentDAO
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.server.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 为选课模块提供登录用户 ID 到正式学号的只读映射。 */
public class CourseStudentDAO {

    /**
     * 按用户账号查询对应学号。
     *
     * @param userId 登录用户 ID
     * @return 对应学号；未建立学籍时返回 {@code null}
     * @throws SQLException 数据库操作失败
     * @throws IOException  数据库配置读取失败
     */
    public String findStudentIdByUserId(String userId) throws SQLException, IOException {
        String sql = "SELECT studentId FROM tblStudent WHERE userId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getString("studentId") : null;
            }
        }
    }
}
