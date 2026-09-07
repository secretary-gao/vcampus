package vcampus.client.view.Library;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextAlignment;

import vcampus.client.biz.Library.BookClientServiceImpl;
import vcampus.client.biz.Library.IBookClientService;
import vcampus.common.vo.Library.Book;
import vcampus.common.vo.Library.BorrowRecord;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;

/**
 * 图书馆主面板
 * 
 * 包含图书查询、借书、查看借阅记录功能。
 * 与 BookClientServiceImpl 配合，通过 Socket 与服务器通信。
 */
public class LibraryPanel extends VBox {

    private final IBookClientService bookClient = new BookClientServiceImpl();

    // 当前登录用户（由主窗口传入）
    private String currentUserId = "zhangsan"; // TODO: 从主窗口获取实际登录用户

    // UI 组件
    private TextField searchField;
    private Button searchBtn;
    private TableView<Book> bookTable;
    private TableView<BorrowRecord> recordTable;
    private Button borrowBtn;
    private StackPane contentArea;

    // 数据
    private ObservableList<Book> bookData = FXCollections.observableArrayList();
    private ObservableList<BorrowRecord> recordData = FXCollections.observableArrayList();

    // 标签状态
    private Button libraryTab;
    private Button borrowTab;
    private Button manageTab;

    public LibraryPanel() {
        initUI();
        initEvents();
        // 默认加载所有图书
        doSearch("");
    }

    private void initUI() {
        setSpacing(0);
        setStyle("-fx-background-color: white;");

        // ========== 1. 顶部标题区 ==========
        Label titleLabel = new Label("📚 虚拟图书馆");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setStyle("-fx-text-fill: #1a237e;");

        Label subtitleLabel = new Label("检索馆藏并管理当前账号的借阅记录，借阅后即可在线阅读电子书");
        subtitleLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");

        VBox headerBox = new VBox(4, titleLabel, subtitleLabel);
        headerBox.setPadding(new Insets(20, 30, 15, 30));
        headerBox.setStyle("-fx-background-color: #f5f7fa;");

        // ========== 2. 导航标签 ==========
        libraryTab = createTabButton("📖 图书馆", true);
        borrowTab = createTabButton("📋 我的借阅", false);
        manageTab = createTabButton("⚙️ 图书管理", false);

        HBox tabBox = new HBox(0, libraryTab, borrowTab, manageTab);
        tabBox.setPadding(new Insets(0, 30, 0, 30));
        tabBox.setStyle("-fx-background-color: white;");

        // ========== 3. 搜索区 ==========
        searchField = new TextField();
        searchField.setPromptText("输入书名、作者或ISBN");
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

        searchBtn = new Button("🔍 搜索");
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
        searchBox.setPadding(new Insets(20, 30, 15, 30));
        searchBox.setStyle("-fx-background-color: white;");

        // ========== 4. 内容区域（堆叠两个表格） ==========
        // 4a. 图书表格（默认显示）
        bookTable = new TableView<>();
        bookTable.setPlaceholder(new Label("没有匹配的图书，建议调整关键词试试"));
        setupBookTable(bookTable);
        bookTable.setStyle("-fx-background-radius: 12;");
        VBox.setVgrow(bookTable, Priority.ALWAYS);

        // 4b. 借阅记录表格（默认隐藏）
        recordTable = new TableView<>();
        recordTable.setPlaceholder(new Label("暂无借阅记录"));
        setupRecordTable(recordTable);
        recordTable.setStyle("-fx-background-radius: 12;");
        recordTable.setVisible(false);
        VBox.setVgrow(recordTable, Priority.ALWAYS);

        contentArea = new StackPane();
        contentArea.getChildren().addAll(bookTable, recordTable);
        contentArea.setPadding(new Insets(0, 30, 10, 30));
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // ========== 5. 底部操作按钮 ==========
        borrowBtn = new Button("📌 借阅所选图书");
        borrowBtn.setPrefHeight(40);
        borrowBtn.setPrefWidth(150);
        borrowBtn.setStyle(
            "-fx-background-color: #1a73e8; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 8; " +
            "-fx-cursor: hand;"
        );
        borrowBtn.setDisable(true);

        HBox actionBox = new HBox(borrowBtn);
        actionBox.setPadding(new Insets(0, 30, 20, 30));
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        // ========== 组装 ==========
        this.getChildren().addAll(headerBox, tabBox, searchBox, contentArea, actionBox);
    }

    private void initEvents() {
        // 搜索按钮
        searchBtn.setOnAction(e -> {
            String keyword = searchField.getText().trim();
            doSearch(keyword);
        });

        // 回车搜索
        searchField.setOnAction(e -> {
            String keyword = searchField.getText().trim();
            doSearch(keyword);
        });

        // 图书表格选中监听
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            borrowBtn.setDisable(newVal == null);
        });

        // 借书按钮
        borrowBtn.setOnAction(e -> handleBorrow());

        // 标签切换
        libraryTab.setOnAction(e -> switchTab("library"));
        borrowTab.setOnAction(e -> switchTab("borrow"));
        manageTab.setOnAction(e -> switchTab("manage"));
    }

    // ========== 核心业务方法 ==========

    private void doSearch(String keyword) {
        // 切换到图书标签
        switchTab("library");

        bookData.clear();
        borrowBtn.setDisable(true);
        bookTable.setPlaceholder(new Label("加载中..."));

        new Thread(() -> {
            try {
                List<Book> books = bookClient.queryBooks(keyword);
                Platform.runLater(() -> {
                    bookData.setAll(books);
                    if (books.isEmpty()) {
                        bookTable.setPlaceholder(new Label("没有匹配的图书，建议调整关键词试试"));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    bookTable.setPlaceholder(new Label("查询失败：" + e.getMessage()));
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void handleBorrow() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.getAvailableCount() <= 0) {
            showAlert("库存不足", "《" + selected.getBookName() + "》当前可借数量为 0，暂不可借。");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认借书");
        confirm.setHeaderText("确认借阅《" + selected.getBookName() + "》？");
        confirm.setContentText("作者：" + selected.getAuthor() + "\n可借数量：" + selected.getAvailableCount());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                doBorrow(selected);
            }
        });
    }

    private void doBorrow(Book book) {
        borrowBtn.setDisable(true);
        borrowBtn.setText("借阅中...");

        new Thread(() -> {
            try {
                boolean success = bookClient.borrowBook(currentUserId, book.getBookId());
                Platform.runLater(() -> {
                    if (success) {
                        showAlert("借书成功", "《" + book.getBookName() + "》借阅成功！");
                        doSearch(searchField.getText().trim());
                    } else {
                        showAlert("借书失败", "借阅失败，请稍后重试。");
                    }
                    borrowBtn.setText("📌 借阅所选图书");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("网络异常", "借书请求失败：" + e.getMessage());
                    borrowBtn.setText("📌 借阅所选图书");
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void loadBorrowRecords() {
        recordData.clear();
        recordTable.setPlaceholder(new Label("加载中..."));

        new Thread(() -> {
            try {
                List<BorrowRecord> records = bookClient.getBorrowRecords(currentUserId);
                Platform.runLater(() -> {
                    recordData.setAll(records);
                    if (records.isEmpty()) {
                        recordTable.setPlaceholder(new Label("暂无借阅记录"));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    recordTable.setPlaceholder(new Label("加载失败：" + e.getMessage()));
                });
                e.printStackTrace();
            }
        }).start();
    }
    private void handleReturnBook(BorrowRecord record) {
        // 弹出确认对话框
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认还书");
        confirm.setHeaderText("确认归还《" + record.getBookId() + "》？"); // 这里如果要显示书名，需要额外查询
        confirm.setContentText("记录号：" + record.getRecordId());
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // 在后台线程中执行还书操作
                new Thread(() -> {
                    try {
                        boolean success = bookClient.returnBook(record.getRecordId());
                        Platform.runLater(() -> {
                            if (success) {
                                showAlert("还书成功", "图书已成功归还！");
                                // 刷新“我的借阅”列表
                                loadBorrowRecords();
                                // 刷新图书搜索结果（以便更新“可借数量”）
                                doSearch(searchField.getText().trim());
                            } else {
                                showAlert("还书失败", "归还失败，请稍后重试。");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showAlert("网络异常", "还书请求失败：" + e.getMessage());
                        });
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }
    // ========== UI 切换 ==========

    private void switchTab(String tab) {
        // 更新标签样式
        libraryTab.setStyle(createTabStyle("library".equals(tab)));
        borrowTab.setStyle(createTabStyle("borrow".equals(tab)));
        manageTab.setStyle(createTabStyle("manage".equals(tab)));

        // 切换内容
        switch (tab) {
            case "library":
                bookTable.setVisible(true);
                recordTable.setVisible(false);
                searchField.setVisible(true);
                searchBtn.setVisible(true);
                borrowBtn.setVisible(true);
                break;
            case "borrow":
                bookTable.setVisible(false);
                recordTable.setVisible(true);
                searchField.setVisible(false);
                searchBtn.setVisible(false);
                borrowBtn.setVisible(false);
                loadBorrowRecords();
                break;
            case "manage":
                // 管理员功能，暂不实现
                showAlert("提示", "管理员功能开发中");
                // 切回图书馆
                switchTab("library");
                break;
        }
    }

    private String createTabStyle(boolean active) {
        return "-fx-background-color: transparent; " +
               "-fx-text-fill: " + (active ? "#1a73e8" : "#666666") + "; " +
               "-fx-border-color: transparent transparent " + (active ? "#1a73e8" : "transparent") + " transparent; " +
               "-fx-border-width: 0 0 3 0; " +
               "-fx-padding: 10 20 10 20; " +
               "-fx-cursor: hand;";
    }

    private Button createTabButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.setFont(Font.font("System", FontWeight.BOLD, 14));
        btn.setPrefHeight(50);
        btn.setStyle(createTabStyle(active));
        return btn;
    }

    // ========== 表格设置 ==========

    @SuppressWarnings("unchecked")
    private void setupBookTable(TableView<Book> table) {
        table.setItems(bookData);

        TableColumn<Book, String> idCol = new TableColumn<>("书号");
        idCol.setCellFactory(col -> createCenterCell());
        idCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        idCol.setPrefWidth(120);

        TableColumn<Book, String> nameCol = new TableColumn<>("书名");
        nameCol.setCellFactory(col -> createCenterCell());
        nameCol.setCellValueFactory(new PropertyValueFactory<>("bookName"));
        nameCol.setPrefWidth(300);

        TableColumn<Book, String> authorCol = new TableColumn<>("作者");
        authorCol.setCellFactory(col -> createCenterCell());
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorCol.setPrefWidth(150);

        TableColumn<Book, String> categoryCol = new TableColumn<>("分类");
        categoryCol.setCellFactory(col -> createCenterCell());
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(120);

        TableColumn<Book, Integer> availCol = new TableColumn<>("可借数量");
        availCol.setCellFactory(col -> createIntegerCenterCell());
        availCol.setCellValueFactory(new PropertyValueFactory<>("availableCount"));
        availCol.setPrefWidth(100);

        // 双击借书
        table.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Book selected = table.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getAvailableCount() > 0) {
                    handleBorrow();
                }
            }
        });
        
        table.getColumns().addAll(idCol, nameCol, authorCol, categoryCol, availCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().forEach(col -> col.setReorderable(false));
    }

    @SuppressWarnings("unchecked")
    private void setupRecordTable(TableView<BorrowRecord> table) {
        table.setItems(recordData);

        TableColumn<BorrowRecord, String> idCol = new TableColumn<>("记录号");
        idCol.setCellFactory(col -> createCenterCell());
        idCol.setCellValueFactory(new PropertyValueFactory<>("recordId"));
        idCol.setPrefWidth(150);

        TableColumn<BorrowRecord, String> bookIdCol = new TableColumn<>("图书编号");
        bookIdCol.setCellFactory(col -> createCenterCell());
        bookIdCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        bookIdCol.setPrefWidth(120);

        TableColumn<BorrowRecord, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);
        statusCol.setCellFactory(col -> createCenterCell());

        // 借阅日期列
        TableColumn<BorrowRecord, Date> borrowDateCol = new TableColumn<>("借阅日期");
        borrowDateCol.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        borrowDateCol.setPrefWidth(150);
        borrowDateCol.setCellFactory(col -> new TableCell<BorrowRecord, Date>() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-alignment: center;");
                } else {
                    setText(sdf.format(item));
                    setStyle("-fx-alignment: center;");
                }
            }
        });

        // 应还日期列（同样处理）
        TableColumn<BorrowRecord, Date> dueDateCol = new TableColumn<>("应还日期");
        dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        dueDateCol.setPrefWidth(150);
        dueDateCol.setCellFactory(col -> new TableCell<BorrowRecord, Date>() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("-fx-alignment: center;");
                    setText(null);
                } else {
                    setStyle("-fx-alignment: center;");
                    setText(sdf.format(item));
                }
            }
        });

        // 状态用颜色标记
        statusCol.setCellFactory(col -> new TableCell<BorrowRecord, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    if ("借阅中".equals(item)) {
                        setStyle("-fx-text-fill: #1a73e8; -fx-font-weight: bold; -fx-alignment: center;");
                    } else if ("已逾期".equals(item)) {
                        setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold; -fx-alignment: center;");
                    } else {
                        setStyle("-fx-text-fill: #388e3c; -fx-alignment: center;");
                    }
                }
            }
        });
        // 新增“操作”列，用于放置“归还”按钮
        TableColumn<BorrowRecord, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(100);
        
        // 设置该列如何为每行数据生成单元格
        actionCol.setCellFactory(col -> new TableCell<BorrowRecord, Void>() {
            private final Button returnBtn = new Button("归还");

            {
                // 1. 设置按钮样式
                returnBtn.setStyle(
                    "-fx-background-color: #4CAF50; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-weight: bold; " +
                    "-fx-background-radius: 4; " +
                    "-fx-cursor: hand;"
                );
                // 2. 设置按钮点击事件
                returnBtn.setOnAction(event -> {
                    // 获取当前行的借阅记录
                    BorrowRecord record = getTableView().getItems().get(getIndex());
                    // 调用处理还书的方法
                    handleReturnBook(record);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                // 获取当前行的记录
                BorrowRecord record = getTableView().getItems().get(getIndex());
                // 只有状态为“借阅中”时才显示“归还”按钮，否则显示为空
                if ("借阅中".equals(record.getStatus())) {
                    setGraphic(returnBtn);
                    // ← 关键：设置单元格内容和按钮居中
                    setAlignment(Pos.CENTER);
                    setStyle("-fx-alignment: center;");
                } else {
                    setGraphic(null);
                }
            }
        });

        // 将新创建的操作列添加到表格中
        table.getColumns().addAll(idCol, bookIdCol, statusCol, borrowDateCol, dueDateCol);
        table.getColumns().add(actionCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().forEach(col -> col.setReorderable(false));
    }

    // ========== 工具方法 ==========

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
        if (userId != null && !userId.isEmpty()) {
            doSearch("");
        }
    }
    private <T> TableCell<T, String> createCenterCell() {
        return new TableCell<T, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: center;");
                }
            }
        };
    }
    private <T> TableCell<T, Integer> createIntegerCenterCell() {
        return new TableCell<T, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-alignment: center;");
                }
            }
        };
    }

    private <T> TableCell<T, Void> createActionCenterCell() {
        return new TableCell<T, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                // 操作列由 setCellFactory 自己处理，这里只设置样式
                setStyle("-fx-alignment: center;");
            }
        };
    }
    // ========== 测试入口 ==========
    public static void main(String[] args) {
        javafx.application.Application.launch(TestApp.class);
    }

    public static class TestApp extends javafx.application.Application {
        @Override
        public void start(javafx.stage.Stage stage) {
            LibraryPanel panel = new LibraryPanel();
            panel.setCurrentUserId("zhangsan");
            javafx.scene.Scene scene = new javafx.scene.Scene(panel, 950, 650);
            stage.setTitle("图书馆模块测试");
            stage.setScene(scene);
            stage.show();
        }
    }
}