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

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * 文献库面板（所有用户可见）
 * 
 * 提供文献列表查询、预览、下载功能。
 */
public class PaperPanel extends VBox {

    private final IPaperClientService paperClient = new PaperClientServiceImpl();

    private TextField searchField;
    private Button searchBtn;
    private TableView<Paper> paperTable;
    private ObservableList<Paper> paperData = FXCollections.observableArrayList();
    private Button previewBtn;
    private Button downloadBtn;

    public PaperPanel() {
        initUI();
        initEvents();
        loadPapers("");
    }

    public void refresh() {
        loadPapers(searchField.getText().trim());
    }
    private void initUI() {
        setSpacing(0);
        setStyle("-fx-background-color: white;");
        setPadding(new Insets(0, 30, 10, 30));

        // 搜索区
        searchField = new TextField();
        searchField.setPromptText("输入标题或作者");
        searchField.setPrefHeight(40);
        searchField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ddd;");

        searchBtn = new Button("🔍 搜索");
        searchBtn.setPrefHeight(40);
        searchBtn.setPrefWidth(100);
        searchBtn.setStyle(
            "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        );

        HBox searchBox = new HBox(12, searchField, searchBtn);
        searchBox.setPadding(new Insets(10, 0, 15, 0));

        // 表格
        paperTable = new TableView<>();
        paperTable.setPlaceholder(new Label("暂无文献"));
        setupPaperTable(paperTable);
        VBox.setVgrow(paperTable, Priority.ALWAYS);

        // 操作按钮
        previewBtn = new Button("📖 预览所选文献");
        previewBtn.setPrefHeight(40);
        previewBtn.setPrefWidth(160);
        previewBtn.setStyle(
            "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        );
        previewBtn.setDisable(true);

        downloadBtn = new Button("💾 下载所选文献");
        downloadBtn.setPrefHeight(40);
        downloadBtn.setPrefWidth(160);
        downloadBtn.setStyle(
            "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        );
        downloadBtn.setDisable(true);

        HBox actionBox = new HBox(15, previewBtn, downloadBtn);
        actionBox.setPadding(new Insets(10, 0, 0, 0));
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        this.getChildren().addAll(searchBox, paperTable, actionBox);
    }

    private void initEvents() {
        searchBtn.setOnAction(e -> loadPapers(searchField.getText().trim()));
        searchField.setOnAction(e -> loadPapers(searchField.getText().trim()));

        paperTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean has = n != null;
            previewBtn.setDisable(!has);
            downloadBtn.setDisable(!has);
        });

        previewBtn.setOnAction(e -> handlePreview());
        downloadBtn.setOnAction(e -> handleDownload());
    }

    private void loadPapers(String keyword) {
        paperData.clear();
        paperTable.setPlaceholder(new Label("加载中..."));

        new Thread(() -> {
            try {
                List<Paper> papers = paperClient.queryPapers(keyword);
                Platform.runLater(() -> {
                    paperData.setAll(papers);
                    if (papers.isEmpty()) {
                        paperTable.setPlaceholder(new Label("没有匹配的文献"));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> paperTable.setPlaceholder(new Label("加载失败：" + e.getMessage())));
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 预览：下载到临时文件，用系统默认程序打开
     */
    private void handlePreview() {
        Paper selected = paperTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        previewBtn.setDisable(true);
        previewBtn.setText("加载中...");

        new Thread(() -> {
            try {
                byte[] data = paperClient.getPdfData(selected.getPdfName());

                // 保存到临时文件
                File tempFile = File.createTempFile("preview_", ".pdf");
                tempFile.deleteOnExit();
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(data);
                }

                // 用系统默认程序打开
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(tempFile);
                }

                Platform.runLater(() -> {
                    previewBtn.setText("📖 预览所选文献");
                    previewBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("预览失败", e.getMessage());
                    previewBtn.setText("📖 预览所选文献");
                    previewBtn.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 下载：弹出保存对话框，用户选择保存位置
     */
    private void handleDownload() {
        Paper selected = paperTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存文献");
        fileChooser.setInitialFileName(selected.getTitle() + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF 文件", "*.pdf")
        );

        File saveFile = fileChooser.showSaveDialog(this.getScene().getWindow());
        if (saveFile == null) return;

        downloadBtn.setDisable(true);
        downloadBtn.setText("下载中...");

        new Thread(() -> {
            try {
                byte[] data = paperClient.getPdfData(selected.getPdfName());
                Files.write(saveFile.toPath(), data);

                Platform.runLater(() -> {
                    showAlert("下载成功", "文件已保存到：\n" + saveFile.getAbsolutePath());
                    downloadBtn.setText("💾 下载所选文献");
                    downloadBtn.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("下载失败", e.getMessage());
                    downloadBtn.setText("💾 下载所选文献");
                    downloadBtn.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void setupPaperTable(TableView<Paper> table) {
        table.setItems(paperData);

        TableColumn<Paper, String> titleCol = new TableColumn<>("标题");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(450);
        titleCol.setStyle("-fx-alignment: center;");

        TableColumn<Paper, String> authorCol = new TableColumn<>("作者");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorCol.setPrefWidth(200);
        authorCol.setStyle("-fx-alignment: center;");

        table.getColumns().addAll(titleCol, authorCol);
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
}