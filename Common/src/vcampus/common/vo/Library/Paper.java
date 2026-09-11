package vcampus.common.vo.Library;

import java.io.Serializable;

public class Paper implements Serializable {
    private static final long serialVersionUID = 1L;

    private String paperId;
    private String title;
    private String author;
    private String pdfName;

    public Paper() {}

    public Paper(String paperId, String title, String author, String pdfName) {
        this.paperId = paperId;
        this.title = title;
        this.author = author;
        this.pdfName = pdfName;
    }

    public String getPaperId() { return paperId; }
    public void setPaperId(String paperId) { this.paperId = paperId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPdfName() { return pdfName; }
    public void setPdfName(String pdfName) { this.pdfName = pdfName; }

    @Override
    public String toString() {
        return "Paper{" +
                "paperId='" + paperId + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", pdfName='" + pdfName + '\'' +
                '}';
    }
}