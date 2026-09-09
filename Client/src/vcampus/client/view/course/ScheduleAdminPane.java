/*
 * ScheduleAdminPane
 *
 * Version 1.1
 *
 * 2026-09-08
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 教务管理员排课页面，使用结构化对话框维护排课并呈现服务端冲突信息。 */
public class ScheduleAdminPane extends VBox {

    private final ICourseClientSrv _client;
    private final TableView<ScheduleRow> _table = new TableView<>();
    private final Label _statusLabel = new Label();
    private List<Course> _courses = List.of();

    /** 创建管理员排课页面。 */
    public ScheduleAdminPane(ICourseClientSrv client) {
        this._client = client;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    /** 从服务器各读取一次课程主数据与排课。 */
    public void refresh() {
        _statusLabel.setText("正在读取排课…");
        CourseViewSupport.runAsync(this, this::loadSnapshot, snapshot -> {
            _courses = snapshot.courses();
            _table.setItems(FXCollections.observableArrayList(snapshot.rows()));
            _statusLabel.setText("共 " + snapshot.rows().size() + " 条排课");
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        Label title = new Label("排课管理");
        title.getStyleClass().add("page-title");
        Label description = new Label("维护课程时间与教室；教师和教室冲突由服务端规则统一校验");
        description.getStyleClass().add("page-description");
        VBox heading = new VBox(3, title, description);
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button refreshButton = button("刷新", "secondary", this::refresh);
        Button addButton = button("+ 新增排课", "primary", () -> openEditor(null));
        HBox header = new HBox(9, heading, refreshButton, addButton);
        header.setAlignment(Pos.CENTER_LEFT);

        _table.getColumns().addAll(
                CourseViewSupport.textColumn("课程", 210, ScheduleRow::displayCourse),
                CourseViewSupport.textColumn("教师", 110, ScheduleRow::teacher),
                CourseViewSupport.textColumn("教室", 105,
                        row -> row.schedule().getClassroom()),
                CourseViewSupport.textColumn("星期", 75,
                        row -> CourseViewSupport.dayName(row.schedule().getDayOfWeek())),
                CourseViewSupport.textColumn("时间", 120,
                        row -> CourseViewSupport.timeRange(
                                row.schedule().getStartTime(), row.schedule().getEndTime())),
                actionColumn()
        );
        CourseViewSupport.configureTable(_table, "暂无排课数据");
        VBox.setVgrow(_table, Priority.ALWAYS);

        _statusLabel.getStyleClass().add("status-label");
        getChildren().addAll(header, _table, _statusLabel);
    }

    private TableColumn<ScheduleRow, Void> actionColumn() {
        TableColumn<ScheduleRow, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(150);
        column.setSortable(false);
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Button _edit = button("编辑", "secondary", () -> {
                ScheduleRow row = getTableRow().getItem();
                if (row != null) {
                    openEditor(row);
                }
            });
            private final Button _delete = button("删除", "danger", () -> {
                ScheduleRow row = getTableRow().getItem();
                if (row != null) {
                    deleteSchedule(row);
                }
            });
            private final HBox _actions = new HBox(7, _edit, _delete);

            {
                _edit.getStyleClass().add("table-action");
                _delete.getStyleClass().add("table-action");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : _actions);
            }
        });
        return column;
    }

    private Snapshot loadSnapshot() throws Exception {
        List<Course> courses = _client.queryCourse("");
        Map<String, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getCourseId, Function.identity(), (a, b) -> a));
        List<ScheduleRow> rows = _client.querySchedule().stream()
                .map(schedule -> new ScheduleRow(schedule, courseMap.get(schedule.getCourseId())))
                .toList();
        return new Snapshot(courses, rows);
    }

    private void openEditor(ScheduleRow original) {
        boolean editing = original != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "编辑排课" : "新增排课");
        dialog.setHeaderText(editing ? "修改课程安排" : "填写课程安排");
        ButtonType saveType = new ButtonType(editing ? "保存修改" : "新增排课",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveType);
        CourseViewSupport.styleDialog(dialog.getDialogPane());

        ComboBox<Course> courseBox = new ComboBox<>(
                FXCollections.observableArrayList(_courses));
        courseBox.setMaxWidth(Double.MAX_VALUE);
        courseBox.setPromptText("选择课程");
        courseBox.setConverter(courseConverter());
        ComboBox<Integer> dayBox = new ComboBox<>(
                FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7));
        dayBox.setMaxWidth(Double.MAX_VALUE);
        dayBox.setPromptText("选择星期");
        dayBox.setConverter(dayConverter());
        TextField classroom = field("例如：教一-101");
        TextField start = field("HH:mm");
        TextField end = field("HH:mm");
        start.setText("08:00");
        end.setText("09:40");

        if (editing) {
            CourseSchedule schedule = original.schedule();
            _courses.stream()
                    .filter(course -> course.getCourseId().equals(schedule.getCourseId()))
                    .findFirst()
                    .ifPresent(courseBox::setValue);
            dayBox.setValue(schedule.getDayOfWeek());
            classroom.setText(schedule.getClassroom());
            start.setText(schedule.getStartTime().toString());
            end.setText(schedule.getEndTime().toString());
        }

        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        addField(form, 0, "课程", courseBox);
        addField(form, 1, "教室", classroom);
        addField(form, 2, "星期", dayBox);
        HBox times = new HBox(8, start, new Label("至"), end);
        times.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(start, Priority.ALWAYS);
        HBox.setHgrow(end, Priority.ALWAYS);
        addField(form, 3, "时间", times);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(480);

        AtomicReference<CourseSchedule> result = new AtomicReference<>();
        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("primary");
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                Course course = courseBox.getValue();
                Integer day = dayBox.getValue();
                String room = required(classroom.getText(), "教室");
                if (course == null || day == null) {
                    throw new IllegalArgumentException("课程和星期均不能为空");
                }
                LocalTime startTime = CourseViewSupport.parseTime(start.getText());
                LocalTime endTime = CourseViewSupport.parseTime(end.getText());
                if (!startTime.isBefore(endTime)) {
                    throw new IllegalArgumentException("结束时间必须晚于开始时间");
                }
                String scheduleId = editing ? original.schedule().getScheduleId() : null;
                result.set(new CourseSchedule(scheduleId, course.getCourseId(),
                        room, day, startTime, endTime));
            } catch (RuntimeException exception) {
                CourseViewSupport.showError(exception);
                event.consume();
            }
        });

        dialog.showAndWait();
        CourseSchedule schedule = result.get();
        if (schedule == null) {
            return;
        }
        if (editing) {
            updateSchedule(schedule);
        } else {
            addSchedule(schedule);
        }
    }

    private void addSchedule(CourseSchedule schedule) {
        CourseViewSupport.runAsync(this, () -> _client.addSchedule(schedule), added -> {
            _statusLabel.setText("排课新增成功：" + added.getScheduleId());
            refresh();
        });
    }

    private void updateSchedule(CourseSchedule schedule) {
        CourseViewSupport.runAsync(this, () -> _client.updateSchedule(schedule), ignored -> {
            _statusLabel.setText("排课修改成功");
            refresh();
        });
    }

    private void deleteSchedule(ScheduleRow row) {
        if (!CourseViewSupport.confirm("删除该条排课？",
                row.displayCourse() + " · "
                        + CourseViewSupport.dayName(row.schedule().getDayOfWeek()) + " "
                        + CourseViewSupport.timeRange(row.schedule().getStartTime(),
                        row.schedule().getEndTime()))) {
            return;
        }
        CourseViewSupport.runAsync(this,
                () -> _client.deleteSchedule(row.schedule().getScheduleId()), ignored -> {
                    _statusLabel.setText("排课删除成功");
                    refresh();
                });
    }

    private StringConverter<Course> courseConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Course course) {
                return course == null ? "" : course.getCourseId() + " · "
                        + course.getCourseName() + " · " + course.getTeacher();
            }

            @Override
            public Course fromString(String value) {
                return null;
            }
        };
    }

    private StringConverter<Integer> dayConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Integer day) {
                return day == null ? "" : CourseViewSupport.dayName(day);
            }

            @Override
            public Integer fromString(String value) {
                return null;
            }
        };
    }

    private TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private void addField(GridPane form, int row, String name, Node field) {
        Label label = new Label(name);
        label.getStyleClass().add("field-label");
        form.add(label, 0, row);
        form.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value.trim();
    }

    private Button button(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
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
