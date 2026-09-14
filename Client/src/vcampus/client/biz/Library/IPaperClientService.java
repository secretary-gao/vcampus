package vcampus.client.biz.Library;

import vcampus.common.vo.Library.Paper;

import java.io.IOException;
import java.util.List;

/**
 * 文献库客户端服务接口
 */
public interface IPaperClientService {

    /**
     * 按关键词查询文献
     */
    List<Paper> queryPapers(String keyword) throws IOException, ClassNotFoundException;

    /**
     * 获取 PDF 字节数据
     */
    byte[] getPdfData(String pdfName) throws IOException, ClassNotFoundException;

    /**
     * 上传文献（管理员）
     */
    boolean addPaper(Paper paper) throws IOException, ClassNotFoundException;

    /**
     * 删除文献（管理员）
     */
    boolean deletePaper(String paperId) throws IOException, ClassNotFoundException;

    /**
     * 上传 PDF 文件到服务器
     * @param pdfName 服务器上的文件名（如 P001.pdf）
     * @param fileData PDF 字节数据
     * @return 是否成功
     */
    boolean uploadPdf(String pdfName, byte[] fileData) throws IOException, ClassNotFoundException;
}