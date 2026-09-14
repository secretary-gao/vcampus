package vcampus.server.srv.Library;

import vcampus.common.vo.Library.Paper;
import vcampus.server.dao.Library.PaperDAO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;

public class PaperServiceImpl implements IPaperService {

    /** PDF 存放目录（服务器本地） */
    private static final String PDF_DIR = "D:\\School_Info_System\\Papers\\";

    private final PaperDAO paperDAO = new PaperDAO();

    @Override
    public List<Paper> queryPapers(String keyword) throws SQLException, IOException {
        return paperDAO.queryPapers(keyword);
    }

    @Override
    public Paper getPaperById(String paperId) throws SQLException, IOException {
        return paperDAO.getPaperById(paperId);
    }

    @Override
    public boolean addPaper(Paper paper) throws SQLException, IOException {
        return paperDAO.addPaper(paper);
    }

    @Override
    public boolean deletePaper(String paperId) throws SQLException, IOException {
        return paperDAO.deletePaper(paperId);
    }

    @Override
    public byte[] getPdfData(String pdfName) throws IOException {
        File file = new File(PDF_DIR + pdfName);
        if (!file.exists()) {
            throw new IOException("PDF 文件不存在：" + pdfName);
        }
        return Files.readAllBytes(file.toPath());
    }
    
    @Override
    public boolean savePdf(String pdfName, byte[] fileData) throws IOException {
        File file = new File(PDF_DIR + pdfName);
        // 确保目录存在
        file.getParentFile().mkdirs();
        Files.write(file.toPath(), fileData);
        return true;
    }
}