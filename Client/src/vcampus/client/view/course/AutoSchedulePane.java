package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.AutoSchedulePlan;
import vcampus.common.vo.AutoScheduleRequest;
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
    private final Button _apply = new Button("应用方案");
    private List<TeachingClass> _unscheduled = List.of();
    private Map<String, TeachingClass> _classes = Map.of();
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
            List<TeachingClass> classes = _client.queryTeachingClass("");
            Set<String> scheduled = _client.querySchedule().stream()
                    .map(CourseSchedule::getTeachingClassId).collect(Collectors.toSet());
            return new Snapshot(classes, classes.stream()
                    .filter(value -> !scheduled.contains(value.getTeachingClassId())).toList());
        }, snapshot -> {
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
        Button cancel = new Button("取消预览");
        cancel.getStyleClass().add("secondary");
        cancel.setOnAction(event -> clearPreview());
        _apply.getStyleClass().add("success");
        _apply.setDisable(true);
        _apply.setOnAction(event -> apply());
        HBox controls = new HBox(9, new Label("周次"), _weekStart,
                new Label("至"), _weekEnd, preview, cancel, _apply);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().add("tool-bar-card");

        _previewTable.getColumns().addAll(
                CourseViewSupport.textColumn("教学班", 160, CourseSchedule::getTeachingClassId),
                CourseViewSupport.textColumn("教师", 100, value -> teacher(value.getTeachingClassId())),
                CourseViewSupport.textColumn("周次", 80,
                        value -> value.getWeekStart() + "–" + value.getWeekEnd()),
                CourseViewSupport.textColumn("星期", 80,
                        value -> CourseViewSupport.dayName(value.getDayOfWeek())),
                CourseViewSupport.textColumn("节次", 80,
                        value -> value.getStartPeriod() + "–" + value.getEndPeriod()),
                CourseViewSupport.textColumn("教室", 120, CourseSchedule::getClassroom));
        CourseViewSupport.configureTable(_previewTable, "生成预览后在此检查候选方案");
        VBox.setVgrow(_previewTable, Priority.ALWAYS);
        _summary.getStyleClass().add("teacher-detail-meta");
        _status.getStyleClass().add("status-label");
        getChildren().addAll(header, controls, _summary, _previewTable, _status);
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
            _status.setText(_apply.isDisabled() ? "未找到完整可行方案" : "预览已生成，数据库尚未改变");
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
        _summary.setText("尚未生成方案");
        _apply.setDisable(true);
    }

    private String teacher(String teachingClassId) {
        TeachingClass value = _classes.get(teachingClassId);
        return value == null ? "—" : value.getTeacher();
    }

    private record Snapshot(List<TeachingClass> classes, List<TeachingClass> unscheduled) { }
}
