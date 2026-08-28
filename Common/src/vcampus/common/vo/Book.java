package vcampus.common.vo;

import java.io.Serializable;

public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bookId;
    private String bookName;
    private String author;
    private String isbn;
    private String category;
    private int totalCount;
    private int availableCount;

    // 无参构造（反序列化需要）
    public Book() {}

    // 全参构造（方便使用）
    public Book(String bookId, String bookName, String author, String isbn,
                String category, int totalCount, int availableCount) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.totalCount = totalCount;
        this.availableCount = availableCount;
    }

    // ------ getter / setter ------
    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getAvailableCount() {
        return availableCount;
    }

    public void setAvailableCount(int availableCount) {
        this.availableCount = availableCount;
    }

    @Override
    public String toString() {
        return "Book{" +
                "bookId='" + bookId + '\'' +
                ", bookName='" + bookName + '\'' +
                ", author='" + author + '\'' +
                ", isbn='" + isbn + '\'' +
                ", category='" + category + '\'' +
                ", totalCount=" + totalCount +
                ", availableCount=" + availableCount +
                '}';
    }
}