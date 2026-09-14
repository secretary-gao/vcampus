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
import javafx.stage.FileChooser;
import vcampus.client.biz.Library.IPaperClientService;
import vcampus.client.biz.Library.PaperClientServiceImpl;
import vcampus.common.vo.Library.Paper;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

/**
 * 文献管理面板（仅管理员可见）
 * 
 * 提供文献上传、删除功能。
 */
public class PaperManagePanel extends VBox {

    private final IPaperClientService paperClient = new PaperClientServiceImpl();

    private TableView<Paper> paperTable;
    private ObservableList<Paper> paperData = FXCollections.observableArrayList();
    private Button uploadBtn;
    private Button deleteBtn;
    private Button refreshBtn;

    public PaperManagePanel() {
        initUI();
        initEvents();
        loadPapers();
    }

    private void initUI() {
        setSpacing(10);
        setPadding(new Insets(0, 30, 10, 30));
        setStyle("-fx-background-color: white;");

        // 操作按钮区
        uploadBtn = createActionButton("📤 上传文献", "#4CAF50");
        deleteBtn = createActionButton("🗑️ 删除文献", "#f44336");
        refreshBtn = createActionButton("🔄 刷新", "#607D8B");
        deleteBtn.setDisable(true);

        HBox actionBox = new HBox(10, uploadBtn, deleteBtn, refreshBtn);
        actionBox.setPadding(new Insets(10, 0, 10, 0));
        actionBox.setAlignment(Pos.CENTER_LEFT);

        // 表格
        paperTable = new TableView<>();
        paperTable.setPlaceholder(new Label("暂无文献数据"));
        setupPaperTable(paperTable);
        VBox.setVgrow(paperTable, Priority.ALWAYS);

        this.getChildren().addAll(actionBox, paperTable);
    }

    private void initEvents() {
        uploadBtn.setOnAction(e -> handleUpload());
        deleteBtn.setOnAction(e -> handleDelete());
        refreshBtn.setOnAction(e -> loadPapers());
    }

    private void loadPapers() {
        paperData.clear();
        paperTable.setPlaceholder(new Label("加载中..."));

        new Thread(() -> {
            try {
                List<Paper> papers = paperClient.queryPapers("");
                Platform.runLater(() -> {
                    paperData.setAll(papers);
                    if (papers.isEmpty()) {
                        paperTable.setPlaceholder(new Label("暂无文献数据"));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> paperTable.setPlaceholder(new Label("加载失败：" + e.getMessage())));
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 上传文献：先填写元数据，再选择本地 PDF 文件
     */
    private void handleUpload() {
        PaperFormDialog dialog = new PaperFormDialog();
        dialog.showAndWait().ifPresent(paper -> {
            // 选择本地 PDF 文件
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择 PDF 文件");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF 文件", "*.pdf")
            );
            File pdfFile = fileChooser.showOpenDialog(this.getScene().getWindow());
            if (pdfFile == null) return;

            new Thread(() -> {
                try {
                    // 1. 上传 PDF 文件到服务器
                    byte[] fileData = Files.readAllBytes(pdfFile.toPath());
                    boolean pdfOk = paperClient.uploadPdf(paper.getPdfName(), fileData);

                    if (!pdfOk) {
                        Platform.runLater(() -> showAlert("失败", "PDF 上传失败"));
                        return;
                    }

                    // 2. 写入元数据
                    boolean metaOk = paperClient.addPaper(paper);

                    Platform.runLater(() -> {
                        if (metaOk) {
                            showAlert("成功", "文献上传成功！");
                            loadPapers();
                        } else {
                            showAlert("失败", "文献元数据写入失败，请检查 paperId 是否已存在");
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert("异常", "上传失败：" + e.getMessage()));
                    e.printStackTrace();
                }
            }).start();
        });
    }

    private void handleDelete() {
        Paper selected = paperTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("确认删除《" + selected.getTitle() + "》？");
        confirm.setContentText("此操作不可恢复！");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        boolean success = paperClient.deletePaper(selected.getPaperId());
                        Platform.runLater(() -> {
                            if (success) {
                                showAlert("成功", "文献删除成功！");
                                loadPapers();
                            } else {
                                showAlert("失败", "文献删除失败");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("异常", "删除失败：" + e.getMessage()));
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

    private void setupPaperTable(TableView<Paper> table) {
        table.setItems(paperData);

        TableColumn<Paper, String> idCol = new TableColumn<>("编号");
        idCol.setCellValueFactory(new PropertyValueFactory<>("paperId"));
        idCol.setPrefWidth(100);
        idCol.setStyle("-fx-alignment: center;");

        TableColumn<Paper, String> titleCol = new TableColumn<>("标题");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(350);

        TableColumn<Paper, String> authorCol = new TableColumn<>("作者");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorCol.setPrefWidth(150);

        TableColumn<Paper, String> pdfNameCol = new TableColumn<>("PDF文件名");
        pdfNameCol.setCellValueFactory(new PropertyValueFactory<>("pdfName"));
        pdfNameCol.setPrefWidth(150);

        table.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            deleteBtn.setDisable(n == null);
        });

        table.getColumns().addAll(idCol, titleCol, authorCol, pdfNameCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().forEach(col -> col.setReorderable(false));
    }

    private Button createActionButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefHeight(40);
        btn.setStyle(
            "-fx-background-color: " + color + "; " +
            "-fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 6; -fx-cursor: hand;"
        );
        return btn;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}