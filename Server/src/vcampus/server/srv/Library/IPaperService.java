package vcampus.server.srv.Library;

import vcampus.common.vo.Library.Paper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface IPaperService {

    List<Paper> queryPapers(String keyword) throws SQLException, IOException;

    Paper getPaperById(String paperId) throws SQLException, IOException;

    boolean addPaper(Paper paper) throws SQLException, IOException;

    boolean deletePaper(String paperId) throws SQLException, IOException;

    boolean savePdf(String pdfName, byte[] fileData) throws IOException;
    /**
     * 读取 PDF 文件的字节数据
     */
    byte[] getPdfData(String pdfName) throws IOException;
}