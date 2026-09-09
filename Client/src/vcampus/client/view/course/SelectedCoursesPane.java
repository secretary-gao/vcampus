/*
 * SelectedCoursesPane
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 学生“我的课程”页面：展示已选课程、上课安排并支持行内退课。 */
public class SelectedCoursesPane extends VBox {

    private final ICourseClientSrv _client;
    private final String _studentId;
    private final Runnable _onEnrollmentChanged;
    private final TableView<SelectedCourseRow> _courseTable = new TableView<>();
    private final Label _statusLabel = new Label();

    /** 创建已选课程页面。 */
    public SelectedCoursesPane(ICourseClientSrv client, String studentId,
                               Runnable onEnrollmentChanged) {
        this._client = client;
        this._studentId = studentId;
        this._onEnrollmentChanged = onEnrollmentChanged;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    /** 从服务器刷新已选课程与对应排课。 */
    public void refresh() {
        _statusLabel.setText("正在读取已选课程…");
        CourseViewSupport.runAsync(this, this::loadRows, rows -> {
            _courseTable.setItems(FXCollections.observableArrayList(rows));
            _statusLabel.setText("已选 " + rows.size() + " 门课程");
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        Label title = new Label("我的课程");
        title.getStyleClass().add("page-title");
        Label description = new Label("查看本学期选课与上课地点，可在对应课程行直接退课");
        description.getStyleClass().add("page-description");
        VBox heading = new VBox(3, title, description);
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button refreshButton = new Button("刷新");
        refreshButton.getStyleClass().add("secondary");
        refreshButton.setOnAction(event -> refresh());
        HBox header = new HBox(12, heading, refreshButton);
        header.setAlignment(Pos.CENTER_LEFT);

        _courseTable.getColumns().addAll(
                CourseViewSupport.textColumn("课程号", 115,
                        row -> row.course().getCourseId()),
                CourseViewSupport.textColumn("教学班", 70,
                        row -> row.teachingClass().getClassNumber()),
                CourseViewSupport.textColumn("课程名称", 190,
                        row -> row.course().getCourseName()),
                CourseViewSupport.textColumn("教师", 110,
                        row -> row.teachingClass().getTeacher()),
                CourseViewSupport.textColumn("学分", 65,
                        row -> String.valueOf(row.course().getCredit())),
                CourseViewSupport.textColumn("时间 / 教室", 290,
                        SelectedCourseRow::scheduleSummary),
                actionColumn()
        );
        CourseViewSupport.configureTable(_courseTable, "暂无已选课程");
        VBox.setVgrow(_courseTable, Priority.ALWAYS);

        _statusLabel.getStyleClass().add("status-label");
        getChildren().addAll(header, _courseTable, _statusLabel);
    }

    private List<SelectedCourseRow> loadRows() throws Exception {
        List<SelectCourse> selections = _client.querySelectedCourse(_studentId);
        Map<String, Course> courses = _client.queryCourse("").stream()
                .collect(Collectors.toMap(Course::getCourseId, Function.identity(), (a, b) -> a));
        Map<String, TeachingClass> classes = _client.queryTeachingClass("").stream()
                .collect(Collectors.toMap(TeachingClass::getTeachingClassId,
                        Function.identity(), (a, b) -> a));
        Map<String, List<CourseSchedule>> schedules = _client.queryStudentSchedule(_studentId).stream()
                .collect(Collectors.groupingBy(CourseSchedule::getTeachingClassId));
        return selections.stream()
                .map(selection -> new SelectedCourseRow(
                        courses.get(selection.getCourseId()),
                        classes.get(selection.getTeachingClassId()),
                        formatSchedules(schedules.getOrDefault(
                                selection.getTeachingClassId(), List.of()))))
                .filter(row -> row.course() != null && row.teachingClass() != null)
                .sorted((left, right) -> left.course().getCourseId()
                        .compareTo(right.course().getCourseId()))
                .toList();
    }

    private String formatSchedules(List<CourseSchedule> schedules) {
        if (schedules.isEmpty()) {
            return "未排课";
        }
        return schedules.stream()
                .sorted((left, right) -> {
                    int byDay = Integer.compare(left.getDayOfWeek(), right.getDayOfWeek());
                    return byDay != 0 ? byDay : left.getStartTime().compareTo(right.getStartTime());
                })
                .map(schedule -> CourseViewSupport.dayName(schedule.getDayOfWeek()) + " "
                        + schedule.getWeekStart() + "-" + schedule.getWeekEnd() + " 周 "
                        + schedule.getStartPeriod() + "-" + schedule.getEndPeriod() + " 节"
                        + " · " + CourseViewSupport.safe(schedule.getClassroom(), "教室待定"))
                .collect(Collectors.joining("；"));
    }

    private TableColumn<SelectedCourseRow, Void> actionColumn() {
        TableColumn<SelectedCourseRow, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(95);
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

    private record SelectedCourseRow(Course course, TeachingClass teachingClass,
                                     String scheduleSummary) {
    }
}
