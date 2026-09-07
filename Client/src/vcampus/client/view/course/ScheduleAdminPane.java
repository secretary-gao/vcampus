/*
 * ScheduleAdminPane
 *
 * Version 1.0
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 教务管理员排课页面，支持排课查询、新增、修改、删除及冲突提示。 */
public class ScheduleAdminPane extends VBox {

    private final ICourseClientSrv _client;
    private final TableView<ScheduleRow> _table = new TableView<>();
    private final ComboBox<Course> _courseBox = new ComboBox<>();
    private final ComboBox<Integer> _dayBox = new ComboBox<>();
    private final TextField _classroomField = new TextField();
    private final TextField _startField = new TextField("08:00");
    private final TextField _endField = new TextField("09:40");
    private final Label _statusLabel = new Label();

    /** 创建管理员排课页面。 */
    public ScheduleAdminPane(ICourseClientSrv client) {
        this._client = client;
        setSpacing(12);
        setPadding(new Insets(16));
        buildView();
        refresh();
    }

    /** 从服务器刷新课程主数据与排课。 */
    public void refresh() {
        _statusLabel.setText("正在读取排课...");
        CourseViewSupport.runAsync(this, this::loadSnapshot, snapshot -> {
            _courseBox.setItems(FXCollections.observableArrayList(snapshot.courses()));
            _table.setItems(FXCollections.observableArrayList(snapshot.rows()));
            _statusLabel.setText("共 " + snapshot.rows().size() + " 条排课");
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        _table.getColumns().addAll(
                CourseViewSupport.textColumn("课程", 200, row -> row.displayCourse()),
                CourseViewSupport.textColumn("教师", 120, row -> row.teacher()),
                CourseViewSupport.textColumn("教室", 115,
                        row -> row.schedule().getClassroom()),
                CourseViewSupport.textColumn("星期", 90,
                        row -> CourseViewSupport.dayName(row.schedule().getDayOfWeek())),
                CourseViewSupport.textColumn("时间", 145,
                        row -> CourseViewSupport.timeRange(
                                row.schedule().getStartTime(), row.schedule().getEndTime()))
        );
        _table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        _table.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, selected) -> populateForm(selected));
        VBox.setVgrow(_table, Priority.ALWAYS);

        _courseBox.setMaxWidth(Double.MAX_VALUE);
        _courseBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Course course) {
                return course == null ? "" : course.getCourseId() + " · "
                        + course.getCourseName() + " · " + course.getTeacher();
            }

            @Override
            public Course fromString(String value) {
                return null;
            }
        });
        _dayBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7));
        _dayBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer day) {
                return day == null ? "" : CourseViewSupport.dayName(day);
            }

            @Override
            public Integer fromString(String value) {
                return null;
            }
        });
        _classroomField.setPromptText("例如：教一-101");
        _startField.setPromptText("HH:mm");
        _endField.setPromptText("HH:mm");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("课程"), _courseBox, new Label("教室"), _classroomField);
        form.addRow(1, new Label("星期"), _dayBox, new Label("时间"),
                new HBox(6, _startField, new Label("至"), _endField));

        Button addButton = button("新增", CourseViewSupport.PRIMARY_BUTTON, this::addSchedule);
        Button updateButton = button("保存修改", CourseViewSupport.SECONDARY_BUTTON, this::updateSchedule);
        Button deleteButton = button("删除", CourseViewSupport.DANGER_BUTTON, this::deleteSchedule);
        Button clearButton = button("清空表单", CourseViewSupport.SECONDARY_BUTTON, this::clearForm);
        Button refreshButton = button("刷新", CourseViewSupport.SECONDARY_BUTTON, this::refresh);
        getChildren().addAll(_table, form,
                new HBox(9, addButton, updateButton, deleteButton, clearButton, refreshButton,
                        _statusLabel));
    }

    private Snapshot loadSnapshot() throws Exception {
        List<Course> courses = _client.queryCourse("");
        Map<String, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getCourseId, Function.identity()));
        List<ScheduleRow> rows = _client.querySchedule().stream()
                .map(schedule -> new ScheduleRow(schedule, courseMap.get(schedule.getCourseId())))
                .toList();
        return new Snapshot(courses, rows);
    }

    private void addSchedule() {
        CourseSchedule schedule;
        try {
            schedule = formSchedule(null);
        } catch (RuntimeException e) {
            CourseViewSupport.showError(e);
            return;
        }
        CourseViewSupport.runAsync(this, () -> _client.addSchedule(schedule), added -> {
            _statusLabel.setText("排课新增成功：" + added.getScheduleId());
            clearForm();
            refresh();
        });
    }

    private void updateSchedule() {
        ScheduleRow selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            CourseViewSupport.showError(new IllegalArgumentException("请先选择一条排课"));
            return;
        }
        CourseSchedule schedule;
        try {
            schedule = formSchedule(selected.schedule().getScheduleId());
        } catch (RuntimeException e) {
            CourseViewSupport.showError(e);
            return;
        }
        CourseViewSupport.runAsync(this, () -> _client.updateSchedule(schedule), ignored -> {
            _statusLabel.setText("排课修改成功");
            refresh();
        });
    }

    private void deleteSchedule() {
        ScheduleRow selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            CourseViewSupport.showError(new IllegalArgumentException("请先选择一条排课"));
            return;
        }
        CourseViewSupport.runAsync(this,
                () -> _client.deleteSchedule(selected.schedule().getScheduleId()), ignored -> {
                    _statusLabel.setText("排课删除成功");
                    clearForm();
                    refresh();
                });
    }

    private CourseSchedule formSchedule(String scheduleId) {
        Course course = _courseBox.getValue();
        Integer day = _dayBox.getValue();
        if (course == null || day == null || _classroomField.getText().isBlank()) {
            throw new IllegalArgumentException("课程、教室和星期均不能为空");
        }
        LocalTime start = CourseViewSupport.parseTime(_startField.getText());
        LocalTime end = CourseViewSupport.parseTime(_endField.getText());
        return new CourseSchedule(scheduleId, course.getCourseId(),
                _classroomField.getText().trim(), day, start, end);
    }

    private void populateForm(ScheduleRow row) {
        if (row == null) {
            return;
        }
        _courseBox.setValue(row.course());
        _classroomField.setText(row.schedule().getClassroom());
        _dayBox.setValue(row.schedule().getDayOfWeek());
        _startField.setText(row.schedule().getStartTime().toString());
        _endField.setText(row.schedule().getEndTime().toString());
    }

    private void clearForm() {
        _table.getSelectionModel().clearSelection();
        _courseBox.setValue(null);
        _dayBox.setValue(null);
        _classroomField.clear();
        _startField.setText("08:00");
        _endField.setText("09:40");
    }

    private Button button(String text, String style, Runnable action) {
        Button button = new Button(text);
        button.setStyle(style);
        button.setOnAction(event -> action.run());
        return button;
    }

    /** 页面一次刷新所需的课程与排课快照。 */
    private record Snapshot(List<Course> courses, List<ScheduleRow> rows) {
    }

    /** 管理表格展示行。 */
    private record ScheduleRow(CourseSchedule schedule, Course course) {
        String displayCourse() {
            return course == null ? schedule.getCourseId()
                    : course.getCourseId() + " · " + course.getCourseName();
        }

        String teacher() {
            return course == null ? "—" : course.getTeacher();
        }
    }
}
