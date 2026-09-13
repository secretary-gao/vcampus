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
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.common.vo.TeachingClass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 教师工作台：按本人授课课程切换查看选课学生名单。 */
public class TeacherCoursesPane extends VBox {

    private final ICourseClientSrv _client;
    private final String _teacherName;
    private final ListView<TeacherCourse> _courseList = new ListView<>();
    private final TableView<TeacherCourseEnrollment> _rosterTable = new TableView<>();
    private final Label _courseCountValue = new Label("—");
    private final Label _studentCountValue = new Label("—");
    private final Label _currentCountValue = new Label("—");
    private final Label _rosterTitle = new Label("选课学生");
    private final Label _detailTitle = new Label("请选择教学班");
    private final Label _detailMeta = new Label("课程安排与容量将在这里显示");
    private final Label _statusLabel = new Label();

    /** 创建教师课程名单页面。 */
    public TeacherCoursesPane(ICourseClientSrv client, String teacherName) {
        this._client = client;
        this._teacherName = teacherName;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    /** 从服务器刷新教师本人课程及名单。 */
    public void refresh() {
        _statusLabel.setText("正在读取本人课程…");
        CourseViewSupport.runAsync(this, this::loadSnapshot, snapshot -> {
            _courseList.setItems(FXCollections.observableArrayList(snapshot.courses()));
            _courseCountValue.setText(String.valueOf(snapshot.courses().size()));
            _studentCountValue.setText(String.valueOf(snapshot.totalStudents()));
            _statusLabel.setText("数据已更新");
            if (snapshot.courses().isEmpty()) {
                _rosterTitle.setText("选课学生");
                _currentCountValue.setText("0");
                _rosterTable.getItems().clear();
            } else {
                _courseList.getSelectionModel().selectFirst();
            }
        });
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
        HBox header = new HBox(12, heading, refreshButton);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox metrics = new HBox(12,
                metric("本学期授课", _courseCountValue, " 门"),
                metric("选课记录", _studentCountValue, " 人次"),
                metric("当前课程选课", _currentCountValue, " 人"));

        Label listTitle = new Label("我教的课程");
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
        VBox right = new VBox(10, _rosterTitle, detail, _rosterTable);
        right.getStyleClass().add("course-card");
        VBox.setVgrow(_rosterTable, Priority.ALWAYS);

        SplitPane workspace = new SplitPane(left, right);
        workspace.setDividerPositions(0.30);
        workspace.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(workspace, Priority.ALWAYS);

        _statusLabel.getStyleClass().add("status-label");
        getChildren().addAll(header, metrics, workspace, _statusLabel);
    }

    private Snapshot loadSnapshot() throws Exception {
        List<TeacherCourseEnrollment> enrollments =
                _client.queryTeacherCourseEnrollments(_teacherName);
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
            courses.put(entry.getKey(), new TeacherCourse(course, teachingClass,
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
        if (selected == null) {
            _rosterTitle.setText("选课学生");
            _detailTitle.setText("请选择教学班");
            _detailMeta.setText("课程安排与容量将在这里显示");
            _currentCountValue.setText("0");
            _rosterTable.getItems().clear();
            return;
        }
        _rosterTitle.setText(selected.course().getCourseName() + " · "
                + selected.classNumber() + " 班 · 选课学生");
        _detailTitle.setText(selected.course().getCourseId() + " · "
                + selected.course().getCourseName());
        _detailMeta.setText("教学班 " + selected.classNumber() + "  ·  容量 "
                + selected.capacity() + " 人  ·  " + scheduleSummary(selected.schedules()));
        _currentCountValue.setText(String.valueOf(selected.roster().size()));
        _rosterTable.setItems(FXCollections.observableArrayList(selected.roster()));
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

    private record TeacherCourse(Course course, TeachingClass teachingClass,
                                 String classNumber, List<CourseSchedule> schedules,
                                 List<TeacherCourseEnrollment> roster) {
        int capacity() {
            return teachingClass == null ? roster.size() : teachingClass.getCapacity();
        }
    }

    private record Snapshot(List<TeacherCourse> courses, int totalStudents) {
    }
}
