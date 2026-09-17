package vcampus.common.vo;

import java.io.Serializable;
import java.util.Date;

public class HealthArticle implements Serializable {
    private static final long serialVersionUID = 1L;

    private String articleId;
    private String title;
    private String content;
    private Date createTime;

    public HealthArticle() {}

    public HealthArticle(String articleId, String title, String content, Date createTime) {
        this.articleId = articleId;
        this.title = title;
        this.content = content;
        this.createTime = createTime;
    }

    public String getArticleId() { return articleId; }
    public void setArticleId(String articleId) { this.articleId = articleId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
