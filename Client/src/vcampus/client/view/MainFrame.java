/*
 * MainFrame
 *
 * Version 1.1
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;
import vcampus.client.biz.IUserClientSrv;
import vcampus.client.biz.UserClientSrv;
import vcampus.client.view.Library.LibraryPanel;
import vcampus.client.view.ai.AIChatPanel;
import vcampus.client.view.course.CoursePanel;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Message;
import vcampus.common.vo.User;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.IOException;
/**
 * 登录成功后的客户端主界面骨架。界面参照东南大学"一网通办"综合服务大厅
 * 的视觉风格：顶部蓝色导航条（校徽 + 标题 + 用户身份），
 * 下方左侧是登录信息卡片，右侧是图标化的功能分区网格，
 * 点击图标进入对应业务模块占位页。
 */
public class MainFrame extends Application {
    /** 卡片统一圆角与阴影样式。 */
    private static final String CARD_STYLE = "-fx-border-color: rgba(29,90,153,0.12); -fx-border-radius: 18;"
            + "-fx-background-radius: 18; -fx-background-color: rgba(255,255,255,0.97);"
            + "-fx-effect: dropshadow(gaussian, rgba(15,40,70,0.10), 20, 0.12, 0, 8);";
    /** 当前登录用户。 */
    private final User _currentUser;
    /** 客户端用户业务服务，负责向服务器发送登出请求。 */
    private final IUserClientSrv _userClientSrv = new UserClientSrv();
    /** 当前窗口的舞台。 */
    private Stage _stage;
    /** 左侧导航栏（身份信息 + 模块列表）。 */
    private VBox _sidebar;
    /** 右侧内容容器：点击左侧导航项后，具体操作界面显示在这里。 */
    private StackPane _contentStack;
    /** 左侧导航项：模块键名 → 对应的行容器，用于切换时更新高亮样式。 */
    private final java.util.Map<String, HBox> _navItems = new java.util.LinkedHashMap<>();
    /** 当前高亮的导航项键名，"dashboard" 表示总览页。 */
    private String _activeModuleKey = "dashboard";
    /**
     * 构造方法。
     *
     * @param currentUser 当前登录用户
     */
    public MainFrame(User currentUser) {
        this._currentUser = currentUser;
    }
    /**
     * 构造方法。
     */
    public MainFrame() {
        this(null);
    }
    /**
     * 搭建主界面。
     */
    private void buildUi() {
        BorderPane root = new BorderPane();
        root.setPrefSize(1200, 780);
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#eef3f8")),
                        new Stop(1, Color.web("#dde8f4"))),
                CornerRadii.EMPTY, Insets.EMPTY)));
        root.setTop(buildNavBar());
        root.setCenter(buildCenterStack());
        Scene scene = new Scene(root);
        _stage.setTitle("东南大学 Vcampus 主界面");
        _stage.setMinWidth(1200);
        _stage.setMinHeight(780);
        _stage.setScene(scene);
        _stage.centerOnScreen();
        _stage.setOnCloseRequest(e -> Platform.exit());
    }
    /**
     * 构建顶部导航条：左侧校徽与标题，右侧当前用户身份。
     *
     * @return 导航条面板
     */
    private BorderPane buildNavBar() {
        BorderPane nav = new BorderPane();
        nav.setPadding(new Insets(14, 26, 14, 26));
        nav.setStyle("-fx-background-color: linear-gradient(to right, #1c5a97, #3f86c9);"
                + "-fx-background-radius: 0 0 18 18;");
        StackPane smallEmblem = SeuEmblem.build(42);
        Label title = new Label("东南大学 · Vcampus");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 19));
        Label subtitle = new Label("综合服务大厅");
        subtitle.setTextFill(Color.web("#dcebfb"));
        subtitle.setFont(Font.font("System", 12));
        VBox titleBlock = new VBox(1, title, subtitle);
        HBox left = new HBox(12, smallEmblem, titleBlock);
        left.setAlignment(Pos.CENTER_LEFT);
        nav.setLeft(left);
        nav.setRight(buildUserChip());
        return nav;
    }
    /**
     * 构建导航条右侧的用户身份小标：头像圆 + 姓名/角色。
     *
     * @return 用户身份控件
     */
    private HBox buildUserChip() {
        String uid = _currentUser == null ? "" : safeText(_currentUser.getUId());
        String name = _currentUser == null ? "" : safeText(_currentUser.getUName());
        String display = name.isEmpty() ? uid : name;
        String initial = display.isEmpty() ? "?" : display.substring(0, 1);
        StackPane avatar = new StackPane();
        avatar.setPrefSize(36, 36);
        Circle avatarCircle = new Circle(18, Color.web("#f4e2ab"));
        Label avatarText = new Label(initial);
        avatarText.setTextFill(Color.web("#1c5a97"));
        avatarText.setFont(Font.font("System", FontWeight.BOLD, 15));
        avatar.getChildren().addAll(avatarCircle, avatarText);
        Label nameLabel = new Label(display.isEmpty() ? "未登录" : display);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        String role = _currentUser == null ? "" : safeText(_currentUser.getURole());
        Label roleLabel = new Label(role.isEmpty() ? "访客" : role);
        roleLabel.setTextFill(Color.web(roleNavColor(role)));
        roleLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        VBox textBlock = new VBox(3, nameLabel, roleLabel);
        HBox chip = new HBox(10, avatar, textBlock);
        chip.setAlignment(Pos.CENTER_RIGHT);
        return chip;
    }
    /**
     * 构建中间内容容器。
     *
     * @return 内容堆栈
     */
    private StackPane buildCenterStack() {
        _contentStack = new StackPane();
        _contentStack.setAlignment(Pos.TOP_LEFT);
        _contentStack.setPadding(new Insets(20, 28, 24, 28));
        BorderPane layout = new BorderPane();
        layout.setLeft(buildSidebar());
        layout.setCenter(_contentStack);
        StackPane outer = new StackPane(layout);
        showDashboard();
        return outer;
    }
    /**
     * JavaFX 启动入口。
     *
     * @param stage 舞台
     */
    @Override
    public void start(Stage stage) {
        this._stage = stage;
        buildUi();
        stage.show();
    }
    /**
     * 构建左侧导航栏：顶部是身份信息（头像/姓名/角色），中间是全部功能
     * 模块的入口列表（替代原来右侧的图标网格——点一下左边，右边
     * {@link #_contentStack} 直接换成对应模块的操作界面，不用来回切页），
     * 底部固定"返回登录 / 退出程序"两个按钮。
     *
     * @return 导航栏面板
     */
    private VBox buildSidebar() {
        _sidebar = new VBox(0);
        _sidebar.setPadding(new Insets(24, 16, 20, 16));
        _sidebar.setPrefWidth(240);
        _sidebar.setMinWidth(240);
        _sidebar.setAlignment(Pos.TOP_CENTER);
        _sidebar.setStyle(CARD_STYLE);
        String uid = _currentUser == null ? "" : safeText(_currentUser.getUId());
        String name = _currentUser == null ? "" : safeText(_currentUser.getUName());
        String role = _currentUser == null ? "" : safeText(_currentUser.getURole());
        String display = name.isEmpty() ? uid : name;
        String initial = display.isEmpty() ? "?" : display.substring(0, 1);
        StackPane avatar = new StackPane();
        avatar.setPrefSize(64, 64);
        Circle avatarCircle = new Circle(32, Color.web("#1c5a97"));
        Label avatarText = new Label(initial);
        avatarText.setTextFill(Color.WHITE);
        avatarText.setFont(Font.font("System", FontWeight.BOLD, 24));
        avatar.getChildren().addAll(avatarCircle, avatarText);
        Label nameLabel = new Label(display.isEmpty() ? "未登录" : display);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        nameLabel.setTextFill(Color.web("#1d2b39"));
        Label roleTag = new Label(role.isEmpty() ? "未设置" : role);
        roleTag.setTextFill(Color.WHITE);
        roleTag.setStyle("-fx-background-color: " + roleColor(role) + "; -fx-background-radius: 12;"
                + "-fx-padding: 2 12 2 12; -fx-font-size: 10.5px; -fx-font-weight: bold;");
        VBox identityBlock = new VBox(8, avatar, nameLabel, roleTag);
        identityBlock.setAlignment(Pos.TOP_CENTER);
        identityBlock.setPadding(new Insets(0, 0, 18, 0));
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: rgba(29,90,153,0.10);");
        VBox navList = new VBox(4);
        navList.setPadding(new Insets(14, 0, 0, 0));
        navList.getChildren().add(buildNavItem("总览", "dashboard", "#697687", this::showDashboard));
        navList.getChildren().add(buildNavItem("图书馆", "library", "#2d6a9f",
                () -> showModulePage("图书馆", "借阅、续借、预约、检索", "library", "#2d6a9f")));
        navList.getChildren().add(buildNavItem("学籍", "student", "#3fa34d",
                () -> showModulePage("学籍", "学籍信息、成绩、证明", "student", "#3fa34d")));
        navList.getChildren().add(buildNavItem("医院", "hospital", "#e6604f",
                () -> showModulePage("医院", "挂号、预约、健康服务", "hospital", "#e6604f")));
        navList.getChildren().add(buildNavItem("教务", "edu", "#e0a53b",
                () -> showModulePage("教务", "课表、选课、考试通知", "edu", "#e0a53b")));
        navList.getChildren().add(buildNavItem("商店", "shop", "#2fa89a",
                () -> showModulePage("商店", "商品、支付、订单", "shop", "#2fa89a")));
        navList.getChildren().add(buildNavItem("校园AI", "ai", "#1c5a97",
                () -> showModulePage("校园AI", "有什么问题都可以问问", "ai", "#1c5a97")));
        if (isAdmin()) {
            navList.getChildren().add(buildNavItem("账号管理", "usermgmt", "#d4a017",
                    () -> showModulePage("账号管理", "禁用/启用用户账号", "usermgmt", "#d4a017")));
        }
        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Button backButton = new Button("返回登录");
        Button exitButton = new Button("退出程序");
        backButton.setOnAction(e -> onBackToLogin());
        exitButton.setOnAction(e -> Platform.exit());
        backButton.setMaxWidth(Double.MAX_VALUE);
        exitButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setPrefHeight(38);
        exitButton.setPrefHeight(38);
        backButton.setStyle("-fx-background-radius: 20; -fx-background-color: #3fa34d; -fx-text-fill: white;"
                + " -fx-font-size: 12.5px; -fx-font-weight: bold; -fx-cursor: hand;");
        exitButton.setStyle("-fx-background-radius: 20; -fx-background-color: #eef3f8; -fx-text-fill: #1c5a97;"
                + " -fx-font-size: 12.5px; -fx-font-weight: bold; -fx-cursor: hand;");
        VBox buttonBlock = new VBox(10, backButton, exitButton);
        buttonBlock.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(buttonBlock, new Insets(10, 0, 0, 0));
        _sidebar.getChildren().addAll(identityBlock, divider, navList, spacer, buttonBlock);
        return _sidebar;
    }
    /**
     * 构建左侧导航栏里的一个功能入口行：色块 + 名称，整行可点击，
     * 悬停/选中时改变背景色。选中状态由 {@link #_activeModuleKey} 驱动，
     * 见 {@link #highlightActiveNav()}。
     *
     * @param name   模块名称
     * @param key    模块键名，需要和 {@link #showModulePage} 里的判断一致
     * @param color  强调色（左侧色块）
     * @param action 点击后要执行的动作
     * @return 导航行容器
     */
    private HBox buildNavItem(String name, String key, String color, Runnable action) {
        Region dot = new Region();
        dot.setPrefSize(8, 8);
        dot.setMaxSize(8, 8);
        dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
        Label label = new Label(name);
        label.setFont(Font.font("System", FontWeight.BOLD, 13));
        label.setTextFill(Color.web("#1d2b39"));
        HBox row = new HBox(10, dot, label);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setMaxWidth(Double.MAX_VALUE);
        row.setStyle("-fx-background-radius: 10; -fx-cursor: hand;");
        row.setOnMouseEntered(e -> {
            if (!key.equals(_activeModuleKey)) {
                row.setStyle("-fx-background-radius: 10; -fx-cursor: hand; -fx-background-color: #f2f6fa;");
            }
        });
        row.setOnMouseExited(e -> {
            if (!key.equals(_activeModuleKey)) {
                row.setStyle("-fx-background-radius: 10; -fx-cursor: hand;");
            }
        });
        row.setOnMouseClicked(e -> action.run());
        _navItems.put(key, row);
        return row;
    }
    /**
     * 按 {@link #_activeModuleKey} 重新刷新每个导航行的高亮样式：
     * 当前选中的模块背景高亮、字体加粗变色，其余恢复默认。
     */
    private void highlightActiveNav() {
        for (java.util.Map.Entry<String, HBox> entry : _navItems.entrySet()) {
            boolean active = entry.getKey().equals(_activeModuleKey);
            HBox row = entry.getValue();
            row.setStyle("-fx-background-radius: 10; -fx-cursor: hand;"
                    + (active ? " -fx-background-color: #e3edf7;" : ""));
            Label label = (Label) row.getChildren().get(1);
            label.setTextFill(Color.web(active ? "#1c5a97" : "#1d2b39"));
        }
    }
    /**
     * 打开模块分区占位页。
     *
     * @param moduleName 模块名
     * @param moduleDesc 模块描述
     * @param moduleKey  模块键名
     * @param color      强调色
     */
    private void showModulePage(String moduleName, String moduleDesc, String moduleKey, String color) {
        if ("shop".equals(moduleKey)) {
            openStore();
            return;
        }
        // ========医院改为嵌入，不再新开窗口========
        if ("hospital".equals(moduleKey)) {
            _activeModuleKey = moduleKey;
            highlightActiveNav();
            showHospitalPage();
            return;
        }
        if ("library".equals(moduleKey)) {
            _activeModuleKey = moduleKey;
            highlightActiveNav();
            showLibraryPage();
            return;
        }
        if ("student".equals(moduleKey)) {
            _activeModuleKey = moduleKey;
            highlightActiveNav();
            showStudentPage();
            return;
        }
        if ("usermgmt".equals(moduleKey)) {
            _activeModuleKey = moduleKey;
            highlightActiveNav();
            showUserManagementPage();
            return;
        }
        if ("edu".equals(moduleKey)) {
            _activeModuleKey = moduleKey;
            highlightActiveNav();
            showCoursePage();
            return;
        }
        if ("ai".equals(moduleKey)) {
            showAiChatPage();
            return;
        }
        VBox page = new VBox(16);
        page.setPadding(new Insets(22));
        page.setMaxWidth(820);
        page.setStyle(CARD_STYLE);
        Label title = new Label(moduleName + "模块分区");
        title.setTextFill(Color.web("#1d2b39"));
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        Label desc = new Label(moduleDesc);
        desc.setTextFill(Color.web("#697687"));
        desc.setFont(Font.font("System", 14));
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(12);
        infoGrid.setVgap(12);
        infoGrid.add(buildInfoTile("模块键名", moduleKey, color), 0, 0);
        infoGrid.add(buildInfoTile("当前状态", "占位页面", color), 1, 0);
        infoGrid.add(buildInfoTile("后续动作", "接真实业务界面", color), 2, 0);
        VBox featureBox = new VBox(8);
        featureBox.setPadding(new Insets(6, 0, 0, 0));
        featureBox.getChildren().addAll(
                buildBullet("列表页、详情页、表单页可按此模块继续扩展"),
                buildBullet("这里先做可见的分区入口，方便答辩展示"),
                buildBullet("后续接入真实业务后，可替换为查找/新增/编辑操作"));
        page.getChildren().addAll(title, desc, infoGrid, featureBox);
        _contentStack.getChildren().setAll(page);
    }
    /**
     * 打开虚拟商店模块的真实业务界面（{@link StoreFrame}）。商店窗口是独立的
     * {@code Application}，新开一个 {@link Stage} 承载。
     */
    private void openStore() {
        try {
            Stage storeStage = new Stage();
            new StoreFrame(_currentUser).start(storeStage);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("打开商店模块失败：" + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * 【新增】医院模块：嵌入主界面右侧内容区，不再独立弹窗
     */
    private void showHospitalPage() {
        HospitalFrame hospitalFrame = new HospitalFrame(
                _currentUser == null ? "" : _currentUser.getUId(),
                _currentUser == null ? "" : _currentUser.getURole()
        );
        VBox wrapper = new VBox(0, hospitalFrame);
        VBox.setVgrow(hospitalFrame, javafx.scene.layout.Priority.ALWAYS);
        wrapper.setMaxWidth(1120);
        wrapper.setMaxHeight(Double.MAX_VALUE);
        wrapper.setStyle(CARD_STYLE);
        _contentStack.getChildren().setAll(wrapper);
    }

    /**
     * 显示"账号管理"操作页（仅管理员可见入口，见 {@link #isAdmin()}），
     * 嵌入右侧 {@link #_contentStack}，和图书馆/学籍走同一套"左侧选、
     * 右侧显示"的布局，不再用弹窗打断操作流程。
     *
     * <p>管理员输入目标用户登录ID、选择新状态（正常/禁用），提交后通过
     * {@link IUserClientSrv#setUserStatus} 发给服务器。服务器端会用
     * 当前登录管理员的 uId 重新查库确认真实角色，不是客户端自称管理员
     * 就能生效，对应说明书"管理员可注销/禁用账号"的要求。</p>
     */
    private void showUserManagementPage() {
        VBox page = new VBox(16);
        page.setPadding(new Insets(24));
        page.setMaxWidth(480);
        page.setStyle(CARD_STYLE);
        Label title = new Label("账号管理");
        title.setTextFill(Color.web("#1d2b39"));
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        Label subtitle = new Label("禁用或启用指定用户的登录账号");
        subtitle.setTextFill(Color.web("#697687"));
        subtitle.setFont(Font.font("System", 13));
        Label idHint = new Label("目标用户登录ID");
        idHint.setFont(Font.font("System", FontWeight.BOLD, 12.5));
        TextField targetIdField = new TextField();
        targetIdField.setPromptText("请输入8位登录ID");
        Label statusHint = new Label("新状态");
        statusHint.setFont(Font.font("System", FontWeight.BOLD, 12.5));
        ToggleGroup statusGroup = new ToggleGroup();
        RadioButton disableOption = new RadioButton("禁用");
        disableOption.setToggleGroup(statusGroup);
        disableOption.setUserData(User.STATUS_DISABLED);
        RadioButton enableOption = new RadioButton("启用（恢复正常）");
        enableOption.setToggleGroup(statusGroup);
        enableOption.setUserData(User.STATUS_NORMAL);
        disableOption.setSelected(true);
        Button submitButton = new Button("提交");
        submitButton.setStyle("-fx-background-color: #d4a017; -fx-text-fill: white;"
                + " -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
        submitButton.setPrefWidth(120);
        Label resultLabel = new Label();
        resultLabel.setWrapText(true);
        resultLabel.setTextFill(Color.web("#697687"));
        submitButton.setOnAction(event -> {
            String targetUId = safeText(targetIdField.getText());
            if (targetUId.length() != 8) {
                resultLabel.setTextFill(Color.web("#e6604f"));
                resultLabel.setText("登录ID必须为8位，请检查后重新输入");
                return;
            }
            String newStatus = (String) statusGroup.getSelectedToggle().getUserData();
            submitButton.setDisable(true);
            resultLabel.setTextFill(Color.web("#697687"));
            resultLabel.setText("正在提交…");
            new Thread(() -> {
                String resultText;
                boolean success;
                try {
                    Message response = _userClientSrv.setUserStatus(
                            _currentUser.getUId(), targetUId, newStatus);
                    success = IConstant.STATUS_SUCCESS.equals(response.getStatusCode());
                    resultText = String.valueOf(response.getData());
                } catch (IOException | ClassNotFoundException e) {
                    success = false;
                    resultText = "网络异常：" + e.getMessage();
                }
                final String finalText = resultText;
                final boolean finalSuccess = success;
                Platform.runLater(() -> {
                    submitButton.setDisable(false);
                    resultLabel.setTextFill(Color.web(finalSuccess ? "#3fa34d" : "#e6604f"));
                    resultLabel.setText(finalText);
                    if (finalSuccess) {
                        targetIdField.clear();
                    }
                });
            }).start();
        });
        page.getChildren().addAll(title, subtitle, idHint, targetIdField, statusHint,
                new HBox(16, disableOption, enableOption), submitButton, resultLabel);
        _contentStack.getChildren().setAll(page);
    }
    /**
     * 打开图书馆模块的真实业务界面（{@link LibraryPanel}），而不是通用占位页。
     */
    private void showLibraryPage() {
        LibraryPanel libraryPanel = new LibraryPanel(_currentUser);

        VBox wrapper = new VBox(0, libraryPanel);
        wrapper.setMaxWidth(900);
        wrapper.setStyle("-fx-background-color: rgba(255,255,255,0.98);"
                + "-fx-background-radius: 20;"
                + "-fx-border-radius: 20;"
                + "-fx-border-color: rgba(45,106,159,0.18);"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 22, 0.12, 0, 8);");
        _contentStack.getChildren().setAll(wrapper);
    }
    /**
     * 打开学籍模块的真实业务界面，并复用主界面的统一服务器连接。
     */
    private void showStudentPage() {
        StudentManagementFrame studentFrame = new StudentManagementFrame();
        BorderPane studentPanel = studentFrame.createView();
        VBox wrapper = new VBox(0, studentPanel);
        wrapper.setMaxWidth(1120);
        wrapper.setMaxHeight(Double.MAX_VALUE);
        wrapper.setStyle("-fx-background-color: rgba(255,255,255,0.98);"
                + "-fx-background-radius: 20;"
                + "-fx-border-radius: 20;"
                + "-fx-border-color: rgba(45,106,159,0.18);"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 22, 0.12, 0, 8);");
        _contentStack.getChildren().setAll(wrapper);
        studentFrame.attachStyleSheet(_stage.getScene());
        Platform.runLater(studentFrame::refresh);
    }
    /**
     * 打开教务（选课）模块的真实业务界面（{@link CoursePanel}），嵌入
     * 右侧内容区，和图书馆/学籍走同一套布局——左侧导航栏常驻，不需要
     * 页面自带"返回总览"按钮。
     */
    private void showCoursePage() {
        CoursePanel coursePanel = new CoursePanel(_currentUser);
        VBox wrapper = new VBox(0, coursePanel);
        VBox.setVgrow(coursePanel, javafx.scene.layout.Priority.ALWAYS);
        wrapper.setMaxWidth(1120);
        wrapper.setMaxHeight(Double.MAX_VALUE);
        wrapper.setStyle(CARD_STYLE);
        _contentStack.getChildren().setAll(wrapper);
    }
    /**
     * 打开 AI 问答面板（{@link AIChatPanel}），嵌入右侧内容区，所有角色
     * 都能用。和图书馆/学籍/教务同一套"左侧选、右侧显示"布局。
     */
    private void showAiChatPage() {
        AIChatPanel aiChatPanel = new AIChatPanel(_currentUser);

        VBox wrapper = new VBox(0, aiChatPanel);
        VBox.setVgrow(aiChatPanel, javafx.scene.layout.Priority.ALWAYS);
        wrapper.setMaxWidth(1120);
        wrapper.setMaxHeight(Double.MAX_VALUE);
        wrapper.setStyle(CARD_STYLE);
        _contentStack.getChildren().setAll(wrapper);
    }

    /**
     * 构建信息小块。
     *
     * @param label 标题
     * @param value 内容
     * @param color 强调色
     * @return 信息块
     */
    private VBox buildInfoTile(String label, String value, String color) {
        VBox tile = new VBox(6);
        tile.setPrefWidth(170);
        tile.setPadding(new Insets(14));
        tile.setStyle("-fx-background-color: #f8fbff;"
                + "-fx-background-radius: 16;"
                + "-fx-border-radius: 16;"
                + "-fx-border-color: rgba(0,0,0,0.05);");
        Label labelText = new Label(label);
        labelText.setTextFill(Color.web("#607080"));
        labelText.setFont(Font.font("System", FontWeight.BOLD, 12));
        Label valueText = new Label(value);
        valueText.setTextFill(Color.web(color));
        valueText.setFont(Font.font("System", FontWeight.BOLD, 16));
        tile.getChildren().addAll(labelText, valueText);
        return tile;
    }
    /**
     * 构建要点文本。
     *
     * @param text 文本内容
     * @return 段落
     */
    private Label buildBullet(String text) {
        Label bullet = new Label("• " + text);
        bullet.setWrapText(true);
        bullet.setTextFill(Color.web("#4f5d6b"));
        bullet.setFont(Font.font("System", 14));
        return bullet;
    }
    /**
     * 显示总览欢迎页：登录后默认看到的页面，以及点左侧导航"总览"回到的页面。
     */
    private void showDashboard() {
        _activeModuleKey = "dashboard";
        highlightActiveNav();
        String name = _currentUser == null ? "" : safeText(_currentUser.getUName());
        String uid = _currentUser == null ? "" : safeText(_currentUser.getUId());
        String display = name.isEmpty() ? uid : name;
        VBox page = new VBox(14);
        page.setPadding(new Insets(28));
        page.setMaxWidth(720);
        page.setStyle(CARD_STYLE);
        Label title = new Label("欢迎回来" + (display.isEmpty() ? "" : "，" + display));
        title.setTextFill(Color.web("#1d2b39"));
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        Label tip = new Label("从左侧选择一个功能模块开始操作。");
        tip.setTextFill(Color.web("#697687"));
        tip.setFont(Font.font("System", 14));
        page.getChildren().addAll(title, tip);
        if (_contentStack != null) {
            _contentStack.getChildren().setAll(page);
        }
    }
    /**
     * 返回登录界面。
     */
    private void onBackToLogin() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认");
        alert.setHeaderText(null);
        alert.setContentText("确定返回登录界面吗？");
        alert.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    Message response = _userClientSrv.logout(_currentUser);
                    if (!IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                        Alert error = new Alert(Alert.AlertType.ERROR);
                        error.setTitle("登出失败");
                        error.setHeaderText(null);
                        error.setContentText(String.valueOf(response.getData()));
                        error.showAndWait();
                        return;
                    }
                    if (_stage != null) {
                        _stage.close();
                    }
                    new LoginFrame().start(new Stage());
                } catch (IOException e) {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("连接失败");
                    error.setHeaderText(null);
                    error.setContentText("无法发送登出请求：" + e.getMessage());
                    error.showAndWait();
                } catch (ClassNotFoundException e) {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("错误");
                    error.setHeaderText(null);
                    error.setContentText("服务器返回的数据无法识别：" + e.getMessage());
                    error.showAndWait();
                } catch (Exception e) {
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("错误");
                    error.setHeaderText(null);
                    error.setContentText("返回登录界面失败：" + e.getMessage());
                    error.showAndWait();
                }
            }
        });
    }
    /**
     * 安全转换文本，避免空值。
     *
     * @param value 原始值
     * @return 非空文本
     */
    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
    /**
     * 角色对应的强调色（用于用户信息卡片里的角色标签背景），三种角色一眼可辨：
     * 学生-绿、教师-蓝、管理员-金。
     *
     * @param role 角色文本
     * @return 十六进制颜色字符串
     */
    private String roleColor(String role) {
        if ("管理员".equals(role)) {
            return "#c9860a";
        }
        if ("教师".equals(role)) {
            return "#2d6a9f";
        }
        return "#3fa34d";
    }
    /**
     * 角色对应的导航栏文字颜色（深蓝渐变背景上要用浅色文字才看得清）。
     *
     * @param role 角色文本
     * @return 十六进制颜色字符串
     */
    private String roleNavColor(String role) {
        if ("管理员".equals(role)) {
            return "#ffd54f";
        }
        if ("教师".equals(role)) {
            return "#bfe3ff";
        }
        return "#dcebfb";
    }
    /**
     * 判断当前登录用户是否为管理员，用于控制"账号管理"等管理员专属
     * 功能入口的可见性。仅做界面展示层面的隐藏/显示，真正的权限校验
     * 在服务器端 {@code UserServerSrv#setUserStatus} 重新查库判定，
     * 不信任客户端传来的角色。
     *
     * @return 是管理员则为 {@code true}
     */
    private boolean isAdmin() {
        return _currentUser != null && "管理员".equals(_currentUser.getURole());
    }
    /**
     * 客户端程序入口。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        launch(args);
    }
}
