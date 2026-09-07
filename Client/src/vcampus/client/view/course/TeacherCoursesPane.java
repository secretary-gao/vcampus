/*
 * TeacherCoursesPane
 *
 * Version 1.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.TeacherCourseEnrollment;

import java.time.format.DateTimeFormatter;

/** 教师查看本人所授课程及选课学生名单的只读页面。 */
public class TeacherCoursesPane extends VBox {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ICourseClientSrv _client;
    private final String _teacherName;
    private final TableView<TeacherCourseEnrollment> _table = new TableView<>();
    private final Label _statusLabel = new Label();

    /** 创建教师课程名单页面。 */
    public TeacherCoursesPane(ICourseClientSrv client, String teacherName) {
        this._client = client;
        this._teacherName = teacherName;
        setSpacing(12);
        setPadding(new Insets(16));
        buildView();
        refresh();
    }

    /** 从服务器刷新教师本人课程名单。 */
    public void refresh() {
        _statusLabel.setText("正在读取本人课程...");
        CourseViewSupport.runAsync(this,
                () -> _client.queryTeacherCourseEnrollments(_teacherName), rows -> {
                    _table.setItems(FXCollections.observableArrayList(rows));
                    long courseCount = rows.stream()
                            .map(TeacherCourseEnrollment::getCourseId).distinct().count();
                    long studentCount = rows.stream()
                            .filter(row -> row.getStudentId() != null).count();
                    _statusLabel.setText("共 " + courseCount + " 门课程，"
                            + studentCount + " 条选课记录");
                });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        _table.getColumns().addAll(
                CourseViewSupport.textColumn("课程号", 105,
                        TeacherCourseEnrollment::getCourseId),
                CourseViewSupport.textColumn("课程名称", 170,
                        TeacherCourseEnrollment::getCourseName),
                CourseViewSupport.textColumn("学号", 110,
                        row -> display(row.getStudentId(), "暂无学生")),
                CourseViewSupport.textColumn("姓名", 90,
                        row -> display(row.getStudentName(), "—")),
                CourseViewSupport.textColumn("班级", 130,
                        row -> display(row.getClassName(), "—")),
                CourseViewSupport.textColumn("专业", 160,
                        row -> display(row.getMajor(), "—")),
                CourseViewSupport.textColumn("选课时间", 145,
                        row -> row.getSelectTime() == null ? "—"
                                : row.getSelectTime().format(DATE_TIME_FORMAT))
        );
        _table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(_table, Priority.ALWAYS);

        Button refreshButton = new Button("刷新");
        refreshButton.setStyle(CourseViewSupport.SECONDARY_BUTTON);
        refreshButton.setOnAction(event -> refresh());
        getChildren().addAll(new Label("授课教师：" + display(_teacherName, "未设置")),
                _table, new HBox(9, refreshButton, _statusLabel));
    }

    private String display(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
