/*
 * CourseHallPane
 *
 * Version 1.1
 *
 * 2026-09-08
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;
import vcampus.common.vo.SelectCourse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 学生课程大厅：检索课程、按状态筛选并在课程行内发起选课。 */
public class CourseHallPane extends VBox {

    private final ICourseClientSrv _client;
    private final String _studentId;
    private final Runnable _onEnrollmentChanged;
    private final TextField _keywordField = new TextField();
    private final TableView<Course> _courseTable = new TableView<>();
    private final Label _statusLabel = new Label();
    private final ToggleGroup _filterGroup = new ToggleGroup();
    private List<Course> _allCourses = List.of();
    private Set<String> _selectedIds = Set.of();

    /** 创建课程大厅。 */
    public CourseHallPane(ICourseClientSrv client, String studentId,
                          Runnable onEnrollmentChanged) {
        this._client = client;
        this._studentId = studentId;
        this._onEnrollmentChanged = onEnrollmentChanged;
        getStyleClass().add("course-page");
        buildView();
        refresh();
    }

    /** 从服务器各读取一次课程与当前选课状态，再在客户端完成状态筛选。 */
    public void refresh() {
        String keyword = _keywordField.getText();
        _statusLabel.setText("正在读取课程…");
        CourseViewSupport.runAsync(this, () -> loadSnapshot(keyword), snapshot -> {
            _allCourses = snapshot.courses();
            _selectedIds = snapshot.selectedIds();
            applyFilter();
        });
    }

    @SuppressWarnings("unchecked")
    private void buildView() {
        Label title = new Label("课程大厅");
        title.getStyleClass().add("page-title");
        Label description = new Label("搜索课程并直接完成选课，课程人数会在操作后同步更新");
        description.getStyleClass().add("page-description");
        VBox heading = new VBox(3, title, description);

        _keywordField.setPromptText("课程号 / 课程名称 / 教师");
        _keywordField.setPrefWidth(390);
        HBox.setHgrow(_keywordField, Priority.ALWAYS);
        Button searchButton = new Button("搜索");
        searchButton.getStyleClass().add("primary");
        searchButton.setDefaultButton(true);
        searchButton.setOnAction(event -> refresh());
        _keywordField.setOnAction(event -> refresh());

        ToggleButton all = filterButton("全部", "all");
        ToggleButton available = filterButton("有余量", "available");
        ToggleButton selected = filterButton("已选", "selected");
        all.setSelected(true);
        _filterGroup.selectedToggleProperty().addListener((observable, oldValue, value) -> {
            if (value == null) {
                oldValue.setSelected(true);
            } else {
                applyFilter();
            }
        });

        HBox searchBar = new HBox(10, _keywordField, searchButton,
                new Label("筛选"), all, available, selected);
        searchBar.getStyleClass().add("tool-bar-card");
        searchBar.setAlignment(Pos.CENTER_LEFT);

        _courseTable.getColumns().addAll(
                CourseViewSupport.textColumn("课程号", 125, Course::getCourseId),
                CourseViewSupport.textColumn("课程名称", 220, Course::getCourseName),
                CourseViewSupport.textColumn("教师", 130, Course::getTeacher),
                CourseViewSupport.textColumn("学分", 70,
                        course -> String.valueOf(course.getCredit())),
                CourseViewSupport.textColumn("已选 / 容量", 115,
                        course -> course.getSelectedCount() + " / " + course.getCapacity()),
                actionColumn()
        );
        CourseViewSupport.configureTable(_courseTable, "暂无可选课程");
        VBox.setVgrow(_courseTable, Priority.ALWAYS);

        _statusLabel.getStyleClass().add("status-label");
        getChildren().addAll(heading, searchBar, _courseTable, _statusLabel);
    }

    private ToggleButton filterButton(String text, String value) {
        ToggleButton button = new ToggleButton(text);
        button.setUserData(value);
        button.setToggleGroup(_filterGroup);
        button.getStyleClass().add("filter-toggle");
        return button;
    }

    private Snapshot loadSnapshot(String keyword) throws Exception {
        List<Course> courses = _client.queryCourse(keyword);
        Set<String> selectedIds = new HashSet<>();
        for (SelectCourse selection : _client.querySelectedCourse(_studentId)) {
            selectedIds.add(selection.getCourseId());
        }
        return new Snapshot(courses, selectedIds);
    }

    private void applyFilter() {
        String filter = _filterGroup.getSelectedToggle() == null ? "all"
                : String.valueOf(_filterGroup.getSelectedToggle().getUserData());
        List<Course> visible = _allCourses.stream()
                .filter(course -> switch (filter) {
                    case "available" -> course.getSelectedCount() < course.getCapacity();
                    case "selected" -> _selectedIds.contains(course.getCourseId());
                    default -> true;
                })
                .toList();
        _courseTable.setItems(FXCollections.observableArrayList(visible));
        _courseTable.refresh();
        _statusLabel.setText("显示 " + visible.size() + " 门，共 " + _allCourses.size() + " 门课程");
    }

    private TableColumn<Course, Void> actionColumn() {
        TableColumn<Course, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(105);
        column.setSortable(false);
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Button _button = new Button();

            {
                _button.getStyleClass().add("table-action");
                _button.setOnAction(event -> {
                    Course course = getTableRow().getItem();
                    if (course != null) {
                        selectCourse(course);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                Course course = empty ? null : getTableRow().getItem();
                if (course == null) {
                    setGraphic(null);
                    return;
                }
                _button.getStyleClass().removeAll("primary", "success", "warning");
                if (_selectedIds.contains(course.getCourseId())) {
                    _button.setText("已选");
                    _button.setDisable(true);
                    _button.getStyleClass().add("success");
                } else if (course.getSelectedCount() >= course.getCapacity()) {
                    _button.setText("已满");
                    _button.setDisable(true);
                    _button.getStyleClass().add("warning");
                } else {
                    _button.setText("选课");
                    _button.setDisable(false);
                    _button.getStyleClass().add("primary");
                }
                setGraphic(_button);
            }
        });
        return column;
    }

    private void selectCourse(Course course) {
        CourseViewSupport.runAsync(this,
                () -> _client.selectCourse(_studentId, course.getCourseId()), ignored -> {
                    _statusLabel.setText("已选：" + course.getCourseName());
                    _onEnrollmentChanged.run();
                });
    }

    private record Snapshot(List<Course> courses, Set<String> selectedIds) {
    }
}
