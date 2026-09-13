/*
 * CoursePanel
 *
 * Version 1.1
 *
 * 2026-09-08
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view.course;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.CourseClientSrv;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.client.view.SeuEmblem;
import vcampus.common.vo.User;

/**
 * Course 模块可嵌入主界面的根面板。学生进行选退课，教师查看本人课程名单，
 * 管理员维护课程和排课；所有数据操作均通过 {@link ICourseClientSrv} 完成。
 */
public class CoursePanel extends BorderPane {

    private final User _currentUser;
    private final ICourseClientSrv _client;
    private final TabPane _tabs = new TabPane();
    private CourseHallPane _hallPane;
    private SelectedCoursesPane _selectedPane;
    private TimetablePane _timetablePane;

    /** 使用默认 Course Socket 服务创建面板。 */
    public CoursePanel(User currentUser) {
        this(currentUser, new CourseClientSrv());
    }

    /** 注入客户端服务创建面板，便于界面 smoke test。 */
    CoursePanel(User currentUser, ICourseClientSrv client) {
        this._currentUser = currentUser;
        this._client = client;
        setPrefSize(1080, 720);
        getStyleClass().add("course-root");
        String stylesheet = CourseViewSupport.stylesheet();
        if (stylesheet != null) {
            getStylesheets().add(stylesheet);
        }
        setTop(buildHeader());
        initializeContent();
    }

    private VBox buildHeader() {
        Label eyebrow = new Label("VCAMPUS ACADEMIC SERVICES");
        eyebrow.getStyleClass().add("course-eyebrow");
        Label title = new Label("教务选课中心");
        title.getStyleClass().add("course-title");
        VBox titles = new VBox(3, eyebrow, title);
        HBox.setHgrow(titles, Priority.ALWAYS);

        String role = _currentUser == null ? "未登录" : safe(_currentUser.getURole());
        String name = _currentUser == null ? "" : safe(_currentUser.getUName());
        Label identity = new Label(role + (name.isBlank() ? "" : " · " + name));
        identity.getStyleClass().addAll("role-badge", roleClass(role));

        HBox headline = new HBox(12, SeuEmblem.build(36), titles, identity);
        headline.getStyleClass().add("course-headline");
        Label subtitle = new Label("课程检索、学习安排与教务管理");
        subtitle.getStyleClass().add("course-subtitle");
        VBox header = new VBox(8, headline, subtitle);
        header.getStyleClass().add("course-header");
        return header;
    }

    private void initializeContent() {
        _tabs.getStyleClass().add("course-tabs");
        _tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        BorderPane.setMargin(_tabs, new Insets(0, 20, 20, 20));

        if (_currentUser == null) {
            setCenter(message("请先登录后进入教务选课中心。"));
            return;
        }
        if ("管理员".equals(_currentUser.getURole())) {
            ScheduleAdminPane schedulePane = new ScheduleAdminPane(_client);
            addTab("课程管理", new CourseAdminPane(_client, schedulePane::refresh));
            addTab("排课管理", schedulePane);
            addTab("自动排课", new AutoSchedulePane(_client, schedulePane::refresh));
            setCenter(_tabs);
            return;
        }
        if ("教师".equals(_currentUser.getURole())) {
            if (_currentUser.getUName() == null || _currentUser.getUName().isBlank()) {
                setCenter(message("当前教师账号尚未设置姓名，无法匹配授课课程。"));
                return;
            }
            addTab("教师工作台", new TeacherCoursesPane(_client, _currentUser.getUName()));
            setCenter(_tabs);
            return;
        }
        if (!"学生".equals(_currentUser.getURole())) {
            setCenter(message("当前账号角色无法使用教务模块。"));
            return;
        }

        setCenter(message("正在查询当前用户的学籍信息…"));
        CourseViewSupport.runAsync(this,
                () -> _client.queryStudentId(_currentUser.getUId()), studentId -> {
                    if (studentId == null || studentId.isBlank()) {
                        setCenter(message("当前账号尚未关联正式学籍，无法进行选课。"));
                        return;
                    }
                    buildStudentTabs(studentId);
                });
    }

    private void buildStudentTabs(String studentId) {
        Runnable enrollmentChanged = () -> {
            if (_hallPane != null) {
                _hallPane.refresh();
            }
            if (_selectedPane != null) {
                _selectedPane.refresh();
            }
            if (_timetablePane != null) {
                _timetablePane.refresh();
            }
        };
        _hallPane = new CourseHallPane(_client, studentId, enrollmentChanged);
        _selectedPane = new SelectedCoursesPane(_client, studentId, enrollmentChanged,
                () -> _tabs.getSelectionModel().select(0));
        _timetablePane = new TimetablePane(_client, studentId);
        addTab("课程大厅", _hallPane);
        addTab("我的课程", _selectedPane);
        addTab("我的课表", _timetablePane);
        setCenter(_tabs);
    }

    private void addTab(String title, javafx.scene.Node content) {
        _tabs.getTabs().add(new Tab(title, content));
    }

    private VBox message(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state-label");
        VBox box = new VBox(label);
        box.getStyleClass().addAll("course-card", "message-card");
        BorderPane.setMargin(box, new Insets(0, 20, 20, 20));
        return box;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未设置" : value;
    }

    private String roleClass(String role) {
        return switch (role) {
            case "管理员" -> "role-admin";
            case "教师" -> "role-teacher";
            case "学生" -> "role-student";
            default -> "role-unknown";
        };
    }
}
