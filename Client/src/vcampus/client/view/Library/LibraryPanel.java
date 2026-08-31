package vcampus.client.view.Library;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class LibraryPanel extends VBox {

    // 模拟数据（等接上后端后替换成真实数据）
    private final ObservableList<Book> bookData = FXCollections.observableArrayList();

    public LibraryPanel() {
        // ========== 1. 顶部标题区 ==========
        Label titleLabel = new Label("📚 虚拟图书馆");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #1a237e;");

        Label subtitleLabel = new Label("检索馆藏并管理当前账号的借阅记录，借阅后即可在线阅读电子书");
        subtitleLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");

        VBox headerBox = new VBox(4, titleLabel, subtitleLabel);
        headerBox.setPadding(new Insets(20, 30, 15, 30));
        headerBox.setStyle("-fx-background-color: #f5f7fa;");
        headerBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(headerBox, Priority.ALWAYS);

        // ========== 2. 导航标签 ==========
        Button libraryTab = createTabButton("📖 图书馆", true);
        Button borrowTab = createTabButton("📋 我的借阅", false);
        Button manageTab = createTabButton("⚙️ 图书管理", false);

        HBox tabBox = new HBox(0, libraryTab, borrowTab, manageTab);
        tabBox.setPadding(new Insets(0, 30, 0, 30));
        tabBox.setStyle("-fx-background-color: white;");
        tabBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tabBox, Priority.ALWAYS);

        // ========== 3. 搜索区 ==========
        TextField searchField = new TextField();
        searchField.setPromptText("输入书名、作者或ISBN");
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

        Button searchBtn = new Button("🔍 搜索");
        searchBtn.setPrefHeight(40);
        searchBtn.setPrefWidth(100);
        searchBtn.setStyle(
            "-fx-background-color: #1a73e8; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );

        HBox searchBox = new HBox(12, searchField, searchBtn);
        searchBox.setPadding(new Insets(20, 30, 20, 30));
        searchBox.setStyle("-fx-background-color: white;");
        searchBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        // ========== 4. 结果展示区（空状态） ==========
        Label emptyIcon = new Label("📖");
        emptyIcon.setFont(Font.font(48));

        Label emptyTitle = new Label("没有匹配的图书");
        emptyTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        emptyTitle.setStyle("-fx-text-fill: #333;");

        Label emptyHint = new Label("建议调整关键词试试");
        emptyHint.setStyle("-fx-text-fill: #999; -fx-font-size: 13px;");

        VBox emptyBox = new VBox(10, emptyIcon, emptyTitle, emptyHint);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60, 0, 60, 0));
        emptyBox.setStyle("-fx-background-color: #fafafa; -fx-background-radius: 12;");
        emptyBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(emptyBox, Priority.ALWAYS);

        ScrollPane resultScroll = new ScrollPane(emptyBox);
        resultScroll.setFitToWidth(true);
        resultScroll.setPadding(new Insets(0, 30, 20, 30));
        resultScroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        resultScroll.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(resultScroll, Priority.ALWAYS);

        // ========== 5. 底部操作按钮 ==========
        Button actionBtn = new Button("📌 选择图书");
        actionBtn.setPrefHeight(40);
        actionBtn.setPrefWidth(120);
        actionBtn.setStyle(
            "-fx-background-color: #1a73e8; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );
        // 无书可选时禁用（等有数据后启用）
        actionBtn.setDisable(true);

        HBox actionBox = new HBox(actionBtn);
        actionBox.setPadding(new Insets(0, 30, 20, 30));
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(actionBox, Priority.ALWAYS);

        // ========== 6. 组装 ==========
        this.getChildren().addAll(headerBox, tabBox, searchBox, resultScroll, actionBox);
        this.setSpacing(0);
        this.setStyle("-fx-background-color: white;");

        // ========== 7. 事件绑定 ==========
        searchBtn.setOnAction(e -> {
            String keyword = searchField.getText().trim();
            if (!keyword.isEmpty()) {
                System.out.println("搜索关键词：" + keyword);
                // TODO: 调用 BookClientService.queryBooks(keyword)
                // 然后把结果放到 bookData 里，刷新 TableView
                // 先用模拟数据演示
                showSearchResult();
            }
        });
    }

    /**
     * 创建导航标签按钮
     */
    private Button createTabButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setFont(Font.font("System", FontWeight.BOLD, 14));
        btn.setPrefHeight(50);
        btn.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: " + (active ? "#1a73e8" : "#666666") + "; " +
            "-fx-border-color: transparent transparent " + (active ? "#1a73e8" : "transparent") + " transparent; " +
            "-fx-border-width: 0 0 3 0; " +
            "-fx-padding: 10 20 10 20; " +
            "-fx-cursor: hand;"
        );
        return btn;
    }

    /**
     * 模拟搜索演示（接上后端后删除）
     */
    private void showSearchResult() {
        // TODO: 替换成真实数据
        System.out.println("搜索完成，显示结果");
        // 这里后面换成 TableView 显示图书列表
    }

    // ========== 临时 Book 类（等 Common/vo 里的 Book 可用后删除） ==========
    public static class Book {
        public String bookId;
        public String bookName;
        public String author;
        public Book(String id, String name, String author) {
            this.bookId = id;
            this.bookName = name;
            this.author = author;
        }
    }
    public static void main(String[] args) {
        javafx.application.Application.launch(TestApp.class);
    }

    public static class TestApp extends javafx.application.Application {
        @Override
        public void start(javafx.stage.Stage stage) {
            Scene scene = new Scene(new LibraryPanel(), 900, 650);
            stage.setScene(scene);
            stage.show();
        }
    }
}