/*
 * CourseAdminPane
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
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;

import java.util.concurrent.atomic.AtomicReference;

/** 教务管理员课程主数据页面，使用工具栏、行内操作和复用表单对话框。 */
public class CourseAdminPane extends VBox {

    private final ICourseClientSrv _client;
    private final Runnable _courseChanged;
    private final TableView<Course> _table = new TableView<>();
    private final TextField _keywordField = new TextField();
    private final Label _statusLabel = new Label();

    /** 创建管理员课程管理页面。 */
    public CourseAdminPane(ICourseClientSrv client, Runnable courseChanged) {
        this._client = client;
        this._courseChanged = courseChanged;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    /** 按当前关键字从服务器刷新课程主数据。 */
    public void refresh() {
        String keyword = _keywordField.getText();
        _statusLabel.setText("正在读取课程…");
        CourseViewSupport.runAsync(this, () -> _client.queryCourse(keyword), courses -> {
            _table.setItems(FXCollections.observableArrayList(courses));
            _statusLabel.setText("共 " + courses.size() + " 门课程");
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        Label title = new Label("课程管理");
        title.getStyleClass().add("page-title");
        Label description = new Label("维护课程主数据；存在选课或排课引用的课程仍受原有规则保护");
        description.getStyleClass().add("page-description");
        VBox heading = new VBox(3, title, description);

        _keywordField.setPromptText("搜索课程号、名称或教师");
        _keywordField.setPrefWidth(300);
        _keywordField.setOnAction(event -> refresh());
        Button searchButton = button("搜索", "secondary", this::refresh);
        Button addButton = button("+ 新增课程", "primary", () -> openEditor(null));
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(9, _keywordField, searchButton, spacer, addButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("tool-bar-card");

        _table.getColumns().addAll(
                CourseViewSupport.textColumn("课程号", 110, Course::getCourseId),
                CourseViewSupport.textColumn("课程名称", 190, Course::getCourseName),
                CourseViewSupport.textColumn("教师", 115, Course::getTeacher),
                CourseViewSupport.textColumn("学分", 65,
                        course -> String.valueOf(course.getCredit())),
                CourseViewSupport.textColumn("容量", 70,
                        course -> String.valueOf(course.getCapacity())),
                CourseViewSupport.textColumn("已选", 70,
                        course -> String.valueOf(course.getSelectedCount())),
                actionColumn()
        );
        CourseViewSupport.configureTable(_table, "暂无课程数据");
        VBox.setVgrow(_table, Priority.ALWAYS);

        _statusLabel.getStyleClass().add("status-label");
        getChildren().addAll(heading, toolbar, _table, _statusLabel);
    }

    private TableColumn<Course, Void> actionColumn() {
        TableColumn<Course, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(150);
        column.setSortable(false);
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Button _edit = button("编辑", "secondary", () -> {
                Course course = getTableRow().getItem();
                if (course != null) {
                    openEditor(course);
                }
            });
            private final Button _delete = button("删除", "danger", () -> {
                Course course = getTableRow().getItem();
                if (course != null) {
                    deleteCourse(course);
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

    private void openEditor(Course original) {
        boolean editing = original != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editing ? "编辑课程" : "新增课程");
        dialog.setHeaderText(editing ? "修改课程信息" : "填写新课程信息");
        ButtonType saveType = new ButtonType(editing ? "保存修改" : "新增课程",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveType);
        CourseViewSupport.styleDialog(dialog.getDialogPane());

        TextField courseId = field("例如：CSE1001");
        TextField courseName = field("课程名称");
        TextField teacher = field("与教师账号姓名一致");
        TextField credit = field("正整数");
        TextField capacity = field("正整数");
        TextField nature = field("例如：必修 / 任选（可留空）");
        TextField openingUnit = field("开课单位（可留空）");
        if (editing) {
            courseId.setText(original.getCourseId());
            courseId.setDisable(true);
            courseName.setText(original.getCourseName());
            teacher.setText(original.getTeacher());
            credit.setText(String.valueOf(original.getCredit()));
            capacity.setText(String.valueOf(original.getCapacity()));
            nature.setText(original.getCourseNature());
            openingUnit.setText(original.getOpeningUnit());
        }

        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        addField(form, 0, "课程号", courseId);
        addField(form, 1, "课程名称", courseName);
        addField(form, 2, "授课教师", teacher);
        addField(form, 3, "学分", credit);
        addField(form, 4, "课程容量", capacity);
        addField(form, 5, "课程性质", nature);
        addField(form, 6, "开课单位", openingUnit);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(430);

        AtomicReference<Course> result = new AtomicReference<>();
        Node saveButton = dialog.getDialogPane().lookupButton(saveType);
        saveButton.getStyleClass().add("primary");
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                int selectedCount = editing ? original.getSelectedCount() : 0;
                Course value = new Course(
                        required(courseId.getText(), "课程号"),
                        required(courseName.getText(), "课程名称"),
                        required(teacher.getText(), "授课教师"),
                        positiveInt(credit.getText(), "学分"),
                        positiveInt(capacity.getText(), "课程容量"),
                        selectedCount);
                value.setCourseNature(optional(nature.getText()));
                value.setOpeningUnit(optional(openingUnit.getText()));
                result.set(value);
            } catch (RuntimeException exception) {
                CourseViewSupport.showError(exception);
                event.consume();
            }
        });

        dialog.showAndWait();
        Course course = result.get();
        if (course == null) {
            return;
        }
        if (editing) {
            updateCourse(course);
        } else {
            addCourse(course);
        }
    }

    private void addCourse(Course course) {
        CourseViewSupport.runAsync(this, () -> _client.addCourse(course), added -> {
            _statusLabel.setText("课程新增成功：" + added.getCourseId());
            refreshAfterChange();
        });
    }

    private void updateCourse(Course course) {
        CourseViewSupport.runAsync(this, () -> _client.updateCourse(course), ignored -> {
            _statusLabel.setText("课程修改成功：" + course.getCourseName());
            refreshAfterChange();
        });
    }

    private void deleteCourse(Course course) {
        if (!CourseViewSupport.confirm("删除“" + course.getCourseName() + "”？",
                "存在选课或排课引用时，系统会拒绝删除。")) {
            return;
        }
        CourseViewSupport.runAsync(this,
                () -> _client.deleteCourse(course.getCourseId()), ignored -> {
                    _statusLabel.setText("课程删除成功：" + course.getCourseName());
                    refreshAfterChange();
                });
    }

    private void refreshAfterChange() {
        refresh();
        if (_courseChanged != null) {
            _courseChanged.run();
        }
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

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int positiveInt(String value, String fieldName) {
        try {
            int parsed = Integer.parseInt(required(value, fieldName));
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + "必须为正整数");
        }
    }

    private Button button(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setOnAction(event -> action.run());
        return button;
    }
}
