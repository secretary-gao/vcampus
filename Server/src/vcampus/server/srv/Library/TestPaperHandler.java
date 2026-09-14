package vcampus.server.srv.Library;

import vcampus.common.vo.Message;
import vcampus.common.vo.MessageType;
import vcampus.common.vo.Library.Paper;

import java.util.List;

public class TestPaperHandler {
    public static void main(String[] args) {
        PaperHandler handler = new PaperHandler();

        // 测试1：查询所有
        System.out.println("========== 查询所有文献 ==========");
        Message req1 = new Message();
        req1.setUid(System.currentTimeMillis());
        req1.setName("queryPapers");
        req1.setType(MessageType.COMMAND);
        req1.setData("");
        Message resp1 = handler.handle(req1);
        System.out.println("状态码：" + resp1.getStatusCode());
        if ("200".equals(resp1.getStatusCode())) {
            @SuppressWarnings("unchecked")
            List<Paper> papers = (List<Paper>) resp1.getData();
            for (Paper p : papers) {
                System.out.println("  " + p.getTitle() + " | " + p.getAuthor());
            }
        }

        // 测试2：读取 PDF 字节
        System.out.println("\n========== 读取 P001.pdf ==========");
        Message req2 = new Message();
        req2.setUid(System.currentTimeMillis());
        req2.setName("getPdfData");
        req2.setType(MessageType.COMMAND);
        req2.setData("P001.pdf");
        Message resp2 = handler.handle(req2);
        System.out.println("状态码：" + resp2.getStatusCode());
        if ("200".equals(resp2.getStatusCode())) {
            byte[] data = (byte[]) resp2.getData();
            System.out.println("PDF 字节数：" + data.length);
        } else {
            System.out.println("错误：" + resp2.getData());
        }
    }
}