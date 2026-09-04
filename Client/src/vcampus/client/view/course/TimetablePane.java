/*
 * TimetablePane
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 学生课程表页面，仅显示当前学生已选课程对应的排课。 */
public class TimetablePane extends VBox {

    private final ICourseClientSrv _client;
    private final String _studentId;
    private final TableView<TimetableRow> _table = new TableView<>();
    private final Label _statusLabel = new Label();

    /** 创建学生课程表页面。 */
    public TimetablePane(ICourseClientSrv client, String studentId) {
        this._client = client;
        this._studentId = studentId;
        setSpacing(12);
        setStyle("-fx-padding: 16;");
        buildView();
        refresh();
    }

    /** 从服务器刷新当前学生课程表。 */
    public void refresh() {
        _statusLabel.setText("正在读取课程表...");
        CourseViewSupport.runAsync(this, this::loadRows, rows -> {
            _table.setItems(FXCollections.observableArrayList(rows));
            _statusLabel.setText("共 " + rows.size() + " 个上课时段");
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        _table.getColumns().addAll(
                CourseViewSupport.textColumn("星期", 100,
                        row -> CourseViewSupport.dayName(row.schedule().getDayOfWeek())),
                CourseViewSupport.textColumn("时间", 145,
                        row -> CourseViewSupport.timeRange(
                                row.schedule().getStartTime(), row.schedule().getEndTime())),
                CourseViewSupport.textColumn("课程号", 135,
                        row -> row.schedule().getCourseId()),
                CourseViewSupport.textColumn("课程名称", 190,
                        row -> row.course() == null ? "未知课程" : row.course().getCourseName()),
                CourseViewSupport.textColumn("教师", 125,
                        row -> row.course() == null ? "—" : row.course().getTeacher()),
                CourseViewSupport.textColumn("教室", 125,
                        row -> row.schedule().getClassroom())
        );
        _table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(_table, Priority.ALWAYS);
        Button refreshButton = new Button("刷新课表");
        refreshButton.setStyle(CourseViewSupport.SECONDARY_BUTTON);
        refreshButton.setOnAction(event -> refresh());
        getChildren().addAll(_table, new HBox(10, refreshButton, _statusLabel));
    }

    private List<TimetableRow> loadRows() throws Exception {
        List<CourseSchedule> schedules = _client.queryStudentSchedule(_studentId);
        Map<String, Course> courses = _client.queryCourse("").stream()
                .collect(Collectors.toMap(Course::getCourseId, Function.identity()));
        return schedules.stream()
                .map(schedule -> new TimetableRow(schedule, courses.get(schedule.getCourseId())))
                .toList();
    }

    /** 课程表展示行，将排课与课程主数据在客户端只读组合。 */
    private record TimetableRow(CourseSchedule schedule, Course course) {
    }
}
