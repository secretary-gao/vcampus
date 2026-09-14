package vcampus.server.dao.Library;

import vcampus.common.vo.Library.Paper;

import java.util.List;

public class TestPaperDAO {
    public static void main(String[] args) {
        PaperDAO dao = new PaperDAO();

        try {
            // 测试1：查询所有
            System.out.println("========== 查询所有文献 ==========");
            List<Paper> all = dao.queryPapers("");
            for (Paper p : all) {
                System.out.println(p.getPaperId() + " | " + p.getTitle() + " | " + p.getAuthor() + " | " + p.getPdfName());
            }

            // 测试2：按关键词查询
            System.out.println("\n========== 查询 'Java' ==========");
            List<Paper> javaPapers = dao.queryPapers("Java");
            for (Paper p : javaPapers) {
                System.out.println(p.getPaperId() + " | " + p.getTitle());
            }

            // 测试3：根据 id 查询
            System.out.println("\n========== 根据 id 查询 P001 ==========");
            Paper p = dao.getPaperById("P001");
            if (p != null) {
                System.out.println("标题：" + p.getTitle());
                System.out.println("作者：" + p.getAuthor());
                System.out.println("PDF文件名：" + p.getPdfName());
            } else {
                System.out.println("未找到 P001");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}