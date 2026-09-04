/*
 * SelectedCoursesPane
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
import vcampus.common.vo.SelectCourse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 学生“我的课程”页面：展示已选课程并支持退课。 */
public class SelectedCoursesPane extends VBox {

    private final ICourseClientSrv _client;
    private final String _studentId;
    private final Runnable _onEnrollmentChanged;
    private final TableView<Course> _courseTable = new TableView<>();
    private final Label _statusLabel = new Label();

    /** 创建已选课程页面。 */
    public SelectedCoursesPane(ICourseClientSrv client, String studentId,
                               Runnable onEnrollmentChanged) {
        this._client = client;
        this._studentId = studentId;
        this._onEnrollmentChanged = onEnrollmentChanged;
        setSpacing(12);
        setStyle("-fx-padding: 16;");
        buildView();
        refresh();
    }

    /** 从服务器刷新已选课程。 */
    public void refresh() {
        _statusLabel.setText("正在读取已选课程...");
        CourseViewSupport.runAsync(this, this::loadSelectedCourses, courses -> {
            _courseTable.setItems(FXCollections.observableArrayList(courses));
            _statusLabel.setText("已选 " + courses.size() + " 门课程");
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        _courseTable.getColumns().addAll(
                CourseViewSupport.textColumn("课程号", 150, Course::getCourseId),
                CourseViewSupport.textColumn("课程名称", 230, Course::getCourseName),
                CourseViewSupport.textColumn("教师", 150, Course::getTeacher),
                CourseViewSupport.textColumn("学分", 90, c -> String.valueOf(c.getCredit())),
                CourseViewSupport.textColumn("人数", 110,
                        c -> c.getSelectedCount() + " / " + c.getCapacity())
        );
        _courseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(_courseTable, Priority.ALWAYS);

        Button refreshButton = new Button("刷新");
        refreshButton.setStyle(CourseViewSupport.SECONDARY_BUTTON);
        refreshButton.setOnAction(event -> refresh());
        Button dropButton = new Button("退选课程");
        dropButton.setStyle(CourseViewSupport.DANGER_BUTTON);
        dropButton.setOnAction(event -> dropCourse());
        getChildren().addAll(_courseTable, new HBox(10, refreshButton, dropButton, _statusLabel));
    }

    private List<Course> loadSelectedCourses() throws Exception {
        List<SelectCourse> selected = _client.querySelectedCourse(_studentId);
        Set<String> ids = selected.stream().map(SelectCourse::getCourseId).collect(Collectors.toSet());
        return _client.queryCourse("").stream()
                .filter(course -> ids.contains(course.getCourseId()))
                .toList();
    }

    private void dropCourse() {
        Course selected = _courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            CourseViewSupport.showError(new IllegalArgumentException("请先选择要退选的课程"));
            return;
        }
        CourseViewSupport.runAsync(this,
                () -> _client.dropCourse(_studentId, selected.getCourseId()), ignored -> {
                    _statusLabel.setText("已退选：" + selected.getCourseName());
                    refresh();
                    _onEnrollmentChanged.run();
                });
    }
}
