package vcampus.client.biz.Library;

import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Library.Paper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

/**
 * 文献库客户端服务实现
 */
public class PaperClientServiceImpl implements IPaperClientService {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    @Override
    @SuppressWarnings("unchecked")
    public List<Paper> queryPapers(String keyword) throws IOException, ClassNotFoundException {
        Message request = new Message();
        request.setUid(System.currentTimeMillis());
        request.setName("queryPapers");
        request.setType(MessageType.COMMAND);
        request.setData(keyword);
        request.setSender("Client");

        Message response = sendAndReceive(request);

        if ("200".equals(response.getStatusCode())) {
            return (List<Paper>) response.getData();
        } else {
            throw new IOException("查询文献失败：" + response.getData());
        }
    }

    @Override
    public byte[] getPdfData(String pdfName) throws IOException, ClassNotFoundException {
        Message request = new Message();
        request.setUid(System.currentTimeMillis());
        request.setName("getPdfData");
        request.setType(MessageType.COMMAND);
        request.setData(pdfName);
        request.setSender("Client");

        Message response = sendAndReceive(request);

        if ("200".equals(response.getStatusCode())) {
            return (byte[]) response.getData();
        } else {
            throw new IOException("获取 PDF 失败：" + response.getData());
        }
    }

    @Override
    public boolean addPaper(Paper paper) throws IOException, ClassNotFoundException {
        Message request = new Message();
        request.setUid(System.currentTimeMillis());
        request.setName("addPaper");
        request.setType(MessageType.COMMAND);
        request.setData(paper);
        request.setSender("admin");

        Message response = sendAndReceive(request);
        return "200".equals(response.getStatusCode());
    }

    @Override
    public boolean deletePaper(String paperId) throws IOException, ClassNotFoundException {
        Message request = new Message();
        request.setUid(System.currentTimeMillis());
        request.setName("deletePaper");
        request.setType(MessageType.COMMAND);
        request.setData(paperId);
        request.setSender("admin");

        Message response = sendAndReceive(request);
        return "200".equals(response.getStatusCode());
    }

    @Override
    public boolean uploadPdf(String pdfName, byte[] fileData) throws IOException, ClassNotFoundException {
        Message request = new Message();
        request.setUid(System.currentTimeMillis());
        request.setName("uploadPdf");
        request.setType(MessageType.COMMAND);

        // data 是一个 Object[]：{pdfName, byte[]}
        request.setData(new Object[]{ pdfName, fileData });
        request.setSender("admin");

        Message response = sendAndReceive(request);
        return "200".equals(response.getStatusCode());
    }

    /**
     * 发送请求并接收响应
     */
    private Message sendAndReceive(Message request) throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(request);
            out.flush();

            return (Message) in.readObject();
        }
    }
}