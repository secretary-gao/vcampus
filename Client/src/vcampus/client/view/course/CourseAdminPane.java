/*
 * CourseAdminPane
 *
 * Version 2.0
 *
 * 2026-09-13
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
import vcampus.common.vo.TeachingClass;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 教务管理员课程目录与教学班管理页面。 */
public class CourseAdminPane extends VBox {

    private final ICourseClientSrv _client;
    private final Runnable _courseChanged;
    private final TableView<CourseRow> _courseTable = new TableView<>();
    private final TableView<TeachingClassRow> _classTable = new TableView<>();
    private final TextField _courseKeyword = new TextField();
    private final TextField _classKeyword = new TextField();
    private final Label _courseStatus = new Label();
    private final Label _classStatus = new Label();
    private List<CourseRow> _allCourses = List.of();
    private List<TeachingClassRow> _allClasses = List.of();
    private List<Course> _courseChoices = List.of();

    /** 创建管理员课程和教学班管理页面。 */
    public CourseAdminPane(ICourseClientSrv client, Runnable courseChanged) {
        this._client = client;
        this._courseChanged = courseChanged;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    /** 一次刷新课程和教学班主数据。 */
    public void refresh() {
        _courseStatus.setText("正在读取课程…");
        _classStatus.setText("正在读取教学班…");
        CourseViewSupport.runAsync(this, this::loadSnapshot, snapshot -> {
            _allCourses = snapshot.courses();
            _allClasses = snapshot.classes();
            _courseChoices = snapshot.courseChoices();
            applyCourseFilter();
            applyClassFilter();
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        Label title = new Label("课程与教学班管理");
        title.getStyleClass().add("page-title");
        Label description = new Label("课程定义教学内容，教学班定义教师、班号、容量与授课语言");
        description.getStyleClass().add("page-description");
        VBox heading = new VBox(3, title, description);

        TabPane sections = new TabPane();
        sections.getStyleClass().add("admin-subtabs");
        sections.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        sections.getTabs().addAll(new Tab("课程目录", buildCourseSection()),
                new Tab("教学班", buildClassSection()));
        VBox.setVgrow(sections, Priority.ALWAYS);
        getChildren().addAll(heading, sections);
    }

    @SuppressWarnings("unchecked")
    private VBox buildCourseSection() {
        _courseKeyword.setPromptText("搜索课程号、课程名称、性质或开课单位");
        _courseKeyword.setPrefWidth(350);
        _courseKeyword.setOnAction(event -> applyCourseFilter());
        HBox.setHgrow(_courseKeyword, Priority.ALWAYS);
        Button search = button("搜索", "secondary", this::applyCourseFilter);
        Button refresh = button("刷新", "secondary", this::refresh);
        Button add = button("+ 新增课程", "primary", () -> openCourseEditor(null));
        HBox toolbar = new HBox(9, _courseKeyword, search, refresh, add);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("tool-bar-card");

        _courseTable.getColumns().addAll(
                CourseViewSupport.textColumn("课程号", 105,
                        row -> row.course().getCourseId()),
                CourseViewSupport.wrappingTextColumn("课程名称", 210,
                        row -> row.course().getCourseName()),
                CourseViewSupport.textColumn("性质", 78,
                        row -> CourseViewSupport.safe(row.course().getCourseNature(), "—")),
                CourseViewSupport.textColumn("开课单位", 175,
                        row -> CourseViewSupport.safe(row.course().getOpeningUnit(), "—")),
                CourseViewSupport.textColumn("学分", 55,
                        row -> String.valueOf(row.course().getCredit())),
                CourseViewSupport.textColumn("教学班", 65,
                        row -> String.valueOf(row.classCount())),
                CourseViewSupport.textColumn("已选 / 总容量", 105,
                        row -> row.course().getSelectedCount() + " / "
                                + row.course().getCapacity()),
                courseActionColumn());
        CourseViewSupport.configureTable(_courseTable, "暂无课程数据");
        _courseTable.getStyleClass().add("admin-course-table");
        _courseTable.setFixedCellSize(-1);
        VBox.setVgrow(_courseTable, Priority.ALWAYS);
        _courseStatus.getStyleClass().add("status-label");
        return new VBox(10, toolbar, _courseTable, _courseStatus);
    }

    @SuppressWarnings("unchecked")
    private VBox buildClassSection() {
        _classKeyword.setPromptText("搜索教学班 ID、课程、班号或教师");
        _classKeyword.setPrefWidth(350);
        _classKeyword.setOnAction(event -> applyClassFilter());
        HBox.setHgrow(_classKeyword, Priority.ALWAYS);
        Button search = button("搜索", "secondary", this::applyClassFilter);
        Button refresh = button("刷新", "secondary", this::refresh);
        Button add = button("+ 新增教学班", "primary", () -> openClassEditor(null));
        HBox toolbar = new HBox(9, _classKeyword, search, refresh, add);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("tool-bar-card");

        _classTable.getColumns().addAll(
                CourseViewSupport.textColumn("教学班 ID", 135,
                        row -> row.teachingClass().getTeachingClassId()),
                CourseViewSupport.textColumn("课程", 195, TeachingClassRow::displayCourse),
                CourseViewSupport.textColumn("班号", 60,
                        row -> row.teachingClass().getClassNumber()),
                CourseViewSupport.textColumn("教师", 105,
                        row -> row.teachingClass().getTeacher()),
                CourseViewSupport.textColumn("授课语言", 85,
                        row -> CourseViewSupport.safe(
                                row.teachingClass().getTeachingLanguage(), "—")),
                CourseViewSupport.textColumn("已选 / 容量", 95,
                        row -> row.teachingClass().getSelectedCount() + " / "
                                + row.teachingClass().getCapacity()),
                CourseViewSupport.textColumn("备注", 130,
                        row -> CourseViewSupport.safe(row.teachingClass().getRemark(), "—")),
                classActionColumn());
        CourseViewSupport.configureTable(_classTable, "暂无教学班数据");
        VBox.setVgrow(_classTable, Priority.ALWAYS);
        _classStatus.getStyleClass().add("status-label");
        return new VBox(10, toolbar, _classTable, _classStatus);
    }

    private Snapshot loadSnapshot() throws Exception {
        List<Course> courses = _client.queryCourse("").stream()
                .sorted(Comparator.comparing(Course::getCourseId)).toList();
        Map<String, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getCourseId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        List<TeachingClass> classes = _client.queryTeachingClass("").stream()
                .sorted(Comparator.comparing(TeachingClass::getCourseId)
                        .thenComparing(TeachingClass::getClassNumber)).toList();
        Map<String, Long> classCounts = classes.stream()
                .collect(Collectors.groupingBy(TeachingClass::getCourseId, Collectors.counting()));
        List<CourseRow> courseRows = courses.stream()
                .map(course -> new CourseRow(course,
                        classCounts.getOrDefault(course.getCourseId(), 0L).intValue()))
                .toList();
        List<TeachingClassRow> classRows = classes.stream()
                .map(value -> new TeachingClassRow(value, courseMap.get(value.getCourseId())))
                .toList();
        return new Snapshot(courseRows, classRows, courses);
    }

    private void applyCourseFilter() {
        String keyword = normalized(_courseKeyword.getText());
        List<CourseRow> rows = _allCourses.stream()
                .filter(row -> keyword.isBlank()
                        || contains(row.course().getCourseId(), keyword)
                        || contains(row.course().getCourseName(), keyword)
                        || contains(row.course().getCourseNature(), keyword)
                        || contains(row.course().getOpeningUnit(), keyword))
                .toList();
        _courseTable.setItems(FXCollections.observableArrayList(rows));
        _courseStatus.setText("显示 " + rows.size() + " 门，共 "
                + _allCourses.size() + " 门课程");
    }

    private void applyClassFilter() {
        String keyword = normalized(_classKeyword.getText());
        List<TeachingClassRow> rows = _allClasses.stream()
                .filter(row -> keyword.isBlank()
                        || contains(row.teachingClass().getTeachingClassId(), keyword)
                        || contains(row.teachingClass().getClassNumber(), keyword)
                        || contains(row.teachingClass().getTeacher(), keyword)
                        || contains(row.displayCourse(), keyword))
                .toList();
        _classTable.setItems(FXCollections.observableArrayList(rows));
        _classStatus.setText("显示 " + rows.size() + " 个，共 "
                + _allClasses.size() + " 个教学班");
    }

    private TableColumn<CourseRow, Void> courseActionColumn() {
        TableColumn<CourseRow, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(142);
        column.setSortable(false);
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Button _edit = button("编辑", "secondary", () -> {
                CourseRow row = getTableRow().getItem();
                if (row != null) openCourseEditor(row.course());
            });
            private final Button _delete = button("删除", "danger", () -> {
                CourseRow row = getTableRow().getItem();
                if (row != null) deleteCourse(row.course());
            });
            private final HBox _actions = actions(_edit, _delete);

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : _actions);
            }
        });
        return column;
    }

    private TableColumn<TeachingClassRow, Void> classActionColumn() {
        TableColumn<TeachingClassRow, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(142);
        column.setSortable(false);
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Button _edit = button("编辑", "secondary", () -> {
                TeachingClassRow row = getTableRow().getItem();
                if (row != null) openClassEditor(row);
            });
            private final Button _delete = button("删除", "danger", () -> {
                TeachingClassRow row = getTableRow().getItem();
                if (row != null) deleteClass(row);
            });
            private final HBox _actions = actions(_edit, _delete);

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : _actions);
            }
        });
        return column;
    }

    private void openCourseEditor(Course original) {
        boolean editing = original != null;
        Dialog<ButtonType> dialog = dialog(editing ? "编辑课程" : "新增课程",
                editing ? "修改课程主数据" : "填写课程与默认教学班信息");
        ButtonType saveType = saveButton(dialog, editing ? "保存修改" : "新增课程");
        TextField courseId = field("例如：CSE1001");
        TextField courseName = field("课程名称");
        TextField teacher = field("用于新课程的默认教学班");
        TextField credit = field("正整数");
        TextField capacity = field("用于新课程的默认教学班");
        TextField nature = field("例如：必修 / 任选");
        TextField openingUnit = field("开课单位");
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
        GridPane form = form();
        addField(form, 0, "课程号", courseId);
        addField(form, 1, "课程名称", courseName);
        addField(form, 2, "学分", credit);
        addField(form, 3, "课程性质", nature);
        addField(form, 4, "开课单位", openingUnit);
        addField(form, 5, "默认教师", teacher);
        addField(form, 6, "默认容量", capacity);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(455);

        AtomicReference<Course> result = new AtomicReference<>();
        validateOnSave(dialog, saveType, event -> {
            int selectedCount = editing ? original.getSelectedCount() : 0;
            Course value = new Course(required(courseId.getText(), "课程号"),
                    required(courseName.getText(), "课程名称"),
                    required(teacher.getText(), "默认教师"),
                    positiveInt(credit.getText(), "学分"),
                    positiveInt(capacity.getText(), "默认容量"), selectedCount);
            value.setCourseNature(optional(nature.getText()));
            value.setOpeningUnit(optional(openingUnit.getText()));
            result.set(value);
        });
        dialog.showAndWait();
        if (result.get() != null) {
            if (editing) updateCourse(result.get()); else addCourse(result.get());
        }
    }

    private void openClassEditor(TeachingClassRow originalRow) {
        boolean editing = originalRow != null;
        TeachingClass original = editing ? originalRow.teachingClass() : null;
        Dialog<ButtonType> dialog = dialog(editing ? "编辑教学班" : "新增教学班",
                editing ? "修改教师、容量与授课信息" : "为现有课程创建教学班");
        ButtonType saveType = saveButton(dialog, editing ? "保存修改" : "新增教学班");
        ComboBox<Course> course = new ComboBox<>(FXCollections.observableArrayList(_courseChoices));
        course.setMaxWidth(Double.MAX_VALUE);
        course.setPromptText("选择课程");
        course.setConverter(courseConverter());
        TextField classId = field("例如：CSE1001-02");
        TextField classNumber = field("例如：02");
        TextField teacher = field("与教师账号姓名一致");
        TextField capacity = field("非负整数");
        TextField language = field("例如：中文 / 全英文（可留空）");
        TextField remark = field("教学班备注（可留空）");
        if (editing) {
            _courseChoices.stream().filter(value -> value.getCourseId()
                    .equals(original.getCourseId())).findFirst().ifPresent(course::setValue);
            course.setDisable(true);
            classId.setText(original.getTeachingClassId());
            classId.setDisable(true);
            classNumber.setText(original.getClassNumber());
            teacher.setText(original.getTeacher());
            capacity.setText(String.valueOf(original.getCapacity()));
            language.setText(original.getTeachingLanguage());
            remark.setText(original.getRemark());
        }
        GridPane form = form();
        addField(form, 0, "所属课程", course);
        addField(form, 1, "教学班 ID", classId);
        addField(form, 2, "教学班号", classNumber);
        addField(form, 3, "授课教师", teacher);
        addField(form, 4, "容量", capacity);
        addField(form, 5, "授课语言", language);
        addField(form, 6, "备注", remark);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setPrefWidth(500);

        AtomicReference<TeachingClass> result = new AtomicReference<>();
        validateOnSave(dialog, saveType, event -> {
            if (course.getValue() == null) {
                throw new IllegalArgumentException("所属课程不能为空");
            }
            TeachingClass value = new TeachingClass(
                    required(classId.getText(), "教学班 ID"),
                    course.getValue().getCourseId(),
                    required(classNumber.getText(), "教学班号"),
                    required(teacher.getText(), "授课教师"),
                    nonNegativeInt(capacity.getText(), "容量"),
                    editing ? original.getSelectedCount() : 0,
                    optional(language.getText()), optional(remark.getText()));
            result.set(value);
        });
        dialog.showAndWait();
        if (result.get() != null) {
            if (editing) updateClass(result.get()); else addClass(result.get());
        }
    }

    private Dialog<ButtonType> dialog(String title, String header) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        CourseViewSupport.styleDialog(dialog.getDialogPane());
        return dialog;
    }

    private ButtonType saveButton(Dialog<ButtonType> dialog, String text) {
        ButtonType type = new ButtonType(text, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, type);
        dialog.getDialogPane().lookupButton(type).getStyleClass().add("primary");
        return type;
    }

    private void validateOnSave(Dialog<ButtonType> dialog, ButtonType saveType,
                                java.util.function.Consumer<ActionEvent> validation) {
        dialog.getDialogPane().lookupButton(saveType).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                validation.accept(event);
            } catch (RuntimeException exception) {
                CourseViewSupport.showError(exception);
                event.consume();
            }
        });
    }

    private void addCourse(Course course) {
        CourseViewSupport.runAsync(this, () -> _client.addCourse(course), ignored -> changed());
    }

    private void updateCourse(Course course) {
        CourseViewSupport.runAsync(this, () -> _client.updateCourse(course), ignored -> changed());
    }

    private void deleteCourse(Course course) {
        if (!CourseViewSupport.confirm("删除“" + course.getCourseName() + "”？",
                "存在教学班、选课或排课引用时，系统会拒绝删除。")) return;
        CourseViewSupport.runAsync(this, () -> _client.deleteCourse(course.getCourseId()),
                ignored -> changed());
    }

    private void addClass(TeachingClass teachingClass) {
        CourseViewSupport.runAsync(this, () -> _client.addTeachingClass(teachingClass),
                ignored -> changed());
    }

    private void updateClass(TeachingClass teachingClass) {
        CourseViewSupport.runAsync(this, () -> _client.updateTeachingClass(teachingClass),
                ignored -> changed());
    }

    private void deleteClass(TeachingClassRow row) {
        if (!CourseViewSupport.confirm("删除教学班 “" + row.displayCourse() + "”？",
                "已有选课或排课时，系统会拒绝删除。")) return;
        CourseViewSupport.runAsync(this, () -> _client.deleteTeachingClass(
                row.teachingClass().getTeachingClassId()), ignored -> changed());
    }

    private void changed() {
        refresh();
        if (_courseChanged != null) _courseChanged.run();
    }

    private HBox actions(Button... buttons) {
        for (Button button : buttons) button.getStyleClass().add("table-action");
        return new HBox(7, buttons);
    }

    private GridPane form() {
        GridPane form = new GridPane();
        form.getStyleClass().add("form-grid");
        return form;
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
        int parsed = nonNegativeInt(value, fieldName);
        if (parsed == 0) throw new IllegalArgumentException(fieldName + "必须为正整数");
        return parsed;
    }

    private int nonNegativeInt(String value, String fieldName) {
        try {
            int parsed = Integer.parseInt(required(value, fieldName));
            if (parsed < 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + "必须为非负整数");
        }
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Button button(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setOnAction(event -> action.run());
        return button;
    }

    private StringConverter<Course> courseConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Course course) {
                return course == null ? "" : course.getCourseId() + " · " + course.getCourseName();
            }

            @Override
            public Course fromString(String value) {
                return null;
            }
        };
    }

    private record Snapshot(List<CourseRow> courses, List<TeachingClassRow> classes,
                            List<Course> courseChoices) {
    }

    private record CourseRow(Course course, int classCount) {
    }

    private record TeachingClassRow(TeachingClass teachingClass, Course course) {
        String displayCourse() {
            return course == null ? teachingClass.getCourseId()
                    : course.getCourseId() + " · " + course.getCourseName();
        }
    }
}
