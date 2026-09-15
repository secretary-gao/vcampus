package vcampus.client.view;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.stage.FileChooser;
import vcampus.client.biz.StudentClientException;
import vcampus.client.biz.StudentClientSrv;
import vcampus.client.biz.IUserClientSrv;
import vcampus.client.biz.UserClientSrv;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Message;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentCampusOverview;
import vcampus.common.vo.StudentStatus;
import vcampus.common.vo.User;

import java.net.URL;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** 学生学籍管理 JavaFX 界面。 */
public class StudentManagementFrame extends Application {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String STYLE_FILE =
            "Client/src/vcampus/client/view/student-management.css";

    private final User _currentUser;
    private final StudentClientSrv _studentClientSrv;
    private final IUserClientSrv _userClientSrv = new UserClientSrv();
    private final ObservableList<Student> _students = FXCollections.observableArrayList();
    private final TableView<Student> _table = new TableView<>(_students);

    private final TextField _studentIdField = new TextField();
    private final TextField _campusCardField = new TextField();
    private final TextField _userIdField = new TextField();
    private final TextField _nameField = new TextField();
    private final TextField _classNameField = new TextField();
    private final TextField _majorField = new TextField();
    private final TextField _gradeField = new TextField();
    private final DatePicker _enrollmentDatePicker = new DatePicker();
    private final ComboBox<StudentStatus> _statusBox = new ComboBox<>();
    private final ComboBox<String> _queryTypeBox = new ComboBox<>();
    private final ComboBox<String> _statusFilterBox = new ComboBox<>();
    private final TextField _queryField = new TextField();

    private final Label _countLabel = new Label("共 0 条");
    private final Label _totalStatLabel = new Label("0");
    private final Label _enrolledStatLabel = new Label("0");
    private final Label _suspendedStatLabel = new Label("0");
    private final Label _graduatedStatLabel = new Label("0");
    private final Label _withdrawnStatLabel = new Label("0");
    private final Label _breakdownStatLabel = new Label("年级/专业分布：暂无");
    private Label _statisticsDialogSubtitle;
    private final Map<javafx.scene.chart.Chart, Label> _chartEmptyLabels = new HashMap<>();
    private final PieChart _statusChart = new PieChart();
    private final BarChart<String, Number> _gradeChart = new BarChart<>(
            new CategoryAxis(), new NumberAxis());
    private final BarChart<String, Number> _majorChart = new BarChart<>(
            new CategoryAxis(), new NumberAxis());
    private final Label _editorModeLabel = new Label("新建学生");
    private final Label _statusLabel = new Label("就绪");
    private final Label _formErrorLabel = new Label();
    private final ProgressIndicator _progressIndicator = new ProgressIndicator();
    private final Button _addButton = new Button("新增档案");
    private final Button _updateButton = new Button("保存修改");
    private final Button _deleteButton = new Button("删除记录");
    private final Button _resetPasswordButton = new Button("重置密码");
    private final Button _undoButton = new Button("撤回上一步");
    private final List<Button> _operationButtons = new ArrayList<>();
    private final Map<TextField, String> _fieldErrors = new LinkedHashMap<>();
    private final Map<String, Label> _detailValues = new LinkedHashMap<>();
    private boolean _operationRunning;
    private BorderPane _root;
    private Dialog<ButtonType> _statisticsDialog;
    private String _undoDescription;
    private CheckedSupplier<Void> _undoOperation;
    private Consumer<Void> _undoSuccess;

    public StudentManagementFrame() {
        this(null);
    }

    public StudentManagementFrame(User currentUser) {
        this._currentUser = currentUser;
        this._studentClientSrv = new StudentClientSrv(currentUser);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Vcampus 学生学籍管理");
        BorderPane root = createView();

        double availableWidth = Screen.getPrimary().getVisualBounds().getWidth();
        double availableHeight = Screen.getPrimary().getVisualBounds().getHeight();
        double initialWidth = Math.min(1220, availableWidth - 48);
        double initialHeight = Math.min(760, availableHeight - 48);
        Scene scene = new Scene(root, initialWidth, initialHeight);
        attachStyleSheet(scene);
        stage.setMinWidth(Math.min(980, initialWidth));
        stage.setMinHeight(Math.min(650, initialHeight));
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
        refresh();
    }

    /**
     * 创建可嵌入主界面的学籍管理面板。
     *
     * @return 学籍管理根面板
     */
    public BorderPane createView() {
        if (_root != null) {
            return _root;
        }
        configureFields();
        configureTable();

        _root = new BorderPane();
        _root.getStyleClass().add("app-root");
        _root.setTop(createHeader());
        _root.setCenter(createWorkspace());
        _root.setBottom(createStatusBar());
        return _root;
    }

    /**
     * 刷新学生列表，供主界面嵌入后调用。
     */
    public void refresh() {
        if (isStudent()) {
            refreshMyStudentInfo();
        } else {
            refreshStudents();
        }
    }

    private VBox createHeader() {
        Label title = new Label(isStudent() ? "我的学籍" : "学生学籍管理");
        title.getStyleClass().add("page-title");

        Label roleLabel = new Label(roleText());
        roleLabel.getStyleClass().add("role-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        _countLabel.getStyleClass().add("count-label");
        HBox titleRow = new HBox(12, title, roleLabel, spacer, _countLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getStyleClass().add("title-row");

        if (isStudent()) {
            Label tip = new Label("已根据当前登录账号自动读取本人学籍信息");
            tip.getStyleClass().add("student-self-hint");
            return new VBox(titleRow, tip);
        }

        _queryTypeBox.getItems().addAll("学号", "姓名");
        if (isAdmin() || isTeacher()) {
            _queryTypeBox.getItems().add(1, "一卡通号");
        }
        _queryTypeBox.setValue("学号");
        _queryTypeBox.setPrefWidth(120);
        _queryTypeBox.getStyleClass().add("query-type");

        _statusFilterBox.getItems().setAll("全部状态", "在读", "休学", "毕业", "退学");
        _statusFilterBox.setValue("全部状态");
        _statusFilterBox.setPrefWidth(110);
        _statusFilterBox.getStyleClass().add("query-type");

        updateQueryPrompt();
        _queryTypeBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> updateQueryPrompt());
        _statusFilterBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> queryStudents(
                        _queryTypeBox.getValue(), _queryField.getText()));
        _queryField.setPrefWidth(300);
        _queryField.setAccessibleHelp("可按学号、一卡通号或姓名包含关键词查询");
        HBox.setHgrow(_queryField, Priority.ALWAYS);

        Button queryButton = new Button("查询");
        queryButton.getStyleClass().addAll("button", "primary-button");
        queryButton.setOnAction(event ->
                queryStudents(_queryTypeBox.getValue(), _queryField.getText()));

        Button resetButton = new Button(isTeacher() ? "我的学生" : "显示全部");
        resetButton.getStyleClass().addAll("button", "secondary-button");
        resetButton.setOnAction(event -> {
            _queryField.clear();
            _statusFilterBox.setValue("全部状态");
            refreshStudents();
        });

        Button refreshButton = new Button("刷新");
        refreshButton.getStyleClass().addAll("button", "quiet-button");
        refreshButton.setTooltip(new Tooltip("重新读取数据库中的学生信息"));
        refreshButton.setOnAction(event -> refreshStudents());

        Button exportButton = new Button("导出 CSV");
        exportButton.getStyleClass().addAll("button", "quiet-button");
        exportButton.setTooltip(new Tooltip("导出当前筛选结果；教师可见学号和一卡通号，不含账号"));
        exportButton.setOnAction(event -> exportStudents());

        Button importButton = new Button("批量导入");
        importButton.getStyleClass().addAll("button", "quiet-button");
        importButton.setTooltip(new Tooltip("导入管理员导出的完整学籍 CSV"));
        importButton.setOnAction(event -> importStudents());

        _queryField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !_operationRunning) {
                queryStudents(_queryTypeBox.getValue(), _queryField.getText());
            }
        });
        _operationButtons.addAll(List.of(queryButton, resetButton, refreshButton, exportButton));
        if (isAdmin()) {
            _operationButtons.add(importButton);
        }

        HBox searchBar = isAdmin()
                ? new HBox(8, _queryTypeBox, _queryField, _statusFilterBox,
                queryButton, resetButton, refreshButton, exportButton, importButton)
                : new HBox(8, _queryTypeBox, _queryField, _statusFilterBox,
                queryButton, resetButton, refreshButton, exportButton);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.getStyleClass().add("search-bar");
        return new VBox(titleRow, searchBar);
    }

    private Node createWorkspace() {
        if (isStudent()) {
            return createStudentDetail();
        }
        VBox tableSection = createTableSection();
        if (!isAdmin()) {
            VBox workspace = new VBox(tableSection);
            workspace.getStyleClass().add("workspace");
            VBox.setVgrow(tableSection, Priority.ALWAYS);
            return workspace;
        }
        ScrollPane editor = new ScrollPane(createEditor());
        editor.setFitToWidth(true);
        editor.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        editor.getStyleClass().add("editor-scroll");
        editor.setMinWidth(300);

        SplitPane workspace = new SplitPane(tableSection, editor);
        workspace.getStyleClass().add("workspace");
        workspace.setDividerPositions(0.66);
        SplitPane.setResizableWithParent(editor, false);
        return workspace;
    }

    private VBox createStudentDetail() {
        Label sectionTitle = new Label("本人学籍档案");
        sectionTitle.getStyleClass().add("section-title");
        Label sectionHint = new Label("该页面仅供查看，如信息有误请联系管理员处理");
        sectionHint.getStyleClass().add("section-hint");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("detail-grid");
        grid.setHgap(18);
        grid.setVgap(10);
        addDetailRow(grid, 0, "学号", "studentId");
        addDetailRow(grid, 1, "一卡通号", "campusCardNo");
        addDetailRow(grid, 2, "姓名", "name");
        addDetailRow(grid, 3, "班级", "className");
        addDetailRow(grid, 4, "专业", "major");
        addDetailRow(grid, 5, "年级", "grade");
        addDetailRow(grid, 6, "入学日期", "enrollmentDate");
        addDetailRow(grid, 7, "学籍状态", "status");

        VBox detail = new VBox(18, new VBox(3, sectionTitle, sectionHint), grid);
        detail.getStyleClass().addAll("workspace", "student-detail");
        return detail;
    }

    private void addDetailRow(GridPane grid, int row, String title, String key) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("detail-label");
        Label valueLabel = new Label("正在读取...");
        valueLabel.getStyleClass().add("detail-value");
        valueLabel.setWrapText(true);
        _detailValues.put(key, valueLabel);
        grid.add(titleLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private VBox createTableSection() {
        Label sectionTitle = new Label("学生列表");
        sectionTitle.getStyleClass().add("section-title");
        Label sectionHint = new Label(isAdmin()
                ? "点击列标题排序；列表展示关键信息，选中学生后可在右侧查看并修改完整档案"
                : "点击列标题排序；仅显示自己任教课程的学生及其学号、一卡通号和公开学籍字段");
        sectionHint.getStyleClass().add("section-hint");
        VBox heading = new VBox(2, sectionTitle, sectionHint);
        heading.getStyleClass().add("section-heading");

        VBox section = new VBox(heading, _table);
        section.getStyleClass().add("table-section");
        VBox.setVgrow(_table, Priority.ALWAYS);
        return section;
    }

    private VBox createStatisticsPanel() {
        Label title = new Label("学籍统计图");
        title.getStyleClass().add("statistics-dialog-title");
        Label subtitle = new Label("统计范围：当前学生列表（已应用搜索和状态筛选）");
        subtitle.getStyleClass().add("statistics-dialog-subtitle");
        _statisticsDialogSubtitle = subtitle;
        FlowPane summary = new FlowPane(8, 8,
                statisticCard("当前结果", _totalStatLabel, "stat-total"),
                statisticCard("在读", _enrolledStatLabel, "stat-enrolled"),
                statisticCard("休学", _suspendedStatLabel, "stat-suspended"),
                statisticCard("毕业", _graduatedStatLabel, "stat-muted"),
                statisticCard("退学", _withdrawnStatLabel, "stat-muted"));
        summary.setPrefWrapLength(900);
        summary.getStyleClass().add("statistics-summary");
        _breakdownStatLabel.getStyleClass().add("stat-breakdown");
        _breakdownStatLabel.setWrapText(true);

        configureStatisticsCharts();
        GridPane charts = new GridPane();
        charts.setHgap(12);
        charts.setVgap(12);
        ColumnConstraints chartLeft = new ColumnConstraints();
        chartLeft.setPercentWidth(50);
        chartLeft.setHgrow(Priority.ALWAYS);
        chartLeft.setFillWidth(true);
        ColumnConstraints chartRight = new ColumnConstraints();
        chartRight.setPercentWidth(50);
        chartRight.setHgrow(Priority.ALWAYS);
        chartRight.setFillWidth(true);
        charts.getColumnConstraints().addAll(chartLeft, chartRight);
        VBox statusCard = chartCard("学籍状态", _statusChart);
        VBox gradeCard = chartCard("年级分布", _gradeChart);
        VBox majorCard = chartCard("专业排行", _majorChart);
        charts.add(statusCard, 0, 0);
        charts.add(gradeCard, 1, 0);
        charts.add(majorCard, 0, 1, 2, 1);
        for (Node card : List.of(statusCard, gradeCard, majorCard)) {
            GridPane.setHgrow(card, Priority.ALWAYS);
            GridPane.setFillWidth(card, true);
        }
        charts.getStyleClass().add("statistics-charts");
        VBox content = new VBox(10, new VBox(2, title, subtitle), summary, charts,
                _breakdownStatLabel);
        content.getStyleClass().addAll("statistics-bar", "statistics-dialog-content");
        return content;
    }

    private void showStatisticsDialog() {
        if (_statisticsDialog == null) {
            _statisticsDialog = new Dialog<>();
            _statisticsDialog.setTitle("学籍统计图");
            _statisticsDialog.setHeaderText(null);
            _statisticsDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            ScrollPane statisticsScroll = new ScrollPane(createStatisticsPanel());
            statisticsScroll.setFitToWidth(true);
            statisticsScroll.setFitToHeight(true);
            statisticsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            statisticsScroll.getStyleClass().add("statistics-scroll");
            _statisticsDialog.getDialogPane().setContent(statisticsScroll);
            _statisticsDialog.getDialogPane().getStyleClass().addAll("app-root", "statistics-dialog");
            _statisticsDialog.getDialogPane().setMinWidth(980);
            _statisticsDialog.getDialogPane().setPrefWidth(1080);
            _statisticsDialog.getDialogPane().setMinHeight(620);
            _statisticsDialog.getDialogPane().setPrefHeight(720);
            _statisticsDialog.setOnShown(event -> attachStyleSheet(
                    _statisticsDialog.getDialogPane().getScene()));
            if (_root != null && _root.getScene() != null) {
                _statisticsDialog.initOwner(_root.getScene().getWindow());
            }
        }
        updateStudentStatistics();
        _statisticsDialog.showAndWait();
    }

    private void configureStatisticsCharts() {
        _statusChart.setLegendVisible(true);
        _statusChart.setLabelsVisible(true);
        _statusChart.setAnimated(false);
        _statusChart.setPrefSize(300, 225);
        _statusChart.setMinSize(220, 210);
        _statusChart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        _statusChart.getStyleClass().add("student-status-chart");

        configureBarChart(_gradeChart, "年级", 230);
        configureBarChart(_majorChart, "专业", 280);
    }

    private static void configureBarChart(BarChart<String, Number> chart,
                                          String categoryLabel, double width) {
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setCategoryGap(8);
        chart.setBarGap(2);
        chart.setPrefSize(width, 225);
        chart.setMinSize(220, 210);
        chart.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        chart.getXAxis().setLabel(categoryLabel);
        chart.getYAxis().setLabel("人数");
        if (chart.getXAxis() instanceof CategoryAxis categoryAxis) {
            categoryAxis.setTickLabelRotation(-25);
        }
        chart.getStyleClass().add("student-bar-chart");
    }

    private VBox chartCard(String title, javafx.scene.chart.Chart chart) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("chart-card-title");
        Label emptyLabel = new Label("当前筛选暂无数据");
        emptyLabel.getStyleClass().add("chart-empty-label");
        emptyLabel.setVisible(true);
        _chartEmptyLabels.put(chart, emptyLabel);
        StackPane chartPane = new StackPane(chart, emptyLabel);
        chartPane.setMinHeight(225);
        chartPane.setPrefHeight(225);
        VBox.setVgrow(chartPane, Priority.ALWAYS);
        StackPane.setAlignment(emptyLabel, Pos.CENTER);
        VBox card = new VBox(2, titleLabel, chartPane);
        card.getStyleClass().add("chart-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private static VBox statisticCard(String title, Label value, String styleClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        value.getStyleClass().addAll("stat-value", styleClass);
        VBox card = new VBox(1, titleLabel, value);
        card.getStyleClass().add("stat-card");
        return card;
    }

    private VBox createEditor() {
        _editorModeLabel.getStyleClass().add("editor-title");
        Label editorHint = new Label(
                "带 * 的项目为必填项；学号和一卡通号不可重复。新增学籍会自动创建学生账号，初始密码为 123456；修改学号会同步选课记录。");
        editorHint.getStyleClass().add("field-hint");
        editorHint.setWrapText(true);

        GridPane form = new GridPane();
        form.getStyleClass().add("student-form");
        form.setHgap(10);
        form.setVgap(5);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(76);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        fieldColumn.setFillWidth(true);
        form.getColumnConstraints().addAll(labelColumn, fieldColumn);

        int row = 0;
        addFormRow(form, row++, "学号 *", _studentIdField);
        addFormRow(form, row++, "一卡通号 *", _campusCardField);
        addFormRow(form, row++, "用户账号 *", _userIdField);
        addFormRow(form, row++, "姓名 *", _nameField);
        addFormRow(form, row++, "班级 *", _classNameField);
        addFormRow(form, row++, "专业 *", _majorField);
        addFormRow(form, row++, "年级 *", _gradeField);
        addFormRow(form, row++, "入学日期", _enrollmentDatePicker);
        addFormRow(form, row, "学籍状态", _statusBox);

        _formErrorLabel.getStyleClass().add("form-error");
        _formErrorLabel.setWrapText(true);
        _formErrorLabel.setVisible(false);
        _formErrorLabel.setManaged(false);

        _addButton.setMaxWidth(Double.MAX_VALUE);
        _addButton.getStyleClass().addAll("button", "primary-button");
        _addButton.setTooltip(new Tooltip("使用当前表单内容创建新的学生档案"));
        _addButton.setOnAction(event -> addStudent());

        _updateButton.setText("保存修改");
        _updateButton.setMaxWidth(Double.MAX_VALUE);
        _updateButton.getStyleClass().addAll("button", "secondary-button");
        _updateButton.setTooltip(new Tooltip("保存所选学生的档案修改"));
        _updateButton.setDisable(true);
        _updateButton.setOnAction(event -> updateStudent());

        _deleteButton.setText("删除档案");
        _deleteButton.setMaxWidth(Double.MAX_VALUE);
        _deleteButton.getStyleClass().addAll("button", "danger-button");
        _deleteButton.setDisable(true);
        _deleteButton.setOnAction(event -> deleteStudent());

        _resetPasswordButton.setMaxWidth(Double.MAX_VALUE);
        _resetPasswordButton.getStyleClass().addAll("button", "quiet-button");
        _resetPasswordButton.setTooltip(new Tooltip("将所选学生密码重置为 123456"));
        _resetPasswordButton.setDisable(true);
        _resetPasswordButton.setOnAction(event -> resetStudentPassword());

        Button clearButton = new Button("清空表单");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.getStyleClass().addAll("button", "quiet-button");
        clearButton.setTooltip(new Tooltip("清空当前表单，取消正在编辑的学生"));
        clearButton.setOnAction(event -> {
            _table.getSelectionModel().clearSelection();
            clearForm();
        });
        _operationButtons.addAll(List.of(_addButton, _updateButton, _deleteButton,
                _resetPasswordButton, clearButton));

        GridPane actions = new GridPane();
        actions.setHgap(8);
        actions.setVgap(8);
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(50);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(50);
        actions.getColumnConstraints().addAll(left, right);
        actions.add(_addButton, 0, 0);
        actions.add(_updateButton, 1, 0);
        actions.add(clearButton, 0, 1);
        actions.add(_resetPasswordButton, 1, 1);
        actions.add(_deleteButton, 0, 2);

        VBox editor = new VBox(7, _editorModeLabel, editorHint, _formErrorLabel, form, actions);
        editor.getStyleClass().add("editor-panel");
        return editor;
    }

    private HBox createStatusBar() {
        Label connectionLabel = new Label("服务器：127.0.0.1:8888");
        connectionLabel.getStyleClass().add("connection-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        _statusLabel.getStyleClass().add("status-message");
        _undoButton.getStyleClass().addAll("button", "quiet-button", "undo-button");
        _undoButton.setDisable(true);
        _undoButton.setTooltip(new Tooltip("撤回最近一次学籍操作"));
        _undoButton.setOnAction(event -> undoLastOperation());
        _progressIndicator.setMinSize(15, 15);
        _progressIndicator.setPrefSize(15, 15);
        _progressIndicator.setMaxSize(15, 15);
        _progressIndicator.setVisible(false);
        HBox statusBar = new HBox(8, connectionLabel, spacer,
                _undoButton, _progressIndicator, _statusLabel);
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getStyleClass().add("status-bar");
        return statusBar;
    }

    private void configureFields() {
        _studentIdField.setPromptText("如 2024000001");
        _campusCardField.setPromptText("输入一卡通号");
        _userIdField.setPromptText("8 位用户账号");
        _nameField.setPromptText("输入姓名");
        _classNameField.setPromptText("输入班级");
        _majorField.setPromptText("输入专业");
        _gradeField.setPromptText("如 2024");
        _gradeField.setTextFormatter(new javafx.scene.control.TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,4}") ? change : null));
        _enrollmentDatePicker.setPromptText("选择日期");
        _statusBox.getItems().setAll(StudentStatus.values());
        _statusBox.setValue(StudentStatus.ENROLLED);

        _enrollmentDatePicker.setMaxWidth(Double.MAX_VALUE);
        _statusBox.setMaxWidth(Double.MAX_VALUE);

        installFieldValidation(_studentIdField,
                () -> validateRequired(_studentIdField, "学号", 10));
        installFieldValidation(_campusCardField,
                () -> validateRequired(_campusCardField, "一卡通号", 20));
        installFieldValidation(_userIdField, this::validateUserId);
        installFieldValidation(_nameField,
                () -> validateRequired(_nameField, "姓名", 20));
        installFieldValidation(_classNameField,
                () -> validateRequired(_classNameField, "班级", 40));
        installFieldValidation(_majorField,
                () -> validateRequired(_majorField, "专业", 50));
        installFieldValidation(_gradeField, this::validateGrade);
    }

    private void configureTable() {
        addTextColumn("学号", 112, Student::getStudentId);
        if (isTeacher()) {
            addTextColumn("一卡通号", 120, Student::getCampusCardNo);
        }
        addTextColumn("姓名", 88, Student::getName);
        addTextColumn("班级", 155, Student::getClassName);
        addTextColumn("专业", 175, Student::getMajor);
        addTextColumn("年级", 74, Student::getGrade);
        addStatusColumn();

        _table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        _table.getSelectionModel().setSelectionMode(
                isAdmin() ? SelectionMode.MULTIPLE : SelectionMode.SINGLE);
        _table.setRowFactory(table -> {
            TableRow<Student> row = new TableRow<>();
            row.setOnContextMenuRequested(event -> {
                if (!row.isEmpty() && !_table.getSelectionModel().isSelected(row.getIndex())) {
                    _table.getSelectionModel().clearAndSelect(row.getIndex());
                }
            });
            return row;
        });
        Label emptyTitle = new Label("未找到学生记录");
        emptyTitle.getStyleClass().add("empty-state-title");
        Label emptyHint = new Label("可修改查询条件后重试");
        emptyHint.getStyleClass().add("empty-state-hint");
        VBox emptyState = new VBox(4, emptyTitle, emptyHint);
        emptyState.setAlignment(Pos.CENTER);
        _table.setPlaceholder(emptyState);
        _table.getStyleClass().add("student-table");
        if (isAdmin()) {
            _table.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, selected) -> fillForm(selected));
            _table.getSelectionModel().getSelectedItems().addListener(
                    (javafx.collections.ListChangeListener<Student>) change -> updateEditingButtons());
        }
        configureTableContextMenu();
    }

    private void configureTableContextMenu() {
        ContextMenu menu = new ContextMenu();
        MenuItem statistics = new MenuItem(isTeacher() ? "查看当前名单统计图" : "查看统计图");
        statistics.setOnAction(event -> showStatisticsDialog());
        menu.getItems().add(statistics);
        if (isAdmin()) {
            MenuItem batchStatus = new MenuItem("批量修改状态…");
            batchStatus.setOnAction(event -> promptBatchUpdateStatus());
            batchStatus.disableProperty().bind(
                    javafx.beans.binding.Bindings.isEmpty(_table.getSelectionModel().getSelectedItems()));
            menu.getItems().add(batchStatus);
            MenuItem overview = new MenuItem("查看学生详细信息");
            overview.setOnAction(event -> showStudentOverview());
            overview.disableProperty().bind(
                    javafx.beans.binding.Bindings.isNull(_table.getSelectionModel().selectedItemProperty()));
            menu.getItems().add(overview);
        }
        _table.setContextMenu(menu);
    }

    private void showStudentOverview() {
        Student selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setInlineStatus("请先选择一名学生", true);
            return;
        }
        runOperation("正在读取学生详细信息...", "学生详细信息已加载", () ->
                _studentClientSrv.loadOverview(selected.getStudentId()),
                this::showStudentOverviewDialog);
    }

    private void showStudentOverviewDialog(StudentCampusOverview overview) {
        Student student = _students.stream()
                .filter(value -> overview.getStudentId().equals(value.getStudentId()))
                .findFirst().orElse(null);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("查看学生详细信息");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().getStyleClass().addAll("app-root", "student-detail-dialog");
        dialog.getDialogPane().setMinWidth(720);
        dialog.getDialogPane().setPrefWidth(820);
        dialog.getDialogPane().setPrefHeight(620);

        Label title = new Label(student == null ? "学生详细信息" : student.getName());
        title.getStyleClass().add("student-detail-title");
        Label subtitle = new Label(student == null ? overview.getStudentId()
                : overview.getStudentId() + "  ·  " + valueOrEmpty(student.getMajor()));
        subtitle.getStyleClass().add("student-detail-subtitle");
        VBox heading = new VBox(3, title, subtitle);
        heading.getStyleClass().add("student-detail-heading");

        GridPane identity = new GridPane();
        identity.setHgap(26);
        identity.setVgap(11);
        identity.getStyleClass().add("student-detail-identity");
        addOverviewField(identity, 0, 0, "学号", overview.getStudentId());
        addOverviewField(identity, 1, 0, "一卡通号", student == null ? null : student.getCampusCardNo());
        addOverviewField(identity, 0, 1, "班级", student == null ? null : student.getClassName());
        addOverviewField(identity, 1, 1, "年级", student == null ? null : student.getGrade());
        addOverviewField(identity, 0, 2, "专业", student == null ? null : student.getMajor());
        addOverviewField(identity, 1, 2, "账号状态", overview.getAccountStatus());
        addOverviewField(identity, 0, 3, "学籍状态", student == null || student.getStatus() == null
                ? null : student.getStatus().toString());
        addOverviewField(identity, 1, 3, "入学日期", student == null || student.getEnrollmentDate() == null
                ? null : DATE_FORMAT.format(student.getEnrollmentDate()));

        Label sectionTitle = new Label("校园使用情况");
        sectionTitle.getStyleClass().add("student-detail-section-title");
        GridPane metrics = new GridPane();
        metrics.setHgap(10);
        metrics.setVgap(10);
        metrics.getStyleClass().add("student-detail-metrics");
        addMetric(metrics, 0, 0, "选课记录", String.valueOf(overview.getSelectedCourseCount()), "条", "detail-metric-green");
        addMetric(metrics, 1, 0, "当前借阅", String.valueOf(overview.getActiveBorrowCount()), "本", "detail-metric-blue");
        addMetric(metrics, 2, 0, "逾期借阅", String.valueOf(overview.getOverdueBorrowCount()), "本", "detail-metric-red");
        addMetric(metrics, 3, 0, "校园卡余额", overview.getWalletBalance().toPlainString(), "元", "detail-metric-amber");
        addMetric(metrics, 0, 1, "消费记录", String.valueOf(overview.getPurchaseCount()), "条", "detail-metric-purple");
        addMetric(metrics, 1, 1, "待就诊预约", String.valueOf(overview.getPendingAppointmentCount()), "条", "detail-metric-teal");

        VBox alertBox = new VBox(6);
        alertBox.getStyleClass().add(overview.getWarnings().isEmpty()
                ? "student-detail-ok" : "student-detail-warning");
        Label alertTitle = new Label(overview.getWarnings().isEmpty() ? "状态检查正常" : "需要关注");
        alertTitle.getStyleClass().add("student-detail-alert-title");
        Label alertText = new Label(overview.getWarnings().isEmpty()
                ? "当前没有发现跨模块异常记录。"
                : String.join("\n", overview.getWarnings()));
        alertText.setWrapText(true);
        alertText.getStyleClass().add("student-detail-alert-text");
        alertBox.getChildren().addAll(alertTitle, alertText);

        VBox content = new VBox(16, heading, identity, sectionTitle, metrics, alertBox);
        content.getStyleClass().add("student-detail-content");
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("student-detail-scroll");
        dialog.getDialogPane().setContent(scrollPane);
        dialog.setOnShown(event -> attachStyleSheet(dialog.getDialogPane().getScene()));
        if (_root != null && _root.getScene() != null) {
            dialog.initOwner(_root.getScene().getWindow());
        }
        dialog.showAndWait();
    }

    private static void addOverviewField(GridPane grid, int column, int row,
                                         String labelText, String value) {
        VBox field = new VBox(3);
        field.getStyleClass().add("student-detail-field");
        Label label = new Label(labelText);
        label.getStyleClass().add("student-detail-field-label");
        Label content = new Label(valueOrEmpty(value));
        content.setWrapText(true);
        content.getStyleClass().add("student-detail-field-value");
        field.getChildren().addAll(label, content);
        grid.add(field, column, row);
        GridPane.setColumnSpan(field, 1);
    }

    private static void addMetric(GridPane grid, int column, int row, String labelText,
                                  String value, String unit, String styleClass) {
        VBox card = new VBox(4);
        card.getStyleClass().addAll("student-detail-metric", styleClass);
        Label label = new Label(labelText);
        label.getStyleClass().add("student-detail-metric-label");
        HBox valueLine = new HBox(4);
        valueLine.setAlignment(Pos.BASELINE_LEFT);
        Label number = new Label(value);
        number.getStyleClass().add("student-detail-metric-value");
        Label suffix = new Label(unit);
        suffix.getStyleClass().add("student-detail-metric-unit");
        valueLine.getChildren().addAll(number, suffix);
        card.getChildren().addAll(label, valueLine);
        grid.add(card, column, row);
        GridPane.setHgrow(card, Priority.ALWAYS);
        GridPane.setFillWidth(card, true);
    }

    private String selectedStudentTitle(String studentId) {
        Student student = _students.stream()
                .filter(value -> studentId.equals(value.getStudentId()))
                .findFirst().orElse(null);
        return student == null ? "学生详细信息" : student.getName() + " · " + studentId;
    }

    private void addTextColumn(String title, double width, Function<Student, String> getter) {
        TableColumn<Student, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell ->
                new SimpleStringProperty(getter.apply(cell.getValue())));
        column.setPrefWidth(width);
        column.setMinWidth(Math.min(width, 72));
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                setText(item);
                setTooltip(new Tooltip(item));
            }
        });
        _table.getColumns().add(column);
    }

    private void addStatusColumn() {
        TableColumn<Student, String> column = new TableColumn<>("状态");
        column.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().getStatus())));
        column.setPrefWidth(82);
        column.setMinWidth(72);
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Label _badge = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                _badge.setText(item);
                _badge.getStyleClass().setAll("status-badge");
                if ("在读".equals(item)) {
                    _badge.getStyleClass().add("status-enrolled");
                } else if ("休学".equals(item)) {
                    _badge.getStyleClass().add("status-suspended");
                } else {
                    _badge.getStyleClass().add("status-muted");
                }
                setGraphic(_badge);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
        _table.getColumns().add(column);
    }

    private static void addFormRow(GridPane form, int row, String title, Region control) {
        Label label = new Label(title);
        label.getStyleClass().add("field-label");
        label.setLabelFor(control);
        control.setMaxWidth(Double.MAX_VALUE);
        form.add(label, 0, row);
        form.add(control, 1, row);
    }

    private void refreshStudents() {
        if (isStudent()) {
            refreshMyStudentInfo();
            return;
        }
        String type = _queryTypeBox.getValue();
        String keyword = _queryField.getText();
        String status = _statusFilterBox.getValue();
        runOperation("正在刷新...", "已刷新学生列表",
                () -> filterStudents(_studentClientSrv.findAll(), type, keyword, status),
                (List<Student> students) -> _students.setAll(students));
    }

    private void refreshMyStudentInfo() {
        runOperation("正在读取本人学籍...", "本人学籍已加载",
                _studentClientSrv::getMyStudentInfo, this::showMyStudentInfo);
    }

    private void showMyStudentInfo(Student student) {
        setDetailValue("studentId", student == null ? null : student.getStudentId());
        setDetailValue("campusCardNo", student == null ? null : student.getCampusCardNo());
        setDetailValue("name", student == null ? null : student.getName());
        setDetailValue("className", student == null ? null : student.getClassName());
        setDetailValue("major", student == null ? null : student.getMajor());
        setDetailValue("grade", student == null ? null : student.getGrade());
        setDetailValue("enrollmentDate", student == null || student.getEnrollmentDate() == null
                ? null : DATE_FORMAT.format(student.getEnrollmentDate()));
        setDetailValue("status", student == null || student.getStatus() == null
                ? null : student.getStatus().toString());
        _countLabel.setText(student == null ? "暂无档案" : "本人档案");
    }

    private void setDetailValue(String key, String value) {
        Label label = _detailValues.get(key);
        if (label != null) {
            label.setText(value == null || value.isBlank() ? "未填写" : value);
        }
    }

    private void queryStudents(String type, String value) {
        if (isStudent()) {
            refreshMyStudentInfo();
            return;
        }
        String status = _statusFilterBox.getValue();
        runOperation("正在查询...", "查询完成", () -> {
            return filterStudents(_studentClientSrv.findAll(), type, value, status);
        }, (List<Student> students) -> _students.setAll(students));
    }

    private void exportStudents() {
        if (_students.isEmpty()) {
            setInlineStatus("当前没有可导出的学生记录", true);
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出学生学籍");
        chooser.setInitialFileName("student-records.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        File file = chooser.showSaveDialog(_root == null ? null : _root.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            StudentCsvExporter.write(file.toPath(), List.copyOf(_students), isAdmin());
            setInlineStatus("已导出 " + _students.size() + " 条记录：" + file.getName(), false);
        } catch (Exception exception) {
            setInlineStatus("导出失败：" + exception.getMessage(), true);
        }
    }

    private void importStudents() {
        if (!isAdmin()) {
            setInlineStatus("只有管理员可以批量导入学籍", true);
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("批量导入学生学籍");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        File file = chooser.showOpenDialog(_root == null ? null : _root.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            StudentCsvImporter.ImportResult preview = StudentCsvImporter.read(file.toPath());
            if (preview.fileError() != null) {
                setInlineStatus("导入预览失败：" + preview.fileError(), true);
                return;
            }
            int valid = (int) preview.rows().stream().filter(row -> row.student() != null).count();
            int invalid = preview.rows().size() - valid;
            StringBuilder text = new StringBuilder("文件：").append(file.getName())
                    .append("\n可提交 ").append(valid).append(" 条");
            if (invalid > 0) {
                text.append("，格式错误 ").append(invalid).append(" 条");
                preview.rows().stream().filter(row -> row.error() != null).limit(8)
                        .forEach(row -> text.append("\n第").append(row.lineNumber())
                                .append("行：").append(row.error()));
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, text.toString(),
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setTitle("导入预览");
            confirm.setHeaderText("确认提交后才会写入学籍");
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }
            runOperation("正在导入学籍...", "批量导入完成",
                    () -> importRows(preview), summary -> {
                        _students.addAll(summary.successes());
                        showImportSummary(summary);
                        if (!summary.successes().isEmpty()) {
                            setUndo("撤回本次导入", () -> {
                                for (Student added : summary.successes()) {
                                    _studentClientSrv.deleteStudent(added.getStudentId());
                                }
                                return null;
                            }, ignored -> refreshStudents());
                        }
                    });
        } catch (Exception exception) {
            setInlineStatus("导入预览失败：" + exception.getMessage(), true);
        }
    }

    private ImportSummary importRows(StudentCsvImporter.ImportResult result) {
        List<Student> successes = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (StudentCsvImporter.Row row : result.rows()) {
            if (row.error() != null) {
                failures.add("第" + row.lineNumber() + "行：" + row.error());
                continue;
            }
            try {
                successes.add(_studentClientSrv.addStudent(row.student()));
            } catch (Exception exception) {
                failures.add("第" + row.lineNumber() + "行：" + userMessage(exception));
            }
        }
        return new ImportSummary(successes, failures);
    }

    private void promptBatchUpdateStatus() {
        if (!isAdmin()) return;
        List<Student> selected = new ArrayList<>(_table.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            setInlineStatus("请先多选需要修改的学生", true);
            return;
        }
        ChoiceDialog<StudentStatus> dialog = new ChoiceDialog<>(null,
                StudentStatus.values());
        dialog.setTitle("批量修改学籍状态");
        dialog.setHeaderText("已选择 " + selected.size() + " 条学籍\n当前状态："
                + summarizeStatuses(selected));
        dialog.setContentText("目标状态：");
        dialog.showAndWait().ifPresent(this::batchUpdateStatus);
    }

    private static String summarizeStatuses(List<Student> students) {
        Map<StudentStatus, Long> counts = students.stream()
                .collect(java.util.stream.Collectors.groupingBy(Student::getStatus,
                        java.util.LinkedHashMap::new, java.util.stream.Collectors.counting()));
        return counts.entrySet().stream()
                .map(entry -> String.valueOf(entry.getKey()) + " " + entry.getValue() + " 人")
                .collect(java.util.stream.Collectors.joining("、"));
    }

    private void batchUpdateStatus(StudentStatus target) {
        if (!isAdmin()) {
            return;
        }
        List<Student> selected = new ArrayList<>(_table.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) {
            setInlineStatus("请先多选需要修改的学生", true);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "确定将选中的 " + selected.size() + " 条学籍改为“" + target + "”吗？",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("批量修改学籍状态");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        runOperation("正在批量修改...", "批量修改完成", () -> {
            List<Student> successes = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            List<Student> before = selected.stream()
                    .map(StudentManagementFrame::copyStudent).toList();
            for (Student selectedStudent : selected) {
                Student changed = copyStudent(selectedStudent);
                changed.setStatus(target);
                try {
                    successes.add(_studentClientSrv.updateStudent(
                            selectedStudent.getStudentId(), changed));
                } catch (Exception exception) {
                    failures.add(selectedStudent.getStudentId() + "：" + userMessage(exception));
                }
            }
            return new BatchUpdateSummary(successes, failures, before);
        }, summary -> {
            for (Student updated : summary.successes()) {
                int index = findStudentIndex(updated.getStudentId());
                if (index >= 0) {
                    _students.set(index, updated);
                }
            }
            if (!summary.failures().isEmpty()) {
                showBatchFailureSummary(summary.failures());
            }
            if (!summary.successes().isEmpty()) {
                setUndo("撤回批量状态修改", () -> {
                    for (Student previous : summary.before()) {
                        Student current = _studentClientSrv.findByStudentId(previous.getStudentId());
                        if (current != null) {
                            _studentClientSrv.updateStudent(current.getStudentId(), previous);
                        }
                    }
                    return null;
                }, ignored -> refreshStudents());
            }
        });
    }

    private int findStudentIndex(String studentId) {
        for (int index = 0; index < _students.size(); index++) {
            if (studentId.equals(_students.get(index).getStudentId())) return index;
        }
        return -1;
    }

    private static Student copyStudent(Student source) {
        Student target = new Student();
        target.setStudentId(source.getStudentId());
        target.setCampusCardNo(source.getCampusCardNo());
        target.setUserId(source.getUserId());
        target.setName(source.getName());
        target.setClassName(source.getClassName());
        target.setMajor(source.getMajor());
        target.setGrade(source.getGrade());
        target.setEnrollmentDate(source.getEnrollmentDate());
        target.setStatus(source.getStatus());
        target.setVersion(source.getVersion());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    private void showBatchFailureSummary(List<String> failures) {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "部分记录未修改：\n" + String.join("\n", failures), ButtonType.OK);
        alert.setTitle("批量修改结果");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private record BatchUpdateSummary(List<Student> successes, List<String> failures,
                                      List<Student> before) {
    }

    private void showImportSummary(ImportSummary summary) {
        StringBuilder message = new StringBuilder("成功导入 ")
                .append(summary.successes().size()).append(" 条");
        if (!summary.failures().isEmpty()) {
            message.append("，失败 ").append(summary.failures().size()).append(" 条");
            int limit = Math.min(summary.failures().size(), 12);
            message.append("\n\n").append(String.join("\n", summary.failures().subList(0, limit)));
            if (summary.failures().size() > limit) {
                message.append("\n……其余错误请修正后重新导入");
            }
        }
        Alert alert = new Alert(summary.failures().isEmpty()
                ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING, message.toString(), ButtonType.OK);
        alert.setTitle("批量导入结果");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private record ImportSummary(List<Student> successes, List<String> failures) {
    }

    /** 在服务端已授权的列表上做包含匹配和状态筛选。 */
    private static List<Student> filterStudents(List<Student> students, String type,
                                                String value, String status) {
        String keyword = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return students.stream()
                .filter(student -> "全部状态".equals(status)
                        || (student.getStatus() != null
                        && student.getStatus().toString().equals(status)))
                .filter(student -> keyword.isEmpty()
                        || searchableValue(student, type).toLowerCase(Locale.ROOT)
                        .contains(keyword))
                .toList();
    }

    private static String searchableValue(Student student, String type) {
        if ("一卡通号".equals(type)) {
            return valueOrEmpty(student.getCampusCardNo());
        }
        if ("姓名".equals(type)) {
            return valueOrEmpty(student.getName());
        }
        return valueOrEmpty(student.getStudentId());
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void addStudent() {
        if (!isAdmin()) {
            setInlineStatus("只有管理员可以新增学籍档案", true);
            return;
        }
        if (!validateForm()) {
            return;
        }
        Student formStudent = readForm(null);
        runOperation("正在新增...", "学生新增成功",
                () -> _studentClientSrv.addStudent(formStudent), saved -> {
            _students.add(saved);
            _table.getSelectionModel().clearSelection();
            clearForm();
            setUndo("撤回新增档案", () -> {
                _studentClientSrv.deleteStudent(saved.getStudentId());
                return null;
            }, ignored -> refreshStudents());
        });
    }

    private void updateStudent() {
        if (!isAdmin()) {
            setInlineStatus("只有管理员可以修改或审核学籍档案", true);
            return;
        }
        Student selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setInlineStatus("请先从列表中选择一条学生记录", true);
            return;
        }
        if (!validateForm()) {
            return;
        }
        Student formStudent = readForm(selected);
        Student before = copyStudent(selected);
        boolean studentIdChanged = !selected.getStudentId().equals(formStudent.getStudentId());
        if (studentIdChanged && !confirmStudentIdChange(
                selected.getStudentId(), formStudent.getStudentId())) {
            return;
        }
        runOperation("正在保存...", studentIdChanged ? "学号及学生信息已更新" : "学生信息已更新",
                () -> _studentClientSrv.updateStudent(selected.getStudentId(), formStudent), updated -> {
            int index = _students.indexOf(selected);
            _students.set(index, updated);
            _table.getSelectionModel().select(index);
            setUndo("撤回“" + updated.getName() + "”的修改", () -> {
                _studentClientSrv.updateStudent(updated.getStudentId(), before);
                return null;
            }, ignored -> refreshStudents());
        });
    }

    private void deleteStudent() {
        if (!isAdmin()) {
            setInlineStatus("只有管理员可以删除学籍档案", true);
            return;
        }
        Student selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setInlineStatus("请先从列表中选择一条学生记录", true);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "确定删除“" + selected.getName() + "”（" + selected.getStudentId() + "）吗？",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("此操作将删除该学生的学籍记录");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        runOperation("正在删除...", "学生记录已删除", () -> {
            _studentClientSrv.deleteStudent(selected.getStudentId());
            return null;
        }, ignored -> {
            Student snapshot = copyStudent(selected);
            _students.remove(selected);
            clearForm();
            setUndo("恢复“" + snapshot.getName() + "”的学籍", () -> {
                _studentClientSrv.addStudent(snapshot);
                return null;
            }, ignoredValue -> refreshStudents());
        });
    }

    private void resetStudentPassword() {
        if (!isAdmin()) {
            setInlineStatus("只有管理员可以重置学生密码", true);
            return;
        }
        Student selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setInlineStatus("请先从列表中选择一条学生记录", true);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "确定将“" + selected.getName() + "”的密码重置为 123456 吗？",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("确认重置密码");
        confirm.setHeaderText("重置后请提醒学生及时修改密码");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        runOperation("正在重置密码...", "学生密码已重置为 123456", () -> {
            Message response = _userClientSrv.resetStudentPassword(
                    _currentUser.getUId(), selected.getUserId());
            if (!IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                throw new IllegalStateException(String.valueOf(response.getData()));
            }
            return null;
        }, ignored -> {
        });
    }

    private Student readForm(Student selected) {
        Student student = new Student();
        student.setStudentId(trimmed(_studentIdField));
        student.setCampusCardNo(trimmed(_campusCardField));
        student.setUserId(trimmed(_userIdField));
        student.setName(trimmed(_nameField));
        student.setClassName(trimmed(_classNameField));
        student.setMajor(trimmed(_majorField));
        student.setGrade(trimmed(_gradeField));
        student.setEnrollmentDate(_enrollmentDatePicker.getValue());
        student.setStatus(_statusBox.getValue());
        if (selected != null) {
            student.setVersion(selected.getVersion());
            student.setUpdatedAt(selected.getUpdatedAt());
        }
        return student;
    }

    private void fillForm(Student student) {
        if (!isAdmin()) {
            return;
        }
        boolean editing = student != null;
        clearValidation();
        _addButton.setDisable(editing);
        _updateButton.setDisable(!editing);
        _deleteButton.setDisable(!editing);
        _resetPasswordButton.setDisable(!editing);
        _editorModeLabel.setText(editing
                ? "编辑学生  ·  " + student.getStudentId() : "新建学生");

        if (!editing) {
            clearForm();
            return;
        }
        _studentIdField.setText(student.getStudentId());
        _studentIdField.setEditable(true);
        _campusCardField.setText(student.getCampusCardNo());
        _userIdField.setText(student.getUserId());
        _nameField.setText(student.getName());
        _classNameField.setText(student.getClassName());
        _majorField.setText(student.getMajor());
        _gradeField.setText(student.getGrade());
        _enrollmentDatePicker.setValue(student.getEnrollmentDate());
        _statusBox.setValue(student.getStatus());
    }

    private void clearForm() {
        _studentIdField.clear();
        _studentIdField.setEditable(true);
        _campusCardField.clear();
        _userIdField.clear();
        _nameField.clear();
        _classNameField.clear();
        _majorField.clear();
        _gradeField.clear();
        _enrollmentDatePicker.setValue((LocalDate) null);
        _statusBox.setValue(StudentStatus.ENROLLED);
        _editorModeLabel.setText("新建学生");
        clearValidation();
        updateEditingButtons();
    }

    private <T> void runOperation(String runningText, String successText,
                                  CheckedSupplier<T> operation, Consumer<T> onSuccess) {
        if (_operationRunning) {
            return;
        }
        _operationRunning = true;
        _statusLabel.setText(runningText);
        _statusLabel.getStyleClass().removeAll("status-error", "status-success");
        _statusLabel.setTooltip(null);
        if (_table.getScene() != null) {
            _table.getScene().setCursor(Cursor.WAIT);
        }
        setBusy(true);

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.get();
            }
        };
        task.setOnSucceeded(event -> {
            try {
                onSuccess.accept(task.getValue());
                updateCount();
                _statusLabel.setText(successText);
                _statusLabel.getStyleClass().add("status-success");
            } finally {
                finishOperation();
            }
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            String message = userMessage(exception);
            _statusLabel.setText("操作失败：" + message);
            _statusLabel.getStyleClass().add("status-error");
            _statusLabel.setTooltip(new Tooltip(message));
            finishOperation();
        });

        Thread worker = new Thread(task, "student-client-operation");
        worker.setDaemon(true);
        worker.start();
    }

    private void setBusy(boolean busy) {
        _progressIndicator.setVisible(busy);
        _table.setDisable(busy);
        _queryTypeBox.setDisable(busy);
        _statusFilterBox.setDisable(busy);
        _queryField.setDisable(busy);
        _undoButton.setDisable(busy || _undoOperation == null);
        for (Button button : _operationButtons) {
            button.setDisable(busy);
        }
        for (Control control : formControls()) {
            control.setDisable(busy);
        }
        if (!busy) {
            updateEditingButtons();
        }
    }

    private void finishOperation() {
        _operationRunning = false;
        setBusy(false);
        if (_table.getScene() != null) {
            _table.getScene().setCursor(Cursor.DEFAULT);
        }
    }

    private void setUndo(String description, CheckedSupplier<Void> operation,
                         Consumer<Void> onSuccess) {
        _undoDescription = description;
        _undoOperation = operation;
        _undoSuccess = onSuccess;
        _undoButton.setText(description);
        _undoButton.setDisable(false);
    }

    private void undoLastOperation() {
        if (_undoOperation == null || _operationRunning) {
            return;
        }
        CheckedSupplier<Void> operation = _undoOperation;
        Consumer<Void> success = _undoSuccess;
        clearUndo();
        runOperation("正在撤回...", "已撤回上一步操作", operation, success);
    }

    private void clearUndo() {
        _undoDescription = null;
        _undoOperation = null;
        _undoSuccess = null;
        _undoButton.setText("撤回上一步");
        _undoButton.setDisable(true);
    }

    private void updateEditingButtons() {
        if (!isAdmin()) {
            _addButton.setDisable(true);
            _updateButton.setDisable(true);
            _deleteButton.setDisable(true);
            _resetPasswordButton.setDisable(true);
            return;
        }
        boolean editing = _table.getSelectionModel().getSelectedItem() != null;
        _addButton.setDisable(_operationRunning || editing);
        _updateButton.setDisable(_operationRunning || !editing);
        _deleteButton.setDisable(_operationRunning || !editing);
        _resetPasswordButton.setDisable(_operationRunning || !editing);
    }

    private List<Control> formControls() {
        return List.of(_studentIdField, _campusCardField, _userIdField,
                _nameField, _classNameField, _majorField, _gradeField,
                _enrollmentDatePicker, _statusBox);
    }

    private void updateCount() {
        if (!isStudent()) {
            _countLabel.setText("共 " + _students.size() + " 条");
            updateStudentStatistics();
        }
    }

    private void updateStudentStatistics() {
        int enrolled = 0;
        int suspended = 0;
        int graduated = 0;
        int withdrawn = 0;
        for (Student student : _students) {
            if (student.getStatus() == StudentStatus.ENROLLED) enrolled++;
            else if (student.getStatus() == StudentStatus.SUSPENDED) suspended++;
            else if (student.getStatus() == StudentStatus.GRADUATED) graduated++;
            else if (student.getStatus() == StudentStatus.WITHDRAWN) withdrawn++;
        }
        _totalStatLabel.setText(String.valueOf(_students.size()));
        _enrolledStatLabel.setText(String.valueOf(enrolled));
        _suspendedStatLabel.setText(String.valueOf(suspended));
        _graduatedStatLabel.setText(String.valueOf(graduated));
        _withdrawnStatLabel.setText(String.valueOf(withdrawn));
        Map<String, Long> grades = _students.stream().filter(student -> student.getGrade() != null)
                .collect(java.util.stream.Collectors.groupingBy(Student::getGrade,
                        java.util.LinkedHashMap::new, java.util.stream.Collectors.counting()));
        Map<String, Long> majors = _students.stream().filter(student -> student.getMajor() != null)
                .collect(java.util.stream.Collectors.groupingBy(Student::getMajor,
                        java.util.LinkedHashMap::new, java.util.stream.Collectors.counting()));
        _breakdownStatLabel.setText("年级：" + compactBreakdown(grades)
                + "　专业：" + compactBreakdown(majors));
        if (_statisticsDialogSubtitle != null) {
            _statisticsDialogSubtitle.setText("统计范围：当前学生列表，共 " + _students.size()
                    + " 条（已应用搜索和状态筛选） · 更新于 "
                    + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
        updateStatisticsCharts(grades, majors, enrolled, suspended, graduated, withdrawn);
    }

    private void updateStatisticsCharts(Map<String, Long> grades, Map<String, Long> majors,
                                        int enrolled, int suspended, int graduated,
                                        int withdrawn) {
        _statusChart.getData().setAll(
                pieData("在读", enrolled), pieData("休学", suspended),
                pieData("毕业", graduated), pieData("退学", withdrawn));
        _gradeChart.getData().setAll(barData(grades));
        _majorChart.getData().setAll(barData(majors));
        setChartEmpty(_statusChart, enrolled + suspended + graduated + withdrawn == 0);
        setChartEmpty(_gradeChart, grades.isEmpty());
        setChartEmpty(_majorChart, majors.isEmpty());
    }

    private void setChartEmpty(javafx.scene.chart.Chart chart, boolean empty) {
        Label label = _chartEmptyLabels.get(chart);
        if (label != null) {
            label.setVisible(empty);
            label.setManaged(empty);
        }
        chart.setOpacity(empty ? 0.15 : 1.0);
    }

    private static PieChart.Data pieData(String name, int value) {
        return new PieChart.Data(name + "  " + value, value);
    }

    private static javafx.scene.chart.XYChart.Series<String, Number> barData(
            Map<String, Long> values) {
        javafx.scene.chart.XYChart.Series<String, Number> series =
                new javafx.scene.chart.XYChart.Series<>();
        values.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(5)
                .forEach(entry -> series.getData().add(
                        new javafx.scene.chart.XYChart.Data<>(entry.getKey(), entry.getValue())));
        return series;
    }

    private static String compactBreakdown(Map<String, Long> values) {
        if (values.isEmpty()) return "暂无";
        return values.entrySet().stream().limit(3)
                .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                .collect(java.util.stream.Collectors.joining("、"));
    }

    private boolean isStudent() {
        return _currentUser != null && _currentUser.isStudent();
    }

    private boolean isTeacher() {
        return _currentUser != null && _currentUser.isTeacher();
    }

    private boolean isAdmin() {
        return _currentUser != null && _currentUser.isAdmin();
    }

    private String roleText() {
        if (isStudent()) {
            return "学生视图";
        }
        if (isTeacher()) {
            return "教师视图";
        }
        if (isAdmin()) {
            return "管理员视图";
        }
        return "未登录";
    }

    private void updateQueryPrompt() {
        String type = _queryTypeBox.getValue();
        _queryField.setPromptText(switch (type == null ? "" : type) {
            case "一卡通号" -> "输入一卡通号关键词";
            case "姓名" -> "输入姓名关键词";
            default -> "输入学号关键词";
        });
    }

    private boolean confirmStudentIdChange(String originalStudentId, String newStudentId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "原学号：" + originalStudentId + "\n新学号：" + newStudentId,
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("确认修改学号");
        confirm.setHeaderText("学号将同时更新到该学生已有的选课记录");
        return confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /**
     * 将学籍模块样式表附加到指定场景，支持独立窗口和主界面嵌入两种用法。
     *
     * @param scene 要附加样式的场景
     */
    public void attachStyleSheet(Scene scene) {
        if (scene == null) {
            return;
        }
        URL resource = getClass().getResource("student-management.css");
        if (resource != null) {
            String stylesheet = resource.toExternalForm();
            if (!scene.getStylesheets().contains(stylesheet)) {
                scene.getStylesheets().add(stylesheet);
            }
            return;
        }
        Path sourceFile = Path.of(STYLE_FILE);
        if (Files.exists(sourceFile)) {
            String stylesheet = sourceFile.toUri().toString();
            if (!scene.getStylesheets().contains(stylesheet)) {
                scene.getStylesheets().add(stylesheet);
            }
        }
    }

    private boolean validateForm() {
        _fieldErrors.clear();
        addFieldError(_studentIdField, validateRequired(_studentIdField, "学号", 10));
        addFieldError(_campusCardField,
                validateRequired(_campusCardField, "一卡通号", 20));
        addFieldError(_userIdField, validateUserId());
        addFieldError(_nameField, validateRequired(_nameField, "姓名", 20));
        addFieldError(_classNameField,
                validateRequired(_classNameField, "班级", 40));
        addFieldError(_majorField, validateRequired(_majorField, "专业", 50));
        addFieldError(_gradeField, validateGrade());
        refreshValidationSummary();

        if (_fieldErrors.isEmpty()) {
            return true;
        }
        _fieldErrors.keySet().iterator().next().requestFocus();
        setInlineStatus("表单中还有需要修改的内容", true);
        return false;
    }

    private void installFieldValidation(TextField field, Supplier<String> validator) {
        field.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) {
                addFieldError(field, validator.get());
                refreshValidationSummary();
            }
        });
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (_fieldErrors.remove(field) != null) {
                field.getStyleClass().remove("field-error");
                refreshValidationSummary();
            }
        });
    }

    private void addFieldError(TextField field, String message) {
        field.getStyleClass().remove("field-error");
        if (message == null) {
            _fieldErrors.remove(field);
            return;
        }
        _fieldErrors.put(field, message);
        field.getStyleClass().add("field-error");
    }

    private void refreshValidationSummary() {
        boolean hasErrors = !_fieldErrors.isEmpty();
        _formErrorLabel.setManaged(hasErrors);
        _formErrorLabel.setVisible(hasErrors);
        _formErrorLabel.setText(hasErrors
                ? "请检查：" + String.join("；", _fieldErrors.values()) : "");
    }

    private void clearValidation() {
        for (TextField field : _fieldErrors.keySet()) {
            field.getStyleClass().remove("field-error");
        }
        _fieldErrors.clear();
        refreshValidationSummary();
    }

    private String validateRequired(TextField field, String label, int maxLength) {
        String value = trimmed(field);
        if (value.isEmpty()) {
            return label + "不能为空";
        }
        return value.length() > maxLength
                ? label + "不能超过" + maxLength + "个字符" : null;
    }

    private String validateUserId() {
        String value = trimmed(_userIdField);
        if (value.isEmpty()) {
            return "用户账号不能为空";
        }
        return value.length() == 8 ? null : "用户账号必须为 8 位";
    }

    private String validateGrade() {
        String value = trimmed(_gradeField);
        if (value.isEmpty()) {
            return "年级不能为空";
        }
        return value.matches("[0-9]{4}") ? null : "年级必须是 4 位数字";
    }

    private static String trimmed(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void setInlineStatus(String message, boolean error) {
        _statusLabel.setText(message);
        _statusLabel.getStyleClass().removeAll("status-error", "status-success");
        if (error) {
            _statusLabel.getStyleClass().add("status-error");
        }
    }

    private static String userMessage(Throwable exception) {
        if (exception instanceof StudentClientException) {
            return exception.getMessage();
        }
        if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
            return exception.getMessage();
        }
        return "无法连接服务器，请确认服务器已启动后重试";
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
