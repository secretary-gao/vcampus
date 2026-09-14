package vcampus.server.dao.Library;

import vcampus.common.vo.Library.Paper;
import vcampus.server.dao.DbHelper;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaperDAO {

    /**
     * 按关键词查询文献（标题/作者模糊匹配）
     */
    public List<Paper> queryPapers(String keyword) throws SQLException, IOException {
        List<Paper> papers = new ArrayList<>();
        String sql = "SELECT * FROM tblPaper WHERE title LIKE ? OR author LIKE ?";
        String likeKeyword = "%" + keyword + "%";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, likeKeyword);
            pstmt.setString(2, likeKeyword);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    papers.add(mapResultSetToPaper(rs));
                }
            }
        }
        return papers;
    }

    /**
     * 根据 paperId 查询单条文献
     */
    public Paper getPaperById(String paperId) throws SQLException, IOException {
        String sql = "SELECT * FROM tblPaper WHERE paperId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, paperId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPaper(rs);
                }
            }
        }
        return null;
    }

    /**
     * 新增文献（管理员）
     */
    public boolean addPaper(Paper paper) throws SQLException, IOException {
        String sql = "INSERT INTO tblPaper (paperId, title, author, pdfName) VALUES (?, ?, ?, ?)";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, paper.getPaperId());
            pstmt.setString(2, paper.getTitle());
            pstmt.setString(3, paper.getAuthor());
            pstmt.setString(4, paper.getPdfName());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * 删除文献（管理员）
     */
    public boolean deletePaper(String paperId) throws SQLException, IOException {
        String sql = "DELETE FROM tblPaper WHERE paperId = ?";

        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, paperId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * ResultSet → Paper 对象映射
     */
    private Paper mapResultSetToPaper(ResultSet rs) throws SQLException {
        Paper paper = new Paper();
        paper.setPaperId(rs.getString("paperId"));
        paper.setTitle(rs.getString("title"));
        paper.setAuthor(rs.getString("author"));
        paper.setPdfName(rs.getString("pdfName"));
        return paper;
    }
}