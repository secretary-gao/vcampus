package vcampus.server.dao;

import vcampus.common.vo.HealthArticle;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

public class HealthArticleDAO {

    /** 查询全部健康教育文章，按创建时间倒序 */
    public List<HealthArticle> findAll() throws SQLException,IOException {
        List<HealthArticle> list = new ArrayList<>();
        String sql = "SELECT articleId, title, content, createTime FROM tblHealthArticle ORDER BY createTime DESC, articleId ASC";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                HealthArticle a = new HealthArticle(
                        rs.getString("articleId"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getTimestamp("createTime")
                );
                list.add(a);
            }
        }
        return list;
    }

    /** 按id查询单篇 */
    public HealthArticle findById(String articleId) throws SQLException,IOException {
        String sql = "SELECT articleId, title, content, createTime FROM tblHealthArticle WHERE articleId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, articleId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new HealthArticle(
                            rs.getString("articleId"),
                            rs.getString("title"),
                            rs.getString("content"),
                            rs.getTimestamp("createTime")
                    );
                }
            }
        }
        return null;
    }
}
