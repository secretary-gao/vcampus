package vcampus.client.biz.Library;

import vcampus.common.vo.Library.Paper;

import java.util.List;

public class TestPaperClient {
    public static void main(String[] args) {
        IPaperClientService client = new PaperClientServiceImpl();

        try {
            // 测试1：查询所有文献
            System.out.println("========== 查询所有文献 ==========");
            List<Paper> papers = client.queryPapers("");
            for (Paper p : papers) {
                System.out.println(p.getPaperId() + " | " + p.getTitle() + " | " + p.getAuthor());
            }

            // 测试2：获取 PDF 字节
            System.out.println("\n========== 获取 P001.pdf ==========");
            byte[] data = client.getPdfData("P001.pdf");
            System.out.println("PDF 字节数：" + data.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}