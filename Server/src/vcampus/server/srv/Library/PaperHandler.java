package vcampus.server.srv.Library;

import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Library.Paper;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class PaperHandler {

    private final IPaperService paperService = new PaperServiceImpl();

    public Message handle(Message request) {
        String name = request.getName();

        try {
            switch (name) {
                case "queryPapers":
                    return handleQueryPapers(request);
                case "getPdfData":
                    return handleGetPdfData(request);
                case "addPaper":
                    return handleAddPaper(request);
                case "deletePaper":
                    return handleDeletePaper(request);
                case "uploadPdf":
                    return handleUploadPdf(request);
                default:
                    return new Message(request.getUid(), name, MessageType.DATA,
                            "400", "未知的文献库操作：" + name, "Server");
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return new Message(request.getUid(), name, MessageType.DATA,
                    "500", "服务器内部异常：" + e.getMessage(), "Server");
        }
    }

    private Message handleQueryPapers(Message request) throws SQLException, IOException {
        String keyword = (String) request.getData();
        List<Paper> papers = paperService.queryPapers(keyword);
        return new Message(request.getUid(), "queryPapers", MessageType.DATA,
                "200", papers, "Server");
    }

    private Message handleGetPdfData(Message request) throws SQLException, IOException {
        String pdfName = (String) request.getData();
        byte[] data = paperService.getPdfData(pdfName);
        return new Message(request.getUid(), "getPdfData", MessageType.DATA,
                "200", data, "Server");
    }

    private Message handleAddPaper(Message request) throws SQLException, IOException {
        Paper paper = (Paper) request.getData();
        boolean success = paperService.addPaper(paper);
        return new Message(request.getUid(), "addPaper", MessageType.DATA,
                success ? "200" : "400",
                success ? "文献添加成功" : "文献添加失败", "Server");
    }

    private Message handleDeletePaper(Message request) throws SQLException, IOException {
        String paperId = (String) request.getData();
        boolean success = paperService.deletePaper(paperId);
        return new Message(request.getUid(), "deletePaper", MessageType.DATA,
                success ? "200" : "400",
                success ? "文献删除成功" : "文献删除失败", "Server");
    }

    private Message handleUploadPdf(Message request) throws IOException {
        Object[] arr = (Object[]) request.getData();
        String pdfName = (String) arr[0];
        byte[] fileData = (byte[]) arr[1];

        boolean success = paperService.savePdf(pdfName, fileData);
        return new Message(request.getUid(), "uploadPdf", MessageType.DATA,
                success ? "200" : "400",
                success ? "PDF 上传成功" : "PDF 上传失败", "Server");
    }
}