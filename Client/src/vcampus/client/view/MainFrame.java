/*
 * MainFrame
 *
 * Version 1.0
 *
 * 2026-08-31
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;

import vcampus.client.biz.IUserClientSrv;
import vcampus.client.biz.UserClientSrv;
import vcampus.client.view.Library.LibraryPanel;
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

    /** 当前登录信息区域。 */
    private VBox _userInfoPanel;

    /** 功能入口区域。 */
    private VBox _featurePanel;

    /** 中间内容容器。 */
    private StackPane _contentStack;

    /** 总览面板。 */
    private HBox _dashboardPanel;

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
        roleLabel.setTextFill(Color.web("#dcebfb"));
        roleLabel.setFont(Font.font("System", 11));

        VBox textBlock = new VBox(0, nameLabel, roleLabel);

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
        _contentStack.setPadding(new Insets(20, 28, 24, 28));

        _dashboardPanel = new HBox(18, buildUserInfoPanel(), buildFeaturePanel());
        _dashboardPanel.setAlignment(Pos.TOP_CENTER);
        _contentStack.getChildren().add(_dashboardPanel);
        return _contentStack;
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
     * 构建左侧登录信息卡片：头像、姓名、角色标签、登录ID，
     * 以及"返回登录 / 退出程序"两个操作按钮。
     *
     * @return 用户信息面板
     */
    private VBox buildUserInfoPanel() {
        _userInfoPanel = new VBox(14);
        _userInfoPanel.setPadding(new Insets(28, 22, 22, 22));
        _userInfoPanel.setPrefWidth(280);
        _userInfoPanel.setAlignment(Pos.TOP_CENTER);
        _userInfoPanel.setStyle(CARD_STYLE);

        String uid = _currentUser == null ? "" : safeText(_currentUser.getUId());
        String name = _currentUser == null ? "" : safeText(_currentUser.getUName());
        String role = _currentUser == null ? "" : safeText(_currentUser.getURole());
        String display = name.isEmpty() ? uid : name;
        String initial = display.isEmpty() ? "?" : display.substring(0, 1);

        StackPane avatar = new StackPane();
        avatar.setPrefSize(72, 72);
        Circle avatarCircle = new Circle(36, Color.web("#1c5a97"));
        Label avatarText = new Label(initial);
        avatarText.setTextFill(Color.WHITE);
        avatarText.setFont(Font.font("System", FontWeight.BOLD, 26));
        avatar.getChildren().addAll(avatarCircle, avatarText);

        Label nameLabel = new Label(display.isEmpty() ? "未登录" : display);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#1d2b39"));

        Label roleTag = new Label(role.isEmpty() ? "未设置" : role);
        roleTag.setTextFill(Color.WHITE);
        roleTag.setStyle("-fx-background-color: #3fa34d; -fx-background-radius: 12;"
                + "-fx-padding: 3 14 3 14; -fx-font-size: 11px; -fx-font-weight: bold;");

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: rgba(29,90,153,0.10);");

        VBox infoLines = new VBox(10,
                buildInfoLine("登录ID", uid.isEmpty() ? "—" : uid),
                buildInfoLine("姓名", name.isEmpty() ? "未填写" : name));
        infoLines.setAlignment(Pos.CENTER_LEFT);
        infoLines.setMaxWidth(Double.MAX_VALUE);

        Button backButton = new Button("返回登录");
        Button exitButton = new Button("退出程序");
        backButton.setOnAction(e -> onBackToLogin());
        exitButton.setOnAction(e -> Platform.exit());
        backButton.setMaxWidth(Double.MAX_VALUE);
        exitButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setPrefHeight(40);
        exitButton.setPrefHeight(40);
        backButton.setStyle("-fx-background-radius: 20; -fx-background-color: #3fa34d; -fx-text-fill: white;"
                + " -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-cursor: hand;");
        exitButton.setStyle("-fx-background-radius: 20; -fx-background-color: #eef3f8; -fx-text-fill: #1c5a97;"
                + " -fx-font-size: 13.5px; -fx-font-weight: bold; -fx-cursor: hand;");

        VBox buttonBlock = new VBox(10, backButton, exitButton);
        buttonBlock.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(buttonBlock, new Insets(6, 0, 0, 0));

        _userInfoPanel.getChildren().addAll(avatar, nameLabel, roleTag, divider, infoLines, buttonBlock);
        return _userInfoPanel;
    }

    /**
     * 构建一行"标签：值"信息。
     *
     * @param label 标签
     * @param value 值
     * @return 信息行
     */
    private HBox buildInfoLine(String label, String value) {
        Label labelText = new Label(label);
        labelText.setTextFill(Color.web("#8b96a4"));
        labelText.setFont(Font.font("System", 12.5));
        labelText.setPrefWidth(56);

        Label valueText = new Label(value);
        valueText.setTextFill(Color.web("#1d2b39"));
        valueText.setFont(Font.font("System", FontWeight.BOLD, 13));

        HBox line = new HBox(6, labelText, valueText);
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }

    /**
     * 构建右侧"常用服务"图标网格。
     *
     * @return 功能入口面板
     */
    private VBox buildFeaturePanel() {
        _featurePanel = new VBox(14);
        _featurePanel.setPadding(new Insets(24));
        _featurePanel.setPrefWidth(560);
        _featurePanel.setStyle(CARD_STYLE);

        Label title = new Label("常用服务");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#1d2b39"));

        Label tip = new Label("点击图标进入对应功能模块");
        tip.setTextFill(Color.web("#8b96a4"));
        tip.setFont(Font.font("System", 12));

        GridPane moduleGrid = new GridPane();
        moduleGrid.setHgap(14);
        moduleGrid.setVgap(14);

        moduleGrid.add(buildModuleTile("图书馆", "借阅、续借、预约、检索", "library", "#2d6a9f"), 0, 0);
        moduleGrid.add(buildModuleTile("学籍", "学籍信息、成绩、证明", "student", "#3fa34d"), 1, 0);
        moduleGrid.add(buildModuleTile("医院", "挂号、预约、健康服务", "hospital", "#e6604f"), 2, 0);
        moduleGrid.add(buildModuleTile("教务", "课表、选课、考试通知", "edu", "#e0a53b"), 0, 1);
        moduleGrid.add(buildModuleTile("宿舍", "入住、报修、查寝", "dorm", "#7c65e6"), 1, 1);
        moduleGrid.add(buildModuleTile("商店", "商品、支付、订单", "shop", "#2fa89a"), 2, 1);

        _featurePanel.getChildren().addAll(title, tip, moduleGrid);
        return _featurePanel;
    }

    /**
     * 构建一个功能图标磁贴：圆形色块图标（取模块名首字）+ 名称 + 简述，
     * 整块可点击，鼠标悬停时轻微高亮，比按钮式卡片更贴近门户"应用图标"的观感。
     *
     * @param name  功能名称
     * @param desc  功能描述
     * @param key   功能键名
     * @param color 强调色
     * @return 功能磁贴
     */
    private VBox buildModuleTile(String name, String desc, String key, String color) {
        String normalStyle = "-fx-background-color: #f8fafc; -fx-background-radius: 16;"
                + "-fx-border-radius: 16; -fx-border-color: rgba(0,0,0,0.05); -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: #eef4fb; -fx-background-radius: 16;"
                + "-fx-border-radius: 16; -fx-border-color: rgba(29,90,153,0.18); -fx-cursor: hand;";

        VBox tile = new VBox(8);
        tile.setPrefSize(166, 138);
        tile.setAlignment(Pos.TOP_CENTER);
        tile.setPadding(new Insets(18, 10, 14, 10));
        tile.setStyle(normalStyle);

        StackPane iconBadge = new StackPane();
        iconBadge.setPrefSize(50, 50);
        Circle iconCircle = new Circle(25, Color.web(color));
        Label iconText = new Label(name.substring(0, 1));
        iconText.setTextFill(Color.WHITE);
        iconText.setFont(Font.font("System", FontWeight.BOLD, 18));
        iconBadge.getChildren().addAll(iconCircle, iconText);

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14.5));
        nameLabel.setTextFill(Color.web("#1d2b39"));

        Label descLabel = new Label(desc);
        descLabel.setWrapText(true);
        descLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        descLabel.setTextFill(Color.web("#8b96a4"));
        descLabel.setFont(Font.font("System", 11));

        tile.getChildren().addAll(iconBadge, nameLabel, descLabel);
        tile.setOnMouseEntered(e -> tile.setStyle(hoverStyle));
        tile.setOnMouseExited(e -> tile.setStyle(normalStyle));
        tile.setOnMouseClicked(e -> showModulePage(name, desc, key, color));
        return tile;
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
        if ("library".equals(moduleKey)) {
            showLibraryPage();
            return;
        }
        if ("student".equals(moduleKey)) {
            showStudentPage();
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

        Button backButton = new Button("返回总览");
        backButton.setStyle("-fx-background-color: #3fa34d; -fx-text-fill: white;"
                + " -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
        backButton.setOnAction(e -> showDashboard());

        HBox bottomBar = new HBox(backButton);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);

        page.getChildren().addAll(title, desc, infoGrid, featureBox, bottomBar);

        _contentStack.getChildren().setAll(page);
    }

    /**
     * 打开图书馆模块的真实业务界面（{@link LibraryPanel}），而不是通用占位页。
     */
    private void showLibraryPage() {
        LibraryPanel libraryPanel = new LibraryPanel();
        if (_currentUser != null) {
            libraryPanel.setCurrentUserId(_currentUser.getUId());
        }

        Button backButton = new Button("返回总览");
        backButton.setStyle("-fx-background-color: #73c553; -fx-text-fill: white;"
                + " -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
        backButton.setOnAction(e -> showDashboard());

        HBox bottomBar = new HBox(backButton);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));

        VBox wrapper = new VBox(0, libraryPanel, bottomBar);
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

        Button backButton = new Button("返回总览");
        backButton.setStyle("-fx-background-color: #73c553; -fx-text-fill: white;"
                + " -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
        backButton.setOnAction(e -> showDashboard());

        HBox bottomBar = new HBox(backButton);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));

        VBox wrapper = new VBox(0, studentPanel, bottomBar);
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
     * 返回总览页。
     */
    private void showDashboard() {
        if (_contentStack != null && _dashboardPanel != null) {
            _contentStack.getChildren().setAll(_dashboardPanel);
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
     * 客户端程序入口。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        launch(args);
    }
}
