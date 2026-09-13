/*
 * SelectedCoursesPane
 *
 * Version 2.0
 *
 * 2026-09-13
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.SelectCourse;
import vcampus.common.vo.TeachingClass;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 学生“我的课程”页面：汇总已选课程、教学班与全部上课安排。 */
public class SelectedCoursesPane extends VBox {

    private final ICourseClientSrv _client;
    private final String _studentId;
    private final Runnable _onEnrollmentChanged;
    private final Runnable _openCourseHall;
    private final TableView<SelectedCourseRow> _courseTable = new TableView<>();
    private final Label _courseCountValue = new Label("—");
    private final Label _creditValue = new Label("—");
    private final Label _scheduleCountValue = new Label("—");
    private final Label _statusLabel = new Label();

    /** 创建已选课程页面。 */
    public SelectedCoursesPane(ICourseClientSrv client, String studentId,
                               Runnable onEnrollmentChanged) {
        this(client, studentId, onEnrollmentChanged, null);
    }

    /** 创建已选课程页面，并可提供返回课程大厅的导航动作。 */
    public SelectedCoursesPane(ICourseClientSrv client, String studentId,
                               Runnable onEnrollmentChanged, Runnable openCourseHall) {
        this._client = client;
        this._studentId = studentId;
        this._onEnrollmentChanged = onEnrollmentChanged;
        this._openCourseHall = openCourseHall;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    /** 从服务器刷新已选课程、教学班与对应排课。 */
    public void refresh() {
        _statusLabel.setText("正在读取已选课程…");
        CourseViewSupport.runAsync(this, this::loadRows, rows -> {
            _courseTable.setItems(FXCollections.observableArrayList(rows));
            _courseCountValue.setText(String.valueOf(rows.size()));
            _creditValue.setText(String.valueOf(rows.stream()
                    .mapToInt(row -> row.course().getCredit()).sum()));
            _scheduleCountValue.setText(String.valueOf(rows.stream()
                    .mapToInt(row -> row.schedules().size()).sum()));
            _statusLabel.setText("已选 " + rows.size() + " 门课程");
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        Label title = new Label("我的课程");
        title.getStyleClass().add("page-title");
        Label description = new Label("本学期已选课程、教学班及上课安排");
        description.getStyleClass().add("page-description");
        VBox heading = new VBox(3, title, description);
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button hallButton = new Button("返回课程大厅");
        hallButton.getStyleClass().add("secondary");
        hallButton.setDisable(_openCourseHall == null);
        hallButton.setOnAction(event -> {
            if (_openCourseHall != null) {
                _openCourseHall.run();
            }
        });
        Button refreshButton = new Button("刷新");
        refreshButton.getStyleClass().add("primary");
        refreshButton.setOnAction(event -> refresh());
        HBox header = new HBox(9, heading, hallButton, refreshButton);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox metrics = new HBox(12,
                metric("已选课程", _courseCountValue, " 门"),
                metric("本学期学分", _creditValue, " 分"),
                metric("上课时段", _scheduleCountValue, " 段"));

        _courseTable.getColumns().addAll(
                CourseViewSupport.textColumn("课程号", 108,
                        row -> row.course().getCourseId()),
                CourseViewSupport.textColumn("课程名称", 190,
                        row -> row.course().getCourseName()),
                CourseViewSupport.textColumn("教学班", 82,
                        row -> row.teachingClass().getClassNumber() + " 班"),
                CourseViewSupport.textColumn("教师", 105,
                        row -> row.teachingClass().getTeacher()),
                CourseViewSupport.wrappingTextColumn("上课时间 / 地点", 315,
                        SelectedCourseRow::scheduleSummary),
                CourseViewSupport.textColumn("学分", 58,
                        row -> String.valueOf(row.course().getCredit())),
                CourseViewSupport.textColumn("选课时间", 132,
                        row -> CourseViewSupport.dateTime(row.selection().getSelectTime())),
                actionColumn()
        );
        CourseViewSupport.configureTable(_courseTable, "暂无已选课程，可返回课程大厅选课");
        _courseTable.getStyleClass().add("selected-course-table");
        _courseTable.setFixedCellSize(-1);
        VBox.setVgrow(_courseTable, Priority.ALWAYS);

        _statusLabel.getStyleClass().add("status-label");
        getChildren().addAll(header, metrics, _courseTable, _statusLabel);
    }

    private List<SelectedCourseRow> loadRows() throws Exception {
        List<SelectCourse> selections = _client.querySelectedCourse(_studentId);
        Map<String, Course> courses = _client.queryCourse("").stream()
                .collect(Collectors.toMap(Course::getCourseId, Function.identity(), (a, b) -> a));
        Map<String, TeachingClass> classes = _client.queryTeachingClass("").stream()
                .collect(Collectors.toMap(TeachingClass::getTeachingClassId,
                        Function.identity(), (a, b) -> a));
        Map<String, List<CourseSchedule>> schedules = _client.queryStudentSchedule(_studentId)
                .stream().collect(Collectors.groupingBy(CourseSchedule::getTeachingClassId));
        return selections.stream()
                .map(selection -> {
                    List<CourseSchedule> classSchedules = schedules.getOrDefault(
                            selection.getTeachingClassId(), List.of()).stream()
                            .sorted(scheduleOrder()).toList();
                    return new SelectedCourseRow(selection,
                            courses.get(selection.getCourseId()),
                            classes.get(selection.getTeachingClassId()), classSchedules,
                            formatSchedules(classSchedules));
                })
                .filter(row -> row.course() != null && row.teachingClass() != null)
                .sorted(Comparator.comparing(row -> row.course().getCourseId()))
                .toList();
    }

    private Comparator<CourseSchedule> scheduleOrder() {
        return Comparator.comparingInt(CourseSchedule::getDayOfWeek)
                .thenComparingInt(CourseSchedule::getWeekStart)
                .thenComparingInt(CourseSchedule::getStartPeriod);
    }

    private String formatSchedules(List<CourseSchedule> schedules) {
        if (schedules.isEmpty()) {
            return "未排课";
        }
        return schedules.stream()
                .map(schedule -> schedule.getWeekStart() + "–" + schedule.getWeekEnd() + " 周  "
                        + CourseViewSupport.dayName(schedule.getDayOfWeek()) + "  "
                        + schedule.getStartPeriod() + "–" + schedule.getEndPeriod() + " 节  ·  "
                        + CourseViewSupport.safe(schedule.getClassroom(), "教室待定"))
                .collect(Collectors.joining("\n"));
    }

    private TableColumn<SelectedCourseRow, Void> actionColumn() {
        TableColumn<SelectedCourseRow, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(82);
        column.setSortable(false);
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Button _button = new Button("退课");

            {
                _button.getStyleClass().addAll("danger", "table-action");
                _button.setOnAction(event -> {
                    SelectedCourseRow row = getTableRow().getItem();
                    if (row != null) {
                        dropCourse(row);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : _button);
            }
        });
        return column;
    }

    private void dropCourse(SelectedCourseRow row) {
        Course course = row.course();
        if (!CourseViewSupport.confirm("退选“" + course.getCourseName() + "”？",
                "退课成功后，课程大厅、我的课程和我的课表会同步更新。")) {
            return;
        }
        CourseViewSupport.runAsync(this,
                () -> _client.dropCourse(_studentId,
                        row.teachingClass().getTeachingClassId()), ignored -> {
                    _statusLabel.setText("已退选：" + course.getCourseName());
                    _onEnrollmentChanged.run();
                });
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

    private record SelectedCourseRow(SelectCourse selection, Course course,
                                     TeachingClass teachingClass,
                                     List<CourseSchedule> schedules,
                                     String scheduleSummary) {
    }
}
