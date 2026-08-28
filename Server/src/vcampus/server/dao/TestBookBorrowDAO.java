package vcampus.server.dao;

import vcampus.common.vo.Book;
import vcampus.common.vo.BorrowRecord;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class TestBookBorrowDAO {
    public static void main(String[] args) {
        BookDAO bookDAO = new BookDAO();
        BorrowRecordDAO borrowDAO = new BorrowRecordDAO();

        try {
            System.out.println("========== 1. 查询所有图书 ==========");
            List<Book> books = bookDAO.queryBooks("");
            for (Book b : books) {
                System.out.println(b.getBookId() + " | " + b.getBookName() + " | 可借: " + b.getAvailableCount());
            }

            System.out.println("\n========== 2. 查询'Java'相关图书 ==========");
            books = bookDAO.queryBooks("Java");
            for (Book b : books) {
                System.out.println(b.getBookId() + " | " + b.getBookName() + " | " + b.getAuthor());
            }

            System.out.println("\n========== 3. 根据ID查询B001 ==========");
            Book book = bookDAO.getBookById("B001");
            if (book != null) {
                System.out.println("书名: " + book.getBookName() + ", 可借: " + book.getAvailableCount());
            }

            System.out.println("\n========== 4. 新增一本测试图书 ==========");
            Book newBook = new Book();
            newBook.setBookId("B999");
            newBook.setBookName("测试图书");
            newBook.setAuthor("测试作者");
            newBook.setIsbn("999-9-999-99999-9");
            newBook.setCategory("测试");
            newBook.setTotalCount(10);
            newBook.setAvailableCount(5);
            boolean added = bookDAO.addBook(newBook);
            System.out.println(added ? "✅ 新增成功" : "❌ 新增失败");

            System.out.println("\n========== 5. 验证新增的图书 ==========");
            Book addedBook = bookDAO.getBookById("B999");
            if (addedBook != null) {
                System.out.println("书名: " + addedBook.getBookName() + ", 可借: " + addedBook.getAvailableCount());
            }

            System.out.println("\n========== 6. 修改图书信息 ==========");
            if (addedBook != null) {
                addedBook.setBookName("测试图书-已修改");
                addedBook.setAvailableCount(8);
                boolean updated = bookDAO.updateBook(addedBook);
                System.out.println(updated ? "✅ 修改成功" : "❌ 修改失败");
                // 验证修改
                Book checked = bookDAO.getBookById("B999");
                System.out.println("修改后书名: " + checked.getBookName() + ", 可借: " + checked.getAvailableCount());
            }

            System.out.println("\n========== 7. 查询所有借阅记录 ==========");
            List<BorrowRecord> records = borrowDAO.getAllRecords();
            for (BorrowRecord r : records) {
                System.out.println(r.getRecordId() + " | " + r.getUserId() + " | " + r.getStatus());
            }

            System.out.println("\n========== 8. 查询zhangsan的借阅记录 ==========");
            records = borrowDAO.getRecordsByUserId("zhangsan");
            for (BorrowRecord r : records) {
                System.out.println(r.getRecordId() + " | " + r.getBookId() + " | " + r.getStatus());
            }

            System.out.println("\n========== 9. 测试借书（先查询B001当前可借数） ==========");
            Book b001 = bookDAO.getBookById("B001");
            System.out.println("B001 当前可借: " + b001.getAvailableCount());

            // 模拟借书：扣库存
            boolean decreased = bookDAO.decreaseAvailableCount("B001");
            System.out.println("扣库存" + (decreased ? "成功" : "失败（库存不足）"));

            // 查一下新的可借数
            b001 = bookDAO.getBookById("B001");
            System.out.println("B001 扣减后可借: " + b001.getAvailableCount());

            System.out.println("\n========== 10. 测试还书（恢复库存） ==========");
            boolean increased = bookDAO.increaseAvailableCount("B001");
            System.out.println("恢复库存" + (increased ? "成功" : "失败"));
            b001 = bookDAO.getBookById("B001");
            System.out.println("B001 恢复后可借: " + b001.getAvailableCount());

            System.out.println("\n========== 11. 删除测试图书 ==========");
            boolean deleted = bookDAO.deleteBook("B999");
            System.out.println(deleted ? "✅ 删除成功" : "❌ 删除失败");
            // 验证删除
            Book checkDeleted = bookDAO.getBookById("B999");
            System.out.println(checkDeleted == null ? "✅ B999 已不存在" : "❌ B999 还在");

            System.out.println("\n========== ✅ 所有测试完成 ==========");

        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}