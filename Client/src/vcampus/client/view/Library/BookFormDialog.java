package vcampus.client.view.Library;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import vcampus.common.vo.Library.Book;

import java.util.Optional;

/**
 * 新增/修改图书表单弹窗
 */
public class BookFormDialog {

    private final Stage stage;
    private final Book book;
    private Optional<Book> result = Optional.empty();

    private TextField bookIdField;
    private TextField bookNameField;
    private TextField authorField;
    private TextField isbnField;
    private TextField categoryField;
    private TextField totalCountField;
    private TextField availableCountField;

    public BookFormDialog(Book existingBook) {
        this.book = existingBook;
        this.stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(existingBook == null ? "新增图书" : "修改图书");

        initUI();
    }

    private void initUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(25, 30, 25, 30));
        root.setStyle("-fx-background-color: white;");

        // 标题
        Label titleLabel = new Label(book == null ? "📖 新增图书" : "✏️ 修改图书");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        // 表单
        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(12);

        int row = 0;

        // 书号
        Label bookIdLabel = new Label("书号 *");
        bookIdLabel.setStyle("-fx-font-weight: bold;");
        bookIdField = new TextField();
        bookIdField.setPrefWidth(250);
        bookIdField.setPromptText("如：B006");
        bookIdField.setDisable(book != null);  // 修改时书号不可改
        if (book != null) {
            bookIdField.setText(book.getBookId());
        }
        form.add(bookIdLabel, 0, row);
        form.add(bookIdField, 1, row);

        row++;

        // 书名
        Label bookNameLabel = new Label("书名 *");
        bookNameLabel.setStyle("-fx-font-weight: bold;");
        bookNameField = new TextField();
        bookNameField.setPromptText("请输入书名");
        if (book != null) {
            bookNameField.setText(book.getBookName());
        }
        form.add(bookNameLabel, 0, row);
        form.add(bookNameField, 1, row);

        row++;

        // 作者
        Label authorLabel = new Label("作者");
        authorField = new TextField();
        authorField.setPromptText("请输入作者");
        if (book != null) {
            authorField.setText(book.getAuthor());
        }
        form.add(authorLabel, 0, row);
        form.add(authorField, 1, row);

        row++;

        // ISBN
        Label isbnLabel = new Label("ISBN");
        isbnField = new TextField();
        isbnField.setPromptText("请输入ISBN号");
        if (book != null) {
            isbnField.setText(book.getIsbn());
        }
        form.add(isbnLabel, 0, row);
        form.add(isbnField, 1, row);

        row++;

        // 分类
        Label categoryLabel = new Label("分类");
        categoryField = new TextField();
        categoryField.setPromptText("请输入分类");
        if (book != null) {
            categoryField.setText(book.getCategory());
        }
        form.add(categoryLabel, 0, row);
        form.add(categoryField, 1, row);

        row++;

        // 馆藏总数
        Label totalLabel = new Label("馆藏总数 *");
        totalLabel.setStyle("-fx-font-weight: bold;");
        totalCountField = new TextField();
        totalCountField.setPromptText("请输入总数");
        if (book != null) {
            totalCountField.setText(String.valueOf(book.getTotalCount()));
        }
        form.add(totalLabel, 0, row);
        form.add(totalCountField, 1, row);

        row++;

        // 可借数量
        Label availLabel = new Label("可借数量 *");
        availLabel.setStyle("-fx-font-weight: bold;");
        availableCountField = new TextField();
        availableCountField.setPromptText("请输入可借数量");
        if (book != null) {
            availableCountField.setText(String.valueOf(book.getAvailableCount()));
        }
        form.add(availLabel, 0, row);
        form.add(availableCountField, 1, row);

        // 按钮
        Button confirmBtn = new Button(book == null ? "确认新增" : "确认修改");
        confirmBtn.setPrefWidth(100);
        confirmBtn.setStyle(
            "-fx-background-color: #1a73e8; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-background-radius: 6; " +
            "-fx-cursor: hand;"
        );

        Button cancelBtn = new Button("取消");
        cancelBtn.setPrefWidth(80);
        cancelBtn.setStyle(
            "-fx-background-color: #f5f5f5; " +
            "-fx-text-fill: #333; " +
            "-fx-background-radius: 6; " +
            "-fx-cursor: hand;"
        );

        HBox btnBox = new HBox(15, confirmBtn, cancelBtn);
        btnBox.setStyle("-fx-alignment: center-right;");

        // 组装
        root.getChildren().addAll(titleLabel, form, btnBox);

        // 事件
        confirmBtn.setOnAction(e -> handleConfirm());
        cancelBtn.setOnAction(e -> stage.close());

        Scene scene = new Scene(root, 420, 500);
        stage.setScene(scene);
    }

    private void handleConfirm() {
        try {
            String bookId = bookIdField.getText().trim();
            String bookName = bookNameField.getText().trim();
            String author = authorField.getText().trim();
            String isbn = isbnField.getText().trim();
            String category = categoryField.getText().trim();
            String totalStr = totalCountField.getText().trim();
            String availStr = availableCountField.getText().trim();

            // 必填校验
            if (bookId.isEmpty()) {
                showAlert("书号不能为空");
                return;
            }
            if (bookName.isEmpty()) {
                showAlert("书名不能为空");
                return;
            }
            if (totalStr.isEmpty()) {
                showAlert("馆藏总数不能为空");
                return;
            }
            if (availStr.isEmpty()) {
                showAlert("可借数量不能为空");
                return;
            }

            int totalCount = Integer.parseInt(totalStr);
            int availableCount = Integer.parseInt(availStr);

            if (totalCount < 0 || availableCount < 0) {
                showAlert("数量不能为负数");
                return;
            }
            if (availableCount > totalCount) {
                showAlert("可借数量不能大于馆藏总数");
                return;
            }

            Book resultBook = new Book();
            resultBook.setBookId(bookId);
            resultBook.setBookName(bookName);
            resultBook.setAuthor(author);
            resultBook.setIsbn(isbn);
            resultBook.setCategory(category);
            resultBook.setTotalCount(totalCount);
            resultBook.setAvailableCount(availableCount);

            result = Optional.of(resultBook);
            stage.close();

        } catch (NumberFormatException e) {
            showAlert("请输入有效的数字");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("输入错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Optional<Book> showAndWait() {
        stage.showAndWait();
        return result;
    }
}