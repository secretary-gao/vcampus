package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.AutoSchedulePlan;
import vcampus.common.vo.AutoScheduleRequest;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.TeachingClass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Administrator preview/apply workspace for constraint-based automatic scheduling. */
public class AutoSchedulePane extends VBox {
    private final ICourseClientSrv _client;
    private final Runnable _onApplied;
    private final TableView<CourseSchedule> _previewTable = new TableView<>();
    private final Label _status = new Label("正在读取未排课教学班…");
    private final Label _summary = new Label("尚未生成方案");
    private final Spinner<Integer> _weekStart = new Spinner<>(1, 30, 1);
    private final Spinner<Integer> _weekEnd = new Spinner<>(1, 30, 16);
    private final Button _validate = new Button("检查冲突");
    private final Button _apply = new Button("应用方案");
    private final ComboBox<Integer> _editDay = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5, 6, 7));
    private final TextField _editRoom = new TextField();
    private final TextField _editWeekStart = new TextField();
    private final TextField _editWeekEnd = new TextField();
    private final TextField _editPeriodStart = new TextField();
    private final TextField _editPeriodEnd = new TextField();
    private CourseSchedule _selectedAssignment;
    private List<TeachingClass> _unscheduled = List.of();
    private Map<String, TeachingClass> _classes = Map.of();
    private Map<String, String> _courseNames = Map.of();
    private AutoSchedulePlan _plan;

    public AutoSchedulePane(ICourseClientSrv client, Runnable onApplied) {
        _client = client;
        _onApplied = onApplied;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    public void refresh() {
        _status.setText("正在读取未排课教学班…");
        CourseViewSupport.runAsync(this, () -> {
            List<Course> courses = _client.queryCourse("");
            List<TeachingClass> classes = _client.queryTeachingClass("");
            Set<String> scheduled = _client.querySchedule().stream()
                    .map(CourseSchedule::getTeachingClassId).collect(Collectors.toSet());
            return new Snapshot(courses, classes, classes.stream()
                    .filter(value -> !scheduled.contains(value.getTeachingClassId())).toList());
        }, snapshot -> {
            _courseNames = snapshot.courses().stream().collect(Collectors.toMap(
                    Course::getCourseId, Course::getCourseName,
                    (left, right) -> left, LinkedHashMap::new));
            _classes = snapshot.classes().stream().collect(Collectors.toMap(
                    TeachingClass::getTeachingClassId, Function.identity(),
                    (left, right) -> left, LinkedHashMap::new));
            _unscheduled = snapshot.unscheduled();
            clearPreview();
            _status.setText("当前有 " + _unscheduled.size() + " 个教学班尚未排课");
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        Label title = new Label("自动排课");
        title.getStyleClass().add("page-title");
        Label description = new Label("使用 Backtracking + MRV 生成候选方案；确认前不会修改数据库");
        description.getStyleClass().add("page-description");
        VBox heading = new VBox(3, title, description);
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button refresh = new Button("刷新");
        refresh.getStyleClass().add("secondary");
        refresh.setOnAction(event -> refresh());
        HBox header = new HBox(12, heading, refresh);
        header.setAlignment(Pos.CENTER_LEFT);

        Button preview = new Button("生成预览");
        preview.getStyleClass().add("primary");
        preview.setOnAction(event -> preview());
        Button loadDemo = new Button("加载演示数据");
        loadDemo.getStyleClass().add("secondary");
        loadDemo.setOnAction(event -> loadDemoData());
        Button cancel = new Button("取消预览");
        cancel.getStyleClass().add("secondary");
        cancel.setOnAction(event -> clearPreview());
        _validate.getStyleClass().add("secondary");
        _validate.setDisable(true);
        _validate.setOnAction(event -> validatePlan());
        _apply.getStyleClass().add("success");
        _apply.setDisable(true);
        _apply.setOnAction(event -> apply());
        HBox controls = new HBox(9, new Label("周次"), _weekStart,
                new Label("至"), _weekEnd, loadDemo, preview, _validate, cancel, _apply);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("tool-bar-card");

        _previewTable.getColumns().addAll(
                CourseViewSupport.textColumn("教学班", 145, CourseSchedule::getTeachingClassId),
                CourseViewSupport.textColumn("课程名称", 180,
                        value -> courseName(value.getCourseId())),
                CourseViewSupport.textColumn("教师", 110, value -> teacher(value.getTeachingClassId())),
                CourseViewSupport.textColumn("周次", 80,
                        value -> value.getWeekStart() + "–" + value.getWeekEnd()),
                CourseViewSupport.textColumn("星期", 80,
                        value -> CourseViewSupport.dayName(value.getDayOfWeek())),
                CourseViewSupport.textColumn("节次", 80,
                        value -> value.getStartPeriod() + "–" + value.getEndPeriod()),
                CourseViewSupport.textColumn("教室", 120, CourseSchedule::getClassroom));
        CourseViewSupport.configureTable(_previewTable, "生成预览后在此检查候选方案");
        _previewTable.getSelectionModel().selectedItemProperty().addListener((ignored, old, value) -> showAssignment(value));
        VBox.setVgrow(_previewTable, Priority.ALWAYS);
        _editDay.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Integer value) {
                return value == null ? "" : CourseViewSupport.dayName(value);
            }
            @Override public Integer fromString(String value) { return null; }
        });
        _editRoom.setPromptText("教室"); _editWeekStart.setPromptText("起始周");
        _editWeekEnd.setPromptText("结束周"); _editPeriodStart.setPromptText("起始节");
        _editPeriodEnd.setPromptText("结束节");
        Button saveEdit = new Button("保存调整"); saveEdit.getStyleClass().add("secondary");
        saveEdit.setOnAction(event -> saveAssignment());
        Button deleteEdit = new Button("删除教学班"); deleteEdit.getStyleClass().add("danger");
        deleteEdit.setOnAction(event -> deleteAssignment());
        HBox editor = new HBox(8, new Label("预览详情"), _editRoom,
                _editWeekStart, _editWeekEnd, _editDay, _editPeriodStart, _editPeriodEnd,
                saveEdit, deleteEdit);
        editor.setAlignment(Pos.CENTER_LEFT);
        editor.getStyleClass().add("tool-bar-card");
        _summary.getStyleClass().add("teacher-detail-meta");
        _status.getStyleClass().add("status-label");
        getChildren().addAll(header, controls, _summary, _previewTable, editor, _status);
    }

    private void preview() {
        if (_unscheduled.isEmpty()) {
            _status.setText("当前没有需要自动排课的教学班");
            return;
        }
        AutoScheduleRequest request = new AutoScheduleRequest(_unscheduled.stream()
                .map(TeachingClass::getTeachingClassId).toList(),
                _weekStart.getValue(), _weekEnd.getValue());
        _status.setText("正在搜索可行方案…");
        CourseViewSupport.runAsync(this, () -> _client.previewAutoSchedule(request), plan -> {
            _plan = plan;
            _previewTable.setItems(FXCollections.observableArrayList(plan.getAssignments()));
            _summary.setText("教学班 " + plan.getTeachingClassCount() + " · 候选位置 "
                    + plan.getCandidateSlotCount() + " · 搜索节点 " + plan.getSearchNodes()
                    + " · 回溯 " + plan.getBacktracks() + " · " + plan.getElapsedMillis() + " ms"
                    + " · 未安排 " + plan.getUnassignedTeachingClassIds().size());
            _apply.setDisable(plan.getAssignments().isEmpty()
                    || !plan.getUnassignedTeachingClassIds().isEmpty());
            _validate.setDisable(_apply.isDisabled());
            _status.setText(_apply.isDisabled() ? "未找到完整可行方案" : "预览已生成，数据库尚未改变");
        });
    }

    private void validatePlan() {
        if (_plan == null || _plan.getAssignments().isEmpty()) return;
        _status.setText("正在检查方案与全部已有排课的冲突…");
        CourseViewSupport.runAsync(this, () -> _client.validateAutoSchedule(_plan), count -> {
            _status.setText("冲突检查通过，方案可以应用");
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("排课方案检查");
            alert.setHeaderText("检查通过");
            alert.setContentText("已检查 " + count
                    + " 个教学班：方案内部无冲突，且与全部已有课程排课无冲突。");
            CourseViewSupport.styleDialog(alert.getDialogPane());
            alert.showAndWait();
        });
    }

    private void loadDemoData() {
        _status.setText("正在加载自动排课演示数据…");
        CourseViewSupport.runAsync(this, _client::loadAutoScheduleDemoData, count -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("加载演示数据");
            alert.setHeaderText(null);
            alert.setContentText(count == 0
                    ? "演示数据已存在，无需重复加载。"
                    : "演示数据加载成功，共生成 " + count
                    + " 个待排课教学班，请点击生成预览。");
            CourseViewSupport.styleDialog(alert.getDialogPane());
            alert.showAndWait();
            refresh();
        });
    }

    private void apply() {
        if (_plan == null || _plan.getAssignments().isEmpty()) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "确认写入 " + _plan.getAssignments().size() + " 条自动排课？",
                ButtonType.OK, ButtonType.CANCEL);
        confirmation.setHeaderText("应用自动排课方案");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        _status.setText("正在事务写入方案…");
        CourseViewSupport.runAsync(this, () -> _client.applyAutoSchedule(_plan), count -> {
            _status.setText("已成功写入 " + count + " 条排课");
            _onApplied.run();
            refresh();
        });
    }

    private void clearPreview() {
        _plan = null;
        _previewTable.getItems().clear();
        _selectedAssignment = null;
        _editRoom.clear(); _editWeekStart.clear(); _editWeekEnd.clear();
        _editPeriodStart.clear(); _editPeriodEnd.clear(); _editDay.setValue(null);
        _summary.setText("尚未生成方案");
        _validate.setDisable(true);
        _apply.setDisable(true);
    }

    private void showAssignment(CourseSchedule value) {
        _selectedAssignment = value;
        if (value == null) return;
        _editRoom.setText(value.getClassroom());
        _editWeekStart.setText(String.valueOf(value.getWeekStart()));
        _editWeekEnd.setText(String.valueOf(value.getWeekEnd()));
        _editDay.setValue(value.getDayOfWeek());
        _editPeriodStart.setText(String.valueOf(value.getStartPeriod()));
        _editPeriodEnd.setText(String.valueOf(value.getEndPeriod()));
    }

    private void saveAssignment() {
        if (_selectedAssignment == null) {
            CourseViewSupport.showError(new IllegalArgumentException("请先选择一条预览排课"));
            return;
        }
        try {
            int startWeek = number(_editWeekStart, "起始周");
            int endWeek = number(_editWeekEnd, "结束周");
            int startPeriod = number(_editPeriodStart, "起始节");
            int endPeriod = number(_editPeriodEnd, "结束节");
            if (_editDay.getValue() == null || startWeek > endWeek || startPeriod > endPeriod) {
                throw new IllegalArgumentException("请填写有效的周次、星期和节次范围");
            }
            _selectedAssignment.setClassroom(required(_editRoom, "教室"));
            _selectedAssignment.setWeekStart(startWeek); _selectedAssignment.setWeekEnd(endWeek);
            _selectedAssignment.setDayOfWeek(_editDay.getValue());
            _selectedAssignment.setStartPeriod(startPeriod); _selectedAssignment.setEndPeriod(endPeriod);
            validateEditedPlan("已更新预览，冲突检查通过");
        } catch (RuntimeException exception) { CourseViewSupport.showError(exception); }
    }

    private void deleteAssignment() {
        if (_selectedAssignment == null) {
            CourseViewSupport.showError(new IllegalArgumentException("请先选择一条预览排课"));
            return;
        }
        if (!CourseViewSupport.confirm("从方案删除该教学班？", _selectedAssignment.getTeachingClassId())) return;
        List<CourseSchedule> values = _plan.getAssignments().stream()
                .filter(value -> value != _selectedAssignment).toList();
        _plan.setAssignments(values);
        _plan.getUnassignedTeachingClassIds().add(_selectedAssignment.getTeachingClassId());
        _previewTable.setItems(FXCollections.observableArrayList(values));
        _selectedAssignment = null;
        _apply.setDisable(true);
        _validate.setDisable(true);
        _status.setText("已从预览移除教学班；请重新生成完整方案后应用");
    }

    private void validateEditedPlan(String success) {
        _status.setText("正在检查调整后的方案…");
        CourseViewSupport.runAsync(this, () -> _client.validateAutoSchedule(_plan), count -> {
            _previewTable.refresh();
            _status.setText(success + "，可应用方案");
            _apply.setDisable(false); _validate.setDisable(false);
        });
    }

    private static String required(TextField field, String name) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isBlank()) throw new IllegalArgumentException(name + "不能为空");
        return value;
    }

    private static int number(TextField field, String name) {
        try { return Integer.parseInt(required(field, name)); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException(name + "必须是整数"); }
    }

    private String teacher(String teachingClassId) {
        TeachingClass value = _classes.get(teachingClassId);
        return value == null ? "—" : value.getTeacher();
    }

    private String courseName(String courseId) {
        return _courseNames.getOrDefault(courseId, courseId == null ? "—" : courseId);
    }

    private record Snapshot(List<Course> courses, List<TeachingClass> classes,
                            List<TeachingClass> unscheduled) { }
}
