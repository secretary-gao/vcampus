/*
 * TeacherCoursesPane
 *
 * Version 1.1
 *
 * 2026-09-08
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.client.biz.IStudentClientSrv;
import vcampus.client.biz.StudentClientSrv;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.common.vo.TeachingClass;
import vcampus.common.vo.User;

import java.util.ArrayList;
import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 教师工作台：按本人授课课程切换查看选课学生名单。 */
public class TeacherCoursesPane extends VBox {

    private final ICourseClientSrv _client;
    private final String _teacherName;
    private final User _currentUser;
    private final IStudentClientSrv _studentClient;
    private final ListView<TeacherCourse> _courseList = new ListView<>();
    private final TableView<TeacherCourseEnrollment> _rosterTable = new TableView<>();
    private final ObservableList<TeacherCourseEnrollment> _rosterRows = FXCollections.observableArrayList();
    private final FilteredList<TeacherCourseEnrollment> _filteredRoster = new FilteredList<>(_rosterRows);
    private final TextField _rosterSearch = new TextField();
    private final Label _rosterMatches = new Label();
    private final Label _courseCountValue = new Label("—");
    private final Label _studentCountValue = new Label("—");
    private final Label _currentCountValue = new Label("—");
    private final Label _rosterTitle = new Label("选课学生");
    private final Label _detailTitle = new Label("请选择教学班");
    private final Label _detailMeta = new Label("课程安排与容量将在这里显示");
    private final Label _statusLabel = new Label();
    private TeacherCourse _currentCourse;
    private boolean _refreshing;

    /** 创建教师课程名单页面。 */
    public TeacherCoursesPane(ICourseClientSrv client, User currentUser) {
        this(client, currentUser, new StudentClientSrv(currentUser));
    }

    /** 注入两个模块的客户端，学籍请求仍由服务器检查当前教师身份。 */
    TeacherCoursesPane(ICourseClientSrv client, String teacherName,
                       IStudentClientSrv studentClient) {
        this(client, teacherForViewTest(teacherName), studentClient);
    }

    private TeacherCoursesPane(ICourseClientSrv client, User currentUser,
                               IStudentClientSrv studentClient) {
        this._client = client;
        this._currentUser = currentUser;
        this._teacherName = currentUser.getUName();
        this._studentClient = studentClient;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    /** 从服务器刷新教师本人课程及名单。 */
    public void refresh() {
        if (_refreshing) {
            return;
        }
        _refreshing = true;
        setDisable(true);
        TeacherCourse selected = _courseList.getSelectionModel().getSelectedItem();
        String selectedClassId = selected == null ? null : selected.teachingClassId();
        _statusLabel.setText("正在读取本人课程…");
        Task<Snapshot> request = new Task<>() {
            @Override
            protected Snapshot call() throws Exception {
                return loadSnapshot();
            }
        };
        request.setOnSucceeded(event -> {
            _refreshing = false;
            setDisable(false);
            Snapshot snapshot = request.getValue();
            _courseList.setItems(FXCollections.observableArrayList(snapshot.courses()));
            _courseCountValue.setText(String.valueOf(snapshot.courses().size()));
            _studentCountValue.setText(String.valueOf(snapshot.totalStudents()));
            _statusLabel.setText("数据已更新");
            if (snapshot.courses().isEmpty()) {
                showRoster(null);
            } else {
                TeacherCourse restored = snapshot.courses().stream()
                        .filter(course -> course.teachingClassId().equals(selectedClassId))
                        .findFirst().orElse(snapshot.courses().get(0));
                _courseList.getSelectionModel().select(restored);
            }
        });
        request.setOnFailed(event -> {
            _refreshing = false;
            setDisable(false);
            _statusLabel.setText("刷新失败，当前名单可能不是最新。请再次点击“刷新”。"
                    + CourseViewSupport.safe(request.getException().getMessage(), "连接失败"));
        });
        Thread thread = new Thread(request, "teacher-roster-refresh");
        thread.setDaemon(true);
        thread.start();
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        Label title = new Label("教师工作台");
        title.getStyleClass().add("page-title");
        Label description = new Label(_teacherName + " · 查看本人授课课程与选课名单");
        description.getStyleClass().add("page-description");
        VBox heading = new VBox(3, title, description);
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button refreshButton = new Button("刷新");
        refreshButton.getStyleClass().add("secondary");
        refreshButton.setOnAction(event -> refresh());
        Button exportButton = new Button("导出整班名单 CSV");
        exportButton.getStyleClass().add("primary");
        exportButton.setOnAction(event -> exportRoster());
        HBox header = new HBox(12, heading, exportButton, refreshButton);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox metrics = new HBox(12,
                metric("授课教学班", _courseCountValue, " 个"),
                metric("选课记录", _studentCountValue, " 人次"),
                metric("当前教学班选课", _currentCountValue, " 人"));

        Label listTitle = new Label("我教的教学班");
        listTitle.getStyleClass().add("section-title");
        _courseList.getStyleClass().add("teacher-course-list");
        _courseList.setPlaceholder(emptyLabel("暂无授课课程"));
        _courseList.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(TeacherCourse item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label name = new Label(item.course().getCourseName());
                name.getStyleClass().add("course-list-name");
                Label meta = new Label(item.course().getCourseId() + "  ·  "
                        + item.classNumber() + " 班  ·  "
                        + item.roster().size() + " / " + item.capacity() + " 人");
                meta.getStyleClass().add("course-list-meta");
                setGraphic(new VBox(4, name, meta));
            }
        });
        _courseList.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> showRoster(selected));
        VBox left = new VBox(10, listTitle, _courseList);
        left.getStyleClass().add("course-card");
        left.setMinWidth(260);
        VBox.setVgrow(_courseList, Priority.ALWAYS);

        _rosterTitle.getStyleClass().add("section-title");
        _detailTitle.getStyleClass().add("teacher-detail-title");
        _detailMeta.getStyleClass().add("teacher-detail-meta");
        _detailMeta.setWrapText(true);
        VBox detail = new VBox(4, _detailTitle, _detailMeta);
        detail.getStyleClass().add("teacher-detail-strip");
        _rosterTable.getColumns().addAll(
                CourseViewSupport.textColumn("学号", 115,
                        TeacherCourseEnrollment::getStudentId),
                CourseViewSupport.textColumn("姓名", 90,
                        TeacherCourseEnrollment::getStudentName),
                CourseViewSupport.textColumn("班级", 125,
                        TeacherCourseEnrollment::getClassName),
                CourseViewSupport.textColumn("专业", 155,
                        TeacherCourseEnrollment::getMajor),
                CourseViewSupport.textColumn("选课时间", 145,
                        row -> CourseViewSupport.dateTime(row.getSelectTime()))
        );
        CourseViewSupport.configureTable(_rosterTable, "暂无学生选修该课程");
        SortedList<TeacherCourseEnrollment> sortedRoster = new SortedList<>(_filteredRoster);
        sortedRoster.comparatorProperty().bind(_rosterTable.comparatorProperty());
        _rosterTable.setItems(sortedRoster);
        Button studentRecordButton = new Button("查看学籍");
        studentRecordButton.getStyleClass().add("primary");
        studentRecordButton.setId("teacher-view-student-record");
        studentRecordButton.setMinWidth(USE_PREF_SIZE);
        studentRecordButton.disableProperty().bind(
                _rosterTable.getSelectionModel().selectedItemProperty().isNull());
        studentRecordButton.setTooltip(new Tooltip("查看所选学生的学籍信息（只读）"));
        studentRecordButton.setOnAction(event -> showStudentRecord());
        _rosterTable.setId("teacher-student-roster");
        _rosterTable.setRowFactory(table -> {
            TableRow<TeacherCourseEnrollment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY
                        && event.getClickCount() == 2) {
                    table.getSelectionModel().select(row.getItem());
                    showStudentRecord();
                }
            });
            return row;
        });
        _rosterTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                showStudentRecord();
                event.consume();
            }
        });
        HBox rosterHeader = new HBox(12, _rosterTitle, studentRecordButton);
        rosterHeader.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(_rosterTitle, Priority.ALWAYS);
        _rosterTitle.setMaxWidth(Double.MAX_VALUE);
        Label rosterHint = new Label("选中学生后可查看学籍；学生选课或退课后，点击上方“刷新”更新名单。");
        rosterHint.getStyleClass().add("status-label");
        rosterHint.setWrapText(true);
        _rosterSearch.setId("teacher-roster-search");
        _rosterSearch.setPromptText("按学号或姓名搜索当前班学生");
        _rosterSearch.setAccessibleText("按学号或姓名搜索当前教学班学生");
        _rosterSearch.textProperty().addListener((observable, oldValue, value) -> updateRosterFilter());
        HBox.setHgrow(_rosterSearch, Priority.ALWAYS);
        Button clearSearch = new Button("清空");
        clearSearch.getStyleClass().add("secondary");
        clearSearch.disableProperty().bind(_rosterSearch.textProperty().isEmpty());
        clearSearch.setOnAction(event -> _rosterSearch.clear());
        _rosterMatches.getStyleClass().add("status-label");
        HBox searchBar = new HBox(8, _rosterSearch, clearSearch, _rosterMatches);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        VBox right = new VBox(10, rosterHeader, detail, rosterHint, searchBar, _rosterTable);
        right.getStyleClass().add("course-card");
        VBox.setVgrow(_rosterTable, Priority.ALWAYS);

        SplitPane workspace = new SplitPane(left, right);
        workspace.setDividerPositions(0.30);
        workspace.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(workspace, Priority.ALWAYS);

        _statusLabel.getStyleClass().add("status-label");
        _statusLabel.setWrapText(true);
        getChildren().addAll(header, metrics, workspace, _statusLabel);
    }

    private Snapshot loadSnapshot() throws Exception {
        List<TeacherCourseEnrollment> enrollments =
                _client.queryTeacherCourseEnrollments(_currentUser);
        Map<String, List<TeacherCourseEnrollment>> byClass = enrollments.stream()
                .filter(row -> row.getTeachingClassId() != null)
                .collect(Collectors.groupingBy(TeacherCourseEnrollment::getTeachingClassId,
                        LinkedHashMap::new, Collectors.toList()));

        Map<String, Course> courseMap = _client.queryCourse("").stream()
                .collect(Collectors.toMap(Course::getCourseId, course -> course,
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, TeachingClass> classMap = _client.queryTeachingClass("").stream()
                .filter(value -> _teacherName.equals(value.getTeacher()))
                .collect(Collectors.toMap(TeachingClass::getTeachingClassId,
                        Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<String, List<CourseSchedule>> schedulesByClass = _client.querySchedule().stream()
                .collect(Collectors.groupingBy(CourseSchedule::getTeachingClassId));
        Map<String, TeacherCourse> courses = new LinkedHashMap<>();
        for (Map.Entry<String, List<TeacherCourseEnrollment>> entry : byClass.entrySet()) {
            TeacherCourseEnrollment first = entry.getValue().get(0);
            Course course = courseMap.get(first.getCourseId());
            if (course == null) {
                course = new Course(first.getCourseId(),
                        CourseViewSupport.safe(first.getCourseName(), first.getCourseId()),
                        _teacherName, 0, 0, 0);
            }
            TeachingClass teachingClass = classMap.get(entry.getKey());
            courses.put(entry.getKey(), new TeacherCourse(entry.getKey(), course, teachingClass,
                    CourseViewSupport.safe(first.getClassNumber(), "—"),
                    schedulesByClass.getOrDefault(entry.getKey(), List.of()),
                    realStudents(entry.getValue())));
        }

        List<TeacherCourse> result = new ArrayList<>(courses.values());
        int totalStudents = result.stream().mapToInt(course -> course.roster().size()).sum();
        return new Snapshot(result, totalStudents);
    }

    private List<TeacherCourseEnrollment> realStudents(
            List<TeacherCourseEnrollment> enrollments) {
        return enrollments.stream()
                .filter(row -> row.getStudentId() != null && !row.getStudentId().isBlank())
                .sorted(Comparator.comparing(TeacherCourseEnrollment::getStudentId))
                .toList();
    }

    private void showRoster(TeacherCourse selected) {
        _rosterTable.getSelectionModel().clearSelection();
        if (selected == null) {
            _currentCourse = null;
            _rosterTitle.setText("选课学生");
            _detailTitle.setText("请选择教学班");
            _detailMeta.setText("课程安排与容量将在这里显示");
            _currentCountValue.setText("0");
            _rosterRows.clear();
            updateRosterFilter();
            return;
        }
        _currentCourse = selected;
        _rosterTitle.setText(selected.course().getCourseName() + " · "
                + selected.classNumber() + " 班 · 选课学生");
        _detailTitle.setText(selected.course().getCourseId() + " · "
                + selected.course().getCourseName());
        _detailMeta.setText("教学班 " + selected.classNumber() + "  ·  容量 "
                + selected.capacity() + " 人  ·  " + scheduleSummary(selected.schedules()));
        _currentCountValue.setText(String.valueOf(selected.roster().size()));
        _rosterRows.setAll(selected.roster());
        updateRosterFilter();
    }

    private void updateRosterFilter() {
        _rosterTable.getSelectionModel().clearSelection();
        String keyword = _rosterSearch.getText().trim().toLowerCase(Locale.ROOT);
        _filteredRoster.setPredicate(row -> keyword.isEmpty()
                || containsKeyword(row.getStudentId(), keyword)
                || containsKeyword(row.getStudentName(), keyword));
        _rosterMatches.setText("显示 " + _filteredRoster.size() + " / " + _rosterRows.size() + " 人");
        _rosterTable.setPlaceholder(emptyLabel(_currentCourse == null ? "请选择教学班"
                : _rosterRows.isEmpty() ? "暂无学生选修该教学班" : "没有匹配的学生，请调整或清空搜索条件"));
    }

    private static boolean containsKeyword(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private static User teacherForViewTest(String teacherName) {
        User user = new User();
        user.setUName(teacherName);
        user.setURole("教师");
        return user;
    }

    private void exportRoster() {
        if (_currentCourse == null) {
            _statusLabel.setText("请先选择一个教学班");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导出教学班学生名单");
        chooser.setInitialFileName(_currentCourse.course().getCourseId() + "-"
                + _currentCourse.classNumber() + "-roster.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        File file = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file == null) return;
        try {
            CourseCsvExporter.writeRoster(file.toPath(), _currentCourse.roster());
            _statusLabel.setText("已导出：" + file.getName());
        } catch (Exception exception) {
            new Alert(Alert.AlertType.ERROR, "导出失败：" + exception.getMessage()).showAndWait();
        }
    }

    private void showStudentRecord() {
        TeacherCourseEnrollment selected = _rosterTable.getSelectionModel().getSelectedItem();
        if (selected == null || isDisabled()) {
            return;
        }
        new StudentRecordDialog(getScene().getWindow(), _studentClient,
                selected.getStudentId()).show();
    }

    private VBox metric(String labelText, Label value, String suffix) {
        Label label = new Label(labelText);
        label.getStyleClass().add("metric-label");
        value.getStyleClass().add("metric-value");
        Label suffixLabel = new Label(suffix);
        suffixLabel.getStyleClass().add("status-label");
        HBox number = new HBox(4, value, suffixLabel);
        number.setAlignment(Pos.BASELINE_LEFT);
        VBox card = new VBox(5, label, number);
        card.getStyleClass().add("metric-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private Label emptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state-label");
        return label;
    }

    private String scheduleSummary(List<CourseSchedule> schedules) {
        if (schedules.isEmpty()) {
            return "尚未排课";
        }
        return schedules.stream()
                .sorted(Comparator.comparingInt(CourseSchedule::getDayOfWeek)
                        .thenComparingInt(CourseSchedule::getWeekStart)
                        .thenComparingInt(CourseSchedule::getStartPeriod))
                .map(schedule -> schedule.getWeekStart() + "–" + schedule.getWeekEnd()
                        + " 周 " + CourseViewSupport.dayName(schedule.getDayOfWeek()) + " "
                        + schedule.getStartPeriod() + "–" + schedule.getEndPeriod() + " 节 · "
                        + CourseViewSupport.safe(schedule.getClassroom(), "教室待定"))
                .collect(Collectors.joining("；"));
    }

    private record TeacherCourse(String teachingClassId, Course course, TeachingClass teachingClass,
                                 String classNumber, List<CourseSchedule> schedules,
                                 List<TeacherCourseEnrollment> roster) {
        int capacity() {
            return teachingClass == null ? roster.size() : teachingClass.getCapacity();
        }
    }

    private record Snapshot(List<TeacherCourse> courses, int totalStudents) {
    }
}
