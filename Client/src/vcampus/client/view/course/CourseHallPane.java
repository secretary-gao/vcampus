/*
 * CourseHallPane
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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;

import java.util.List;

/** 学生课程大厅：检索课程、查看余量并发起选课。 */
public class CourseHallPane extends VBox {

    private final ICourseClientSrv _client;
    private final String _studentId;
    private final Runnable _onEnrollmentChanged;
    private final TextField _keywordField = new TextField();
    private final TableView<Course> _courseTable = new TableView<>();
    private final Label _statusLabel = new Label();

    /**
     * 创建课程大厅。
     *
     * @param client Course 客户端服务
     * @param studentId 当前学生学号
     * @param onEnrollmentChanged 选课成功后的刷新回调
     */
    public CourseHallPane(ICourseClientSrv client, String studentId,
                          Runnable onEnrollmentChanged) {
        this._client = client;
        this._studentId = studentId;
        this._onEnrollmentChanged = onEnrollmentChanged;
        setSpacing(12);
        setStyle("-fx-padding: 16;");
        buildView();
        refresh();
    }

    /** 从服务器刷新课程列表。 */
    public void refresh() {
        String keyword = _keywordField.getText();
        _statusLabel.setText("正在读取课程...");
        CourseViewSupport.runAsync(this, () -> _client.queryCourse(keyword), courses -> {
            _courseTable.setItems(FXCollections.observableArrayList(courses));
            _statusLabel.setText("共 " + courses.size() + " 门课程");
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        _keywordField.setPromptText("按课程号、课程名或教师搜索");
        HBox.setHgrow(_keywordField, Priority.ALWAYS);
        Button searchButton = new Button("查询");
        searchButton.setStyle(CourseViewSupport.PRIMARY_BUTTON);
        searchButton.setOnAction(event -> refresh());
        _keywordField.setOnAction(event -> refresh());
        HBox searchBar = new HBox(10, _keywordField, searchButton);

        _courseTable.getColumns().addAll(
                CourseViewSupport.textColumn("课程号", 145, Course::getCourseId),
                CourseViewSupport.textColumn("课程名称", 210, Course::getCourseName),
                CourseViewSupport.textColumn("教师", 130, Course::getTeacher),
                CourseViewSupport.textColumn("学分", 75, c -> String.valueOf(c.getCredit())),
                CourseViewSupport.textColumn("已选 / 容量", 115,
                        c -> c.getSelectedCount() + " / " + c.getCapacity())
        );
        _courseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(_courseTable, Priority.ALWAYS);

        Button selectButton = new Button("选择课程");
        selectButton.setStyle(CourseViewSupport.PRIMARY_BUTTON);
        selectButton.setOnAction(event -> selectCourse());
        HBox actions = new HBox(12, selectButton, _statusLabel);
        getChildren().addAll(searchBar, _courseTable, actions);
    }

    private void selectCourse() {
        Course selected = _courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            CourseViewSupport.showError(new IllegalArgumentException("请先选择一门课程"));
            return;
        }
        CourseViewSupport.runAsync(this,
                () -> _client.selectCourse(_studentId, selected.getCourseId()), ignored -> {
                    _statusLabel.setText("已选：" + selected.getCourseName());
                    refresh();
                    _onEnrollmentChanged.run();
                });
    }
}
