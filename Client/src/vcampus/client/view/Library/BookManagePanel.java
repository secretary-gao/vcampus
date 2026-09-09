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
import vcampus.client.biz.Library.BookClientServiceImpl;
import vcampus.client.biz.Library.IBookClientService;
import vcampus.common.vo.Library.Book;
import vcampus.common.vo.Library.BorrowRecord;

import java.util.List;

/**
 * 图书管理面板（管理员专用）
 * 
 * 提供图书的增删改查功能。
 * 只有管理员角色的用户才能看到此面板。
 */
public class BookManagePanel extends VBox {

    private final IBookClientService bookClient = new BookClientServiceImpl();

    private TableView<Book> bookTable;
    private ObservableList<Book> bookData = FXCollections.observableArrayList();

    private Button addBtn;
    private Button editBtn;
    private Button deleteBtn;
    private Button refreshBtn;
    private Button searchBtn;
    private TextField searchField;

    public BookManagePanel() {
        initUI();
        initEvents();
        loadBooks();
    }

    private void initUI() {
        searchBtn = new Button("🔍 搜索");
        setSpacing(0);
        //setPadding(new Insets(5, 30, 20, 30));
        //setStyle("-fx-background-color: white;");

        // ========== 顶部标题 ==========
        //Label titleLabel = new Label("📚 图书管理");
        //titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        // ========== 搜索区 ==========
        searchField = new TextField();
        searchField.setPromptText("输入书名、作者或ISBN");
        searchField.setPrefHeight(35);
        searchField.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #ddd;");

        searchBtn.setPrefHeight(35);
        searchBtn.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");

        HBox searchBox = new HBox(10, searchField, searchBtn);
        searchBox.setPadding(new Insets(10, 0, 10, 0));

        // ========== 操作按钮区 ==========
        addBtn = createActionButton("➕ 新增", "#4CAF50");
        editBtn = createActionButton("✏️ 修改", "#FF9800");
        deleteBtn = createActionButton("🗑️ 删除", "#f44336");
        refreshBtn = createActionButton("🔄 刷新", "#607D8B");

        editBtn.setDisable(true);
        deleteBtn.setDisable(true);

        HBox actionBox = new HBox(10, addBtn, editBtn, deleteBtn, refreshBtn);
        actionBox.setPadding(new Insets(5, 0, 10, 0));

        // ========== 图书表格 ==========
        bookTable = new TableView<>();
        bookTable.setPlaceholder(new Label("暂无图书数据"));
        setupBookTable(bookTable);
        VBox.setVgrow(bookTable, Priority.ALWAYS);

        // ========== 组装 ==========
        this.getChildren().addAll(searchBox, actionBox, bookTable);
    }

    private void initEvents() {

        searchBtn.setOnAction(e -> loadBooks(searchField.getText().trim()));
        searchField.setOnAction(e -> loadBooks(searchField.getText().trim()));
        addBtn.setOnAction(e -> handleAdd());
            
        // 修改
        editBtn.setOnAction(e -> handleEdit());
        
        // 删除
        deleteBtn.setOnAction(e -> handleDelete());
        
        // 刷新
        refreshBtn.setOnAction(e -> loadBooks(searchField.getText().trim()));
        
        // 搜索
    }

    // ========== 数据加载 ==========

    private void loadBooks() {
        loadBooks("");
    }

    private void loadBooks(String keyword) {
        bookData.clear();
        bookTable.setPlaceholder(new Label("加载中..."));

        new Thread(() -> {
            try {
                List<Book> books = bookClient.queryBooks(keyword);
                Platform.runLater(() -> {
                    bookData.setAll(books);
                    if (books.isEmpty()) {
                        bookTable.setPlaceholder(new Label("没有匹配的图书"));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    bookTable.setPlaceholder(new Label("加载失败：" + e.getMessage()));
                });
                e.printStackTrace();
            }
        }).start();
    }

    // ========== 业务操作 ==========

    private void handleAdd() {
        BookFormDialog dialog = new BookFormDialog(null);
        dialog.showAndWait().ifPresent(book -> {
            new Thread(() -> {
                try {
                    boolean success = bookClient.addBook(book);
                    Platform.runLater(() -> {
                        if (success) {
                            showAlert("成功", "图书添加成功！");
                            loadBooks(searchField.getText().trim());
                        } else {
                            showAlert("失败", "图书添加失败，请检查书号是否已存在。");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert("异常", "添加失败：" + e.getMessage());
                    });
                    e.printStackTrace();
                }
            }).start();
        });
    }

    private void handleEdit() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        BookFormDialog dialog = new BookFormDialog(selected);
        dialog.showAndWait().ifPresent(updatedBook -> {
            new Thread(() -> {
                try {
                    boolean success = bookClient.updateBook(updatedBook);
                    Platform.runLater(() -> {
                        if (success) {
                            showAlert("成功", "图书修改成功！");
                            loadBooks(searchField.getText().trim());
                        } else {
                            showAlert("失败", "图书修改失败，请检查书号是否存在。");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        showAlert("异常", "修改失败：" + e.getMessage());
                    });
                    e.printStackTrace();
                }
            }).start();
        });
    }

    private void handleDelete() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("确认删除《" + selected.getBookName() + "》？");
        confirm.setContentText("书号：" + selected.getBookId() + "\n此操作不可恢复！");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        boolean success = bookClient.deleteBook(selected.getBookId());
                        Platform.runLater(() -> {
                            if (success) {
                                showAlert("成功", "图书删除成功！");
                                loadBooks(searchField.getText().trim());
                            } else {
                                showAlert("失败", "图书删除失败，请检查书号是否存在。");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            showAlert("异常", "删除失败：" + e.getMessage());
                        });
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    // ========== UI 辅助 ==========

    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefHeight(35);
        btn.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6; " +
            "-fx-cursor: hand;"
        );
        return btn;
    }

    private void setupBookTable(TableView<Book> table) {
        table.setItems(bookData);

        TableColumn<Book, String> idCol = new TableColumn<>("书号");
        idCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        idCol.setPrefWidth(120);
        idCol.setStyle("-fx-alignment: center;");

        TableColumn<Book, String> nameCol = new TableColumn<>("书名");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("bookName"));
        nameCol.setPrefWidth(200);

        TableColumn<Book, String> authorCol = new TableColumn<>("作者");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorCol.setPrefWidth(120);

        TableColumn<Book, String> isbnCol = new TableColumn<>("ISBN");
        isbnCol.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        isbnCol.setPrefWidth(150);

        TableColumn<Book, String> categoryCol = new TableColumn<>("分类");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(100);

        TableColumn<Book, Integer> totalCol = new TableColumn<>("馆藏总数");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalCount"));
        totalCol.setPrefWidth(90);
        totalCol.setStyle("-fx-alignment: center;");

        TableColumn<Book, Integer> availCol = new TableColumn<>("可借数量");
        availCol.setCellValueFactory(new PropertyValueFactory<>("availableCount"));
        availCol.setPrefWidth(90);
        availCol.setStyle("-fx-alignment: center;");

        // 表格选中监听
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasSelection = newVal != null;
            editBtn.setDisable(!hasSelection);
            deleteBtn.setDisable(!hasSelection);
        });

        table.getColumns().addAll(idCol, nameCol, authorCol, isbnCol, categoryCol, totalCol, availCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().forEach(col -> col.setReorderable(false));
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // 重新绑定事件（修正 initUI 里的搜索按钮）
    public void initEventsFixed() {
        // 因为上面的 initUI 里 searchBtn 是局部变量，用这个方法重新绑定
        // 或者直接重构 initUI 把 searchBtn 改成成员变量
    }
}