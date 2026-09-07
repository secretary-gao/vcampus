/*
 * CourseAdminPane
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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;

/** 教务管理员课程主数据页面，支持课程查询、新增、修改和删除。 */
public class CourseAdminPane extends VBox {

    private final ICourseClientSrv _client;
    private final Runnable _courseChanged;
    private final TableView<Course> _table = new TableView<>();
    private final TextField _courseIdField = new TextField();
    private final TextField _courseNameField = new TextField();
    private final TextField _teacherField = new TextField();
    private final TextField _creditField = new TextField();
    private final TextField _capacityField = new TextField();
    private final Label _statusLabel = new Label();

    /** 创建管理员课程管理页面。 */
    public CourseAdminPane(ICourseClientSrv client, Runnable courseChanged) {
        this._client = client;
        this._courseChanged = courseChanged;
        setSpacing(12);
        setPadding(new Insets(16));
        buildView();
        refresh();
    }

    /** 从服务器刷新课程主数据。 */
    public void refresh() {
        _statusLabel.setText("正在读取课程...");
        CourseViewSupport.runAsync(this, () -> _client.queryCourse(""), courses -> {
            _table.setItems(FXCollections.observableArrayList(courses));
            _statusLabel.setText("共 " + courses.size() + " 门课程");
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        _table.getColumns().addAll(
                CourseViewSupport.textColumn("课程号", 110, Course::getCourseId),
                CourseViewSupport.textColumn("课程名称", 200, Course::getCourseName),
                CourseViewSupport.textColumn("教师", 120, Course::getTeacher),
                CourseViewSupport.textColumn("学分", 75,
                        course -> String.valueOf(course.getCredit())),
                CourseViewSupport.textColumn("容量", 90,
                        course -> String.valueOf(course.getCapacity())),
                CourseViewSupport.textColumn("已选", 90,
                        course -> String.valueOf(course.getSelectedCount()))
        );
        _table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        _table.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> populateForm(selected));
        VBox.setVgrow(_table, Priority.ALWAYS);

        _courseIdField.setPromptText("例如：CSE1001");
        _courseNameField.setPromptText("课程名称");
        _teacherField.setPromptText("与教师账号姓名一致");
        _creditField.setPromptText("正整数");
        _capacityField.setPromptText("正整数");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("课程号"), _courseIdField,
                new Label("课程名称"), _courseNameField);
        form.addRow(1, new Label("授课教师"), _teacherField,
                new Label("学分"), _creditField);
        form.addRow(2, new Label("课程容量"), _capacityField);

        Button addButton = button("新增", CourseViewSupport.PRIMARY_BUTTON, this::addCourse);
        Button updateButton = button(
                "保存修改", CourseViewSupport.SECONDARY_BUTTON, this::updateCourse);
        Button deleteButton = button("删除", CourseViewSupport.DANGER_BUTTON, this::deleteCourse);
        Button clearButton = button("清空表单", CourseViewSupport.SECONDARY_BUTTON, this::clearForm);
        Button refreshButton = button("刷新", CourseViewSupport.SECONDARY_BUTTON, this::refresh);
        getChildren().addAll(_table, form,
                new HBox(9, addButton, updateButton, deleteButton, clearButton,
                        refreshButton, _statusLabel));
    }

    private void addCourse() {
        Course course;
        try {
            course = formCourse();
        } catch (RuntimeException e) {
            CourseViewSupport.showError(e);
            return;
        }
        CourseViewSupport.runAsync(this, () -> _client.addCourse(course), added -> {
            _statusLabel.setText("课程新增成功：" + added.getCourseId());
            clearForm();
            refreshAfterChange();
        });
    }

    private void updateCourse() {
        if (_table.getSelectionModel().getSelectedItem() == null) {
            CourseViewSupport.showError(new IllegalArgumentException("请先选择一门课程"));
            return;
        }
        Course course;
        try {
            course = formCourse();
        } catch (RuntimeException e) {
            CourseViewSupport.showError(e);
            return;
        }
        CourseViewSupport.runAsync(this, () -> _client.updateCourse(course), ignored -> {
            _statusLabel.setText("课程修改成功");
            refreshAfterChange();
        });
    }

    private void deleteCourse() {
        Course selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            CourseViewSupport.showError(new IllegalArgumentException("请先选择一门课程"));
            return;
        }
        CourseViewSupport.runAsync(this,
                () -> _client.deleteCourse(selected.getCourseId()), ignored -> {
                    _statusLabel.setText("课程删除成功");
                    clearForm();
                    refreshAfterChange();
                });
    }

    private Course formCourse() {
        String courseId = required(_courseIdField.getText(), "课程号");
        String courseName = required(_courseNameField.getText(), "课程名称");
        String teacher = required(_teacherField.getText(), "授课教师");
        int credit = positiveInt(_creditField.getText(), "学分");
        int capacity = positiveInt(_capacityField.getText(), "课程容量");
        return new Course(courseId, courseName, teacher, credit, capacity, 0);
    }

    private void populateForm(Course course) {
        if (course == null) {
            return;
        }
        _courseIdField.setText(course.getCourseId());
        _courseIdField.setDisable(true);
        _courseNameField.setText(course.getCourseName());
        _teacherField.setText(course.getTeacher());
        _creditField.setText(String.valueOf(course.getCredit()));
        _capacityField.setText(String.valueOf(course.getCapacity()));
    }

    private void clearForm() {
        _table.getSelectionModel().clearSelection();
        _courseIdField.setDisable(false);
        _courseIdField.clear();
        _courseNameField.clear();
        _teacherField.clear();
        _creditField.clear();
        _capacityField.clear();
    }

    private void refreshAfterChange() {
        refresh();
        if (_courseChanged != null) {
            _courseChanged.run();
        }
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private int positiveInt(String value, String fieldName) {
        try {
            int parsed = Integer.parseInt(required(value, fieldName));
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "必须为正整数");
        }
    }

    private Button button(String text, String style, Runnable action) {
        Button button = new Button(text);
        button.setStyle(style);
        button.setOnAction(event -> action.run());
        return button;
    }
}
