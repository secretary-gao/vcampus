package vcampus.client.view.Library;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import vcampus.common.vo.Library.Paper;

import java.util.Optional;

/**
 * 上传文献时填写元数据的弹窗
 */
public class PaperFormDialog {

    private final Stage stage;
    private Optional<Paper> result = Optional.empty();

    private TextField paperIdField;
    private TextField titleField;
    private TextField authorField;
    private TextField pdfNameField;

    public PaperFormDialog() {
        this.stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("上传文献");
        initUI();
    }

    private void initUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(25, 30, 25, 30));
        root.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("📤 上传文献");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        GridPane form = new GridPane();
        form.setHgap(15);
        form.setVgap(12);

        int row = 0;

        Label idLabel = new Label("文献编号 *");
        paperIdField = new TextField();
        paperIdField.setPromptText("如：P006");
        paperIdField.setPrefWidth(250);
        form.add(idLabel, 0, row);
        form.add(paperIdField, 1, row++);

        Label titleLbl = new Label("标题 *");
        titleField = new TextField();
        titleField.setPromptText("请输入标题");
        form.add(titleLbl, 0, row);
        form.add(titleField, 1, row++);

        Label authorLbl = new Label("作者");
        authorField = new TextField();
        authorField.setPromptText("请输入作者");
        form.add(authorLbl, 0, row);
        form.add(authorField, 1, row++);

        Label pdfLbl = new Label("PDF文件名 *");
        pdfNameField = new TextField();
        pdfNameField.setPromptText("如：P006.pdf");
        form.add(pdfLbl, 0, row);
        form.add(pdfNameField, 1, row++);

        Button confirmBtn = new Button("确认上传");
        confirmBtn.setPrefWidth(100);
        confirmBtn.setStyle(
            "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 6; -fx-cursor: hand;"
        );

        Button cancelBtn = new Button("取消");
        cancelBtn.setPrefWidth(80);
        cancelBtn.setStyle(
            "-fx-background-color: #f5f5f5; -fx-text-fill: #333; " +
            "-fx-background-radius: 6; -fx-cursor: hand;"
        );

        HBox btnBox = new HBox(15, confirmBtn, cancelBtn);
        btnBox.setStyle("-fx-alignment: center-right;");

        root.getChildren().addAll(titleLabel, form, btnBox);

        confirmBtn.setOnAction(e -> handleConfirm());
        cancelBtn.setOnAction(e -> stage.close());

        Scene scene = new Scene(root, 420, 350);
        stage.setScene(scene);
    }

    private void handleConfirm() {
        String paperId = paperIdField.getText().trim();
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String pdfName = pdfNameField.getText().trim();

        if (paperId.isEmpty() || title.isEmpty() || pdfName.isEmpty()) {
            showAlert("文献编号、标题、PDF文件名 不能为空");
            return;
        }

        Paper paper = new Paper();
        paper.setPaperId(paperId);
        paper.setTitle(title);
        paper.setAuthor(author);
        paper.setPdfName(pdfName);

        result = Optional.of(paper);
        stage.close();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("输入错误");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public Optional<Paper> showAndWait() {
        stage.showAndWait();
        return result;
    }
}