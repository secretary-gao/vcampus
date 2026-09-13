/*
 * CourseHallPane
 *
 * Version 2.0
 *
 * 2026-09-13
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.SelectCourse;
import vcampus.common.vo.TeachingClass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 学生课程大厅：按课程组织教学班，并在选择前展示容量与课表冲突状态。 */
public class CourseHallPane extends VBox {

    private final ICourseClientSrv _client;
    private final String _studentId;
    private final Runnable _onEnrollmentChanged;
    private final TextField _keywordField = new TextField();
    private final VBox _courseList = new VBox();
    private final Label _statusLabel = new Label();
    private final ToggleGroup _filterGroup = new ToggleGroup();
    private final Set<String> _expandedCourseIds = new HashSet<>();
    private List<CourseGroup> _allCourses = List.of();
    private boolean _initialExpansionApplied;

    /** 创建课程大厅。 */
    public CourseHallPane(ICourseClientSrv client, String studentId,
                          Runnable onEnrollmentChanged) {
        this._client = client;
        this._studentId = studentId;
        this._onEnrollmentChanged = onEnrollmentChanged;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    /** 一次读取课程、教学班、排课和当前选课，形成一致的只读页面快照。 */
    public void refresh() {
        _statusLabel.setText("正在读取课程与教学班…");
        CourseViewSupport.runAsync(this, this::loadSnapshot, groups -> {
            _allCourses = groups;
            if (!_initialExpansionApplied) {
                if (!groups.isEmpty()) {
                    _expandedCourseIds.add(groups.get(0).course().getCourseId());
                }
                _initialExpansionApplied = true;
            }
            applyFilter();
        });
    }

    private void buildView() {
        Label title = new Label("课程大厅");
        title.getStyleClass().add("page-title");
        Label description = new Label("按课程展开教学班，选课前即可查看时间、余量与冲突状态");
        description.getStyleClass().add("page-description");
        VBox heading = new VBox(3, title, description);

        _keywordField.setPromptText("课程号 / 课程名称 / 教师 / 开课单位");
        _keywordField.setPrefWidth(380);
        _keywordField.setOnAction(event -> applyFilter());
        HBox.setHgrow(_keywordField, Priority.ALWAYS);
        Button searchButton = button("搜索", "primary", this::applyFilter);
        Button refreshButton = button("刷新数据", "secondary", this::refresh);

        ToggleButton all = filterButton("全部课程", "all");
        ToggleButton available = filterButton("当前可选", "available");
        ToggleButton selected = filterButton("我的已选", "selected");
        ToggleButton conflict = filterButton("时间冲突", "conflict");
        all.setSelected(true);
        _filterGroup.selectedToggleProperty().addListener((observable, oldValue, value) -> {
            if (value == null && oldValue != null) {
                oldValue.setSelected(true);
            } else if (value != null) {
                applyFilter();
            }
        });

        HBox searchRow = new HBox(9, _keywordField, searchButton, refreshButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox filters = new HBox(8, new Label("状态"), all, available, selected, conflict);
        filters.setAlignment(Pos.CENTER_LEFT);
        VBox toolbar = new VBox(10, searchRow, filters);
        toolbar.getStyleClass().add("tool-bar-card");

        GridPane columns = courseHeader();
        _courseList.getStyleClass().add("course-catalog");
        ScrollPane scroll = new ScrollPane(_courseList);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("course-catalog-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        _statusLabel.getStyleClass().add("status-label");
        getChildren().addAll(heading, toolbar, columns, scroll, _statusLabel);
    }

    private GridPane courseHeader() {
        GridPane header = courseColumns();
        header.getStyleClass().add("course-catalog-header");
        addColumnLabel(header, "课程号", 0);
        addColumnLabel(header, "课程名称", 1);
        addColumnLabel(header, "教学班", 2);
        addColumnLabel(header, "课程性质", 3);
        addColumnLabel(header, "开课单位", 4);
        addColumnLabel(header, "学分", 5);
        addColumnLabel(header, "选课状态", 6);
        addColumnLabel(header, "", 7);
        return header;
    }

    private void addColumnLabel(GridPane grid, String text, int column) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        grid.add(label, column, 0);
    }

    private GridPane courseColumns() {
        GridPane grid = new GridPane();
        double[] widths = {13, 24, 9, 10, 19, 7, 12, 6};
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            column.setFillWidth(true);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    private SnapshotData loadData() throws Exception {
        List<Course> courses = _client.queryCourse("");
        List<TeachingClass> classes = _client.queryTeachingClass("");
        List<CourseSchedule> schedules = _client.querySchedule();
        List<SelectCourse> selections = _client.querySelectedCourse(_studentId);
        return new SnapshotData(courses, classes, schedules, selections);
    }

    private List<CourseGroup> loadSnapshot() throws Exception {
        SnapshotData data = loadData();
        Map<String, Course> courses = data.courses().stream()
                .collect(Collectors.toMap(Course::getCourseId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, TeachingClass> classes = data.classes().stream()
                .collect(Collectors.toMap(TeachingClass::getTeachingClassId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        Map<String, List<CourseSchedule>> schedulesByClass = data.schedules().stream()
                .collect(Collectors.groupingBy(CourseSchedule::getTeachingClassId));
        Set<String> selectedClassIds = data.selections().stream()
                .map(SelectCourse::getTeachingClassId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        Set<String> selectedCourseIds = data.selections().stream()
                .map(SelectCourse::getCourseId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        List<CourseSchedule> selectedSchedules = selectedClassIds.stream()
                .flatMap(id -> schedulesByClass.getOrDefault(id, List.of()).stream())
                .toList();

        Map<String, String> selectedCourseNamesByClass = new HashMap<>();
        for (String classId : selectedClassIds) {
            TeachingClass selectedClass = classes.get(classId);
            Course selectedCourse = selectedClass == null ? null
                    : courses.get(selectedClass.getCourseId());
            selectedCourseNamesByClass.put(classId, selectedCourse == null
                    ? "已选课程" : selectedCourse.getCourseName());
        }

        Map<String, List<TeachingClassRow>> rowsByCourse = new LinkedHashMap<>();
        data.classes().stream()
                .sorted(Comparator.comparing(TeachingClass::getCourseId)
                        .thenComparing(TeachingClass::getClassNumber))
                .forEach(teachingClass -> {
                    Course course = courses.get(teachingClass.getCourseId());
                    if (course == null) {
                        return;
                    }
                    List<CourseSchedule> classSchedules = schedulesByClass.getOrDefault(
                            teachingClass.getTeachingClassId(), List.of()).stream()
                            .sorted(scheduleOrder()).toList();
                    Availability availability = availability(course, teachingClass,
                            classSchedules, selectedClassIds, selectedCourseIds,
                            selectedSchedules, selectedCourseNamesByClass);
                    rowsByCourse.computeIfAbsent(course.getCourseId(), ignored -> new ArrayList<>())
                            .add(new TeachingClassRow(course, teachingClass,
                                    classSchedules, availability));
                });

        return courses.values().stream()
                .sorted(Comparator.comparing(Course::getCourseId))
                .map(course -> new CourseGroup(course,
                        rowsByCourse.getOrDefault(course.getCourseId(), List.of())))
                .toList();
    }

    private Availability availability(Course course, TeachingClass teachingClass,
                                      List<CourseSchedule> schedules,
                                      Set<String> selectedClassIds,
                                      Set<String> selectedCourseIds,
                                      List<CourseSchedule> selectedSchedules,
                                      Map<String, String> selectedCourseNamesByClass) {
        if (selectedClassIds.contains(teachingClass.getTeachingClassId())) {
            return new Availability(State.SELECTED, "当前已选教学班");
        }
        if (selectedCourseIds.contains(course.getCourseId())) {
            return new Availability(State.SAME_COURSE,
                    "已选择本课程的其他教学班");
        }
        if (teachingClass.getSelectedCount() >= teachingClass.getCapacity()) {
            return new Availability(State.FULL, "教学班名额已满");
        }
        for (CourseSchedule candidate : schedules) {
            for (CourseSchedule selected : selectedSchedules) {
                if (scheduleConflicts(candidate, selected)) {
                    return new Availability(State.CONFLICT,
                            "与《" + selectedCourseNamesByClass.getOrDefault(
                                    selected.getTeachingClassId(), "已选课程") + "》冲突");
                }
            }
        }
        return new Availability(State.AVAILABLE,
                schedules.isEmpty() ? "可选 · 尚未排课" : "可选择");
    }

    static boolean scheduleConflicts(CourseSchedule left, CourseSchedule right) {
        return left.getDayOfWeek() == right.getDayOfWeek()
                && Math.max(left.getWeekStart(), right.getWeekStart())
                <= Math.min(left.getWeekEnd(), right.getWeekEnd())
                && Math.max(left.getStartPeriod(), right.getStartPeriod())
                <= Math.min(left.getEndPeriod(), right.getEndPeriod());
    }

    private Comparator<CourseSchedule> scheduleOrder() {
        return Comparator.comparingInt(CourseSchedule::getDayOfWeek)
                .thenComparingInt(CourseSchedule::getWeekStart)
                .thenComparingInt(CourseSchedule::getStartPeriod);
    }

    private void applyFilter() {
        String keyword = _keywordField.getText() == null ? ""
                : _keywordField.getText().trim().toLowerCase(Locale.ROOT);
        String filter = _filterGroup.getSelectedToggle() == null ? "all"
                : String.valueOf(_filterGroup.getSelectedToggle().getUserData());
        List<CourseGroup> visible = _allCourses.stream()
                .filter(group -> matchesKeyword(group, keyword))
                .filter(group -> matchesFilter(group, filter))
                .toList();
        renderCourses(visible);
        long classCount = visible.stream().mapToLong(group -> group.classes().size()).sum();
        _statusLabel.setText("显示 " + visible.size() + " 门课程、" + classCount
                + " 个教学班；共 " + _allCourses.size() + " 门课程");
    }

    private boolean matchesKeyword(CourseGroup group, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        Course course = group.course();
        return contains(course.getCourseId(), keyword)
                || contains(course.getCourseName(), keyword)
                || contains(course.getCourseNature(), keyword)
                || contains(course.getOpeningUnit(), keyword)
                || group.classes().stream().anyMatch(row ->
                contains(row.teachingClass().getTeacher(), keyword)
                        || contains(row.teachingClass().getClassNumber(), keyword)
                        || contains(row.teachingClass().getTeachingClassId(), keyword));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesFilter(CourseGroup group, String filter) {
        return switch (filter) {
            case "available" -> group.classes().stream()
                    .anyMatch(row -> row.availability().state() == State.AVAILABLE);
            case "selected" -> group.selected();
            case "conflict" -> group.classes().stream()
                    .anyMatch(row -> row.availability().state() == State.CONFLICT);
            default -> true;
        };
    }

    private void renderCourses(List<CourseGroup> groups) {
        _courseList.getChildren().clear();
        if (groups.isEmpty()) {
            VBox empty = new VBox(6,
                    styledLabel("没有符合条件的课程", "section-title"),
                    styledLabel("请调整关键词或状态筛选后重试", "empty-state-label"));
            empty.getStyleClass().add("catalog-empty");
            _courseList.getChildren().add(empty);
            return;
        }
        groups.forEach(group -> _courseList.getChildren().add(courseGroupView(group)));
    }

    private VBox courseGroupView(CourseGroup group) {
        String courseId = group.course().getCourseId();
        VBox classes = new VBox();
        classes.getStyleClass().add("teaching-class-list");
        boolean expanded = _expandedCourseIds.contains(courseId);
        classes.setVisible(expanded);
        classes.setManaged(expanded);
        if (group.classes().isEmpty()) {
            Label empty = new Label("当前课程尚未创建教学班");
            empty.getStyleClass().add("teaching-class-empty");
            classes.getChildren().add(empty);
        } else {
            group.classes().forEach(row -> classes.getChildren().add(teachingClassView(row)));
        }

        GridPane summary = courseColumns();
        summary.getStyleClass().add("course-summary-row");
        summary.add(styledLabel(group.course().getCourseId(), "course-code"), 0, 0);
        summary.add(styledLabel(group.course().getCourseName(), "course-name"), 1, 0);
        summary.add(styledLabel(group.classes().size() + " 个", "course-meta"), 2, 0);
        summary.add(styledLabel(CourseViewSupport.safe(
                group.course().getCourseNature(), "—"), "course-meta"), 3, 0);
        summary.add(styledLabel(CourseViewSupport.safe(
                group.course().getOpeningUnit(), "—"), "course-meta"), 4, 0);
        summary.add(styledLabel(group.course().getCredit() + "", "course-meta"), 5, 0);
        Availability courseStatus = group.status();
        Label status = styledLabel(courseStatus.label(), "status-chip");
        status.getStyleClass().add(courseStatus.state().styleClass());
        summary.add(status, 6, 0);
        Button expand = new Button(expanded ? "收起" : "展开");
        expand.getStyleClass().add("course-expand-button");
        expand.setOnAction(event -> {
            boolean show = !classes.isVisible();
            classes.setVisible(show);
            classes.setManaged(show);
            expand.setText(show ? "收起" : "展开");
            if (show) {
                _expandedCourseIds.add(courseId);
            } else {
                _expandedCourseIds.remove(courseId);
            }
        });
        summary.add(expand, 7, 0);

        VBox groupBox = new VBox(summary, classes);
        groupBox.getStyleClass().add("course-group");
        return groupBox;
    }

    private GridPane teachingClassView(TeachingClassRow row) {
        TeachingClass teachingClass = row.teachingClass();
        GridPane grid = new GridPane();
        grid.getStyleClass().add("teaching-class-row");
        double[] widths = {19, 35, 13, 18, 15};
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width);
            column.setFillWidth(true);
            grid.getColumnConstraints().add(column);
        }

        Label teacher = styledLabel("[" + teachingClass.getClassNumber() + "]  "
                + CourseViewSupport.safe(teachingClass.getTeacher(), "教师待定"),
                "teaching-class-title");
        Label classId = styledLabel(teachingClass.getTeachingClassId(), "teaching-class-id");
        String language = CourseViewSupport.safe(teachingClass.getTeachingLanguage(), "");
        VBox identity = new VBox(3, teacher, classId);
        if (!language.isBlank()) {
            identity.getChildren().add(styledLabel(language, "teaching-class-id"));
        }
        grid.add(identity, 0, 0);
        grid.add(scheduleView(row.schedules()), 1, 0);

        int capacity = teachingClass.getCapacity();
        int selected = teachingClass.getSelectedCount();
        double progress = capacity <= 0 ? 1.0 : Math.min(1.0, (double) selected / capacity);
        ProgressBar capacityBar = new ProgressBar(progress);
        capacityBar.setMaxWidth(Double.MAX_VALUE);
        capacityBar.getStyleClass().add("capacity-bar");
        Label capacityText = styledLabel(selected + " / " + capacity + " 人",
                "teaching-class-meta");
        VBox capacityBox = new VBox(5, capacityText, capacityBar);
        grid.add(capacityBox, 2, 0);

        Availability availability = row.availability();
        Label chip = styledLabel(availability.label(), "status-chip");
        chip.getStyleClass().add(availability.state().styleClass());
        Label reason = styledLabel(availability.reason(), "status-reason");
        reason.setWrapText(true);
        grid.add(new VBox(5, chip, reason), 3, 0);

        Button action = new Button(availability.state().buttonText());
        action.getStyleClass().addAll("table-action", availability.state().buttonClass());
        action.setDisable(availability.state() != State.AVAILABLE);
        action.setOnAction(event -> selectCourse(row));
        HBox actionBox = new HBox(action);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(actionBox, 4, 0);
        return grid;
    }

    private VBox scheduleView(List<CourseSchedule> schedules) {
        VBox box = new VBox(3);
        if (schedules.isEmpty()) {
            box.getChildren().add(styledLabel("尚未排课", "schedule-empty"));
            return box;
        }
        for (CourseSchedule schedule : schedules) {
            Label line = new Label(scheduleLine(schedule));
            line.setWrapText(true);
            line.getStyleClass().add("schedule-line");
            box.getChildren().add(line);
        }
        return box;
    }

    private String scheduleLine(CourseSchedule schedule) {
        return schedule.getWeekStart() + "–" + schedule.getWeekEnd() + " 周  "
                + CourseViewSupport.dayName(schedule.getDayOfWeek()) + "  "
                + schedule.getStartPeriod() + "–" + schedule.getEndPeriod() + " 节  ·  "
                + CourseViewSupport.safe(schedule.getClassroom(), "教室待定");
    }

    private void selectCourse(TeachingClassRow row) {
        CourseViewSupport.runAsync(this,
                () -> _client.selectCourse(_studentId,
                        row.teachingClass().getTeachingClassId()), ignored -> {
                    _statusLabel.setText("已选：《" + row.course().getCourseName()
                            + "》" + row.teachingClass().getClassNumber() + " 班");
                    _expandedCourseIds.add(row.course().getCourseId());
                    _onEnrollmentChanged.run();
                });
    }

    private ToggleButton filterButton(String text, String value) {
        ToggleButton button = new ToggleButton(text);
        button.setUserData(value);
        button.setToggleGroup(_filterGroup);
        button.getStyleClass().add("filter-toggle");
        return button;
    }

    private Button button(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setOnAction(event -> action.run());
        return button;
    }

    private Label styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private record SnapshotData(List<Course> courses, List<TeachingClass> classes,
                                List<CourseSchedule> schedules,
                                List<SelectCourse> selections) {
    }

    private record CourseGroup(Course course, List<TeachingClassRow> classes) {
        boolean selected() {
            return classes.stream().anyMatch(row -> row.availability().state() == State.SELECTED);
        }

        Availability status() {
            return classes.stream().filter(row -> row.availability().state() == State.SELECTED)
                    .findFirst()
                    .map(row -> new Availability(State.SELECTED,
                            "已选 " + row.teachingClass().getClassNumber() + " 班"))
                    .orElseGet(() -> {
                        long available = classes.stream()
                                .filter(row -> row.availability().state() == State.AVAILABLE).count();
                        if (available > 0) {
                            return new Availability(State.AVAILABLE, available + " 个班可选");
                        }
                        if (classes.isEmpty()) {
                            return new Availability(State.UNAVAILABLE, "暂无教学班");
                        }
                        if (classes.stream().allMatch(row ->
                                row.availability().state() == State.FULL)) {
                            return new Availability(State.FULL, "全部已满");
                        }
                        if (classes.stream().anyMatch(row ->
                                row.availability().state() == State.CONFLICT)) {
                            return new Availability(State.CONFLICT, "时间冲突");
                        }
                        return new Availability(State.UNAVAILABLE, "当前不可选");
                    });
        }
    }

    private record TeachingClassRow(Course course, TeachingClass teachingClass,
                                    List<CourseSchedule> schedules,
                                    Availability availability) {
    }

    private record Availability(State state, String reason) {
        String label() {
            return switch (state) {
                case AVAILABLE -> "可选";
                case SELECTED -> "已选";
                case SAME_COURSE -> "同课已选";
                case FULL -> "已满";
                case CONFLICT -> "时间冲突";
                case UNAVAILABLE -> "不可选";
            };
        }
    }

    private enum State {
        AVAILABLE("state-available", "选择", "primary"),
        SELECTED("state-selected", "已选", "success"),
        SAME_COURSE("state-unavailable", "不可重复", "secondary"),
        FULL("state-full", "已满", "warning"),
        CONFLICT("state-conflict", "时间冲突", "danger"),
        UNAVAILABLE("state-unavailable", "不可选", "secondary");

        private final String _styleClass;
        private final String _buttonText;
        private final String _buttonClass;

        State(String styleClass, String buttonText, String buttonClass) {
            _styleClass = styleClass;
            _buttonText = buttonText;
            _buttonClass = buttonClass;
        }

        String styleClass() {
            return _styleClass;
        }

        String buttonText() {
            return _buttonText;
        }

        String buttonClass() {
            return _buttonClass;
        }
    }
}
