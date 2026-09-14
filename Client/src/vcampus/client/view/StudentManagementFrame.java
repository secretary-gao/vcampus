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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Screen;
import vcampus.client.biz.StudentClientException;
import vcampus.client.biz.StudentClientSrv;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentStatus;
import vcampus.common.vo.User;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final TextField _queryField = new TextField();

    private final Label _countLabel = new Label("共 0 条");
    private final Label _editorModeLabel = new Label("新建学生");
    private final Label _statusLabel = new Label("就绪");
    private final Label _formErrorLabel = new Label();
    private final ProgressIndicator _progressIndicator = new ProgressIndicator();
    private final Button _addButton = new Button("新增档案");
    private final Button _updateButton = new Button("保存修改");
    private final Button _deleteButton = new Button("删除记录");
    private final List<Button> _operationButtons = new ArrayList<>();
    private final Map<TextField, String> _fieldErrors = new LinkedHashMap<>();
    private final Map<String, Label> _detailValues = new LinkedHashMap<>();
    private boolean _operationRunning;
    private BorderPane _root;

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
        if (isAdmin()) {
            _queryTypeBox.getItems().add(1, "一卡通号");
        }
        _queryTypeBox.setValue("学号");
        _queryTypeBox.setPrefWidth(120);
        _queryTypeBox.getStyleClass().add("query-type");

        updateQueryPrompt();
        _queryTypeBox.valueProperty().addListener(
                (observable, oldValue, newValue) -> updateQueryPrompt());
        _queryField.setPrefWidth(300);
        _queryField.setAccessibleHelp(isAdmin()
                ? "可按学号、一卡通号或姓名精确查询"
                : "可按学号或姓名精确查询");
        HBox.setHgrow(_queryField, Priority.ALWAYS);

        Button queryButton = new Button("查询");
        queryButton.getStyleClass().addAll("button", "primary-button");
        queryButton.setOnAction(event ->
                queryStudents(_queryTypeBox.getValue(), _queryField.getText()));

        Button resetButton = new Button(isTeacher() ? "我的学生" : "显示全部");
        resetButton.getStyleClass().addAll("button", "secondary-button");
        resetButton.setOnAction(event -> {
            _queryField.clear();
            refreshStudents();
        });

        Button refreshButton = new Button("刷新");
        refreshButton.getStyleClass().addAll("button", "quiet-button");
        refreshButton.setTooltip(new Tooltip("重新读取数据库中的学生信息"));
        refreshButton.setOnAction(event -> refreshStudents());

        _queryField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER && !_operationRunning) {
                queryStudents(_queryTypeBox.getValue(), _queryField.getText());
            }
        });
        _operationButtons.addAll(List.of(queryButton, resetButton, refreshButton));

        HBox searchBar = new HBox(8, _queryTypeBox, _queryField,
                queryButton, resetButton, refreshButton);
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
                ? "列表展示关键信息，选中学生后可在右侧查看并修改完整档案"
                : "仅显示自己任教课程的学生及其公开学籍字段");
        sectionHint.getStyleClass().add("section-hint");
        VBox heading = new VBox(2, sectionTitle, sectionHint);
        heading.getStyleClass().add("section-heading");

        VBox section = new VBox(heading, _table);
        section.getStyleClass().add("table-section");
        VBox.setVgrow(_table, Priority.ALWAYS);
        return section;
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

        Button clearButton = new Button("新建档案");
        clearButton.setMaxWidth(Double.MAX_VALUE);
        clearButton.getStyleClass().addAll("button", "quiet-button");
        clearButton.setTooltip(new Tooltip("清空表单并进入新建模式"));
        clearButton.setOnAction(event -> {
            _table.getSelectionModel().clearSelection();
            clearForm();
        });
        _operationButtons.addAll(List.of(_addButton, _updateButton, _deleteButton, clearButton));

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
        actions.add(_deleteButton, 1, 1);

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
        _progressIndicator.setMinSize(15, 15);
        _progressIndicator.setPrefSize(15, 15);
        _progressIndicator.setMaxSize(15, 15);
        _progressIndicator.setVisible(false);
        HBox statusBar = new HBox(8, connectionLabel, spacer,
                _progressIndicator, _statusLabel);
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
        addTextColumn("姓名", 88, Student::getName);
        addTextColumn("班级", 155, Student::getClassName);
        addTextColumn("专业", 175, Student::getMajor);
        addTextColumn("年级", 74, Student::getGrade);
        addStatusColumn();

        _table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        _table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
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
        }
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
        runOperation("正在刷新...", "已刷新学生列表",
                _studentClientSrv::findAll,
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
        if (value == null || value.trim().isEmpty()) {
            refreshStudents();
            return;
        }
        runOperation("正在查询...", "查询完成", () -> {
            String keyword = value.trim();
            if ("学号".equals(type)) {
                Student student = _studentClientSrv.findByStudentId(keyword);
                return student == null ? List.<Student>of() : List.of(student);
            } else if ("一卡通号".equals(type)) {
                Student student = _studentClientSrv.findByCampusCardNo(keyword);
                return student == null ? List.<Student>of() : List.of(student);
            }
            return _studentClientSrv.findByName(keyword);
        }, (List<Student> students) -> _students.setAll(students));
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
            _students.remove(selected);
            clearForm();
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
        _queryField.setDisable(busy);
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

    private void updateEditingButtons() {
        if (!isAdmin()) {
            _addButton.setDisable(true);
            _updateButton.setDisable(true);
            _deleteButton.setDisable(true);
            return;
        }
        boolean editing = _table.getSelectionModel().getSelectedItem() != null;
        _addButton.setDisable(_operationRunning || editing);
        _updateButton.setDisable(_operationRunning || !editing);
        _deleteButton.setDisable(_operationRunning || !editing);
    }

    private List<Control> formControls() {
        return List.of(_studentIdField, _campusCardField, _userIdField,
                _nameField, _classNameField, _majorField, _gradeField,
                _enrollmentDatePicker, _statusBox);
    }

    private void updateCount() {
        if (!isStudent()) {
            _countLabel.setText("共 " + _students.size() + " 条");
        }
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
            case "一卡通号" -> "输入完整一卡通号";
            case "姓名" -> "输入学生姓名";
            default -> "输入完整学号";
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
