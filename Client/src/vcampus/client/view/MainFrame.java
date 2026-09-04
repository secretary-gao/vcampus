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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * 登录成功后的客户端主界面骨架。当前阶段先提供一个统一入口，
 * 展示已登录用户信息，并预留后续业务模块按钮位置，方便继续扩展。
 */
public class MainFrame extends Application {

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

    /** 顶部横幅。 */
    private Label _bannerLabel;

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
        root.setPrefSize(1200, 760);
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#f7f9fc")),
                        new Stop(1, Color.web("#e9f1fb"))),
                CornerRadii.EMPTY, Insets.EMPTY)));

        root.setTop(buildBanner());

        root.setCenter(buildCenterStack());

        Button backButton = new Button("返回登录");
        Button exitButton = new Button("退出程序");

        backButton.setOnAction(e -> onBackToLogin());
        exitButton.setOnAction(e -> Platform.exit());
        backButton.setPrefWidth(140);
        exitButton.setPrefWidth(140);
        backButton.setPrefHeight(44);
        exitButton.setPrefHeight(44);
        backButton.setStyle("-fx-background-radius: 24; -fx-background-color: #73c553; -fx-text-fill: white;"
                + " -fx-font-size: 15px; -fx-font-weight: bold;");
        exitButton.setStyle("-fx-background-radius: 24; -fx-background-color: #ecf3fb; -fx-text-fill: #2d6a9f;"
                + " -fx-font-size: 15px; -fx-font-weight: bold;");

        HBox buttonPanel = new HBox(12, backButton, exitButton);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);
        buttonPanel.setPadding(new Insets(0, 32, 24, 0));
        root.setBottom(buttonPanel);

        Scene scene = new Scene(root);
        _stage.setTitle("东南大学 Vcampus 主界面");
        _stage.setMinWidth(1200);
        _stage.setMinHeight(760);
        _stage.setScene(scene);
        _stage.centerOnScreen();
        _stage.setOnCloseRequest(e -> Platform.exit());
    }

    /**
     * 构建顶部横幅。
     *
     * @return 横幅面板
     */
    private BorderPane buildBanner() {
        BorderPane banner = new BorderPane();
        banner.setPadding(new Insets(18, 26, 18, 26));
        banner.setStyle("-fx-background-color: linear-gradient(to right, #2d6a9f, #4f8fc6);"
                + "-fx-background-radius: 0 0 24 24;");

        _bannerLabel = new Label("东南大学 · Vcampus 系列作品");
        _bannerLabel.setTextFill(Color.WHITE);
        _bannerLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        banner.setLeft(_bannerLabel);

        Label subtitle = new Label("校园服务统一入口");
        subtitle.setTextFill(Color.WHITE);
        subtitle.setFont(Font.font("System", 14));
        banner.setRight(subtitle);
        BorderPane.setAlignment(subtitle, Pos.CENTER_RIGHT);
        return banner;
    }

    /**
     * 构建中间内容容器。
     *
     * @return 内容堆栈
     */
    private StackPane buildCenterStack() {
        _contentStack = new StackPane();
        _contentStack.setPadding(new Insets(18, 28, 18, 28));

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
     * 构建用户信息区域。
     *
     * @return 用户信息面板
     */
    private VBox buildUserInfoPanel() {
        _userInfoPanel = new VBox(8);
        _userInfoPanel.setPadding(new Insets(18));
        _userInfoPanel.setSpacing(10);
        _userInfoPanel.setPrefWidth(350);
        _userInfoPanel.setStyle("-fx-border-color: rgba(45,106,159,0.18); -fx-border-radius: 20;"
                + "-fx-background-radius: 20; -fx-background-color: rgba(255,255,255,0.96);"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 22, 0.12, 0, 8);");

        Label title = new Label("当前登录信息");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#1d2b39"));

        String uid = _currentUser == null ? "" : safeText(_currentUser.getUId());
        String name = _currentUser == null ? "" : safeText(_currentUser.getUName());
        String role = _currentUser == null ? "" : safeText(_currentUser.getURole());

        _userInfoPanel.getChildren().addAll(
                title,
                new Label("欢迎使用东南大学 Vcampus 身份认证中心"),
                new Label("登录ID：" + uid),
                new Label("姓名：" + (name.isEmpty() ? "未填写" : name)),
                new Label("角色：" + (role.isEmpty() ? "未设置" : role)));
        return _userInfoPanel;
    }

    /**
     * 构建后续功能占位区域。
     *
     * @return 功能入口面板
     */
    private VBox buildFeaturePanel() {
        _featurePanel = new VBox(8);
        _featurePanel.setPadding(new Insets(18));
        _featurePanel.setSpacing(14);
        _featurePanel.setPrefWidth(540);
        _featurePanel.setStyle("-fx-border-color: rgba(45,106,159,0.18); -fx-border-radius: 20;"
                + "-fx-background-radius: 20; -fx-background-color: rgba(255,255,255,0.96);"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 22, 0.12, 0, 8);");

        Label title = new Label("功能入口（后续扩展）");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#1d2b39"));

        Label tip = new Label("以下为功能分区示意，后续可继续接入真实业务");
        tip.setTextFill(Color.web("#708090"));

        GridPane moduleGrid = new GridPane();
        moduleGrid.setHgap(12);
        moduleGrid.setVgap(12);

        moduleGrid.add(buildModuleCard("图书馆", "借阅、续借、预约、检索", "library", "#4f8fc6"), 0, 0);
        moduleGrid.add(buildModuleCard("学籍", "学籍信息、成绩、证明", "student", "#73c553"), 1, 0);
        moduleGrid.add(buildModuleCard("医院", "挂号、预约、健康服务", "hospital", "#f26a6a"), 2, 0);
        moduleGrid.add(buildModuleCard("教务", "课表、选课、考试通知", "edu", "#f2b84b"), 0, 1);
        moduleGrid.add(buildModuleCard("宿舍", "入住、报修、查寝", "dorm", "#8d77ff"), 1, 1);
        moduleGrid.add(buildModuleCard("商店", "商品、支付、订单", "shop", "#38b5a6"), 2, 1);

        _featurePanel.getChildren().addAll(title, tip, moduleGrid);
        return _featurePanel;
    }

    /**
     * 构建一个功能卡片。
     *
     * @param name  功能名称
     * @param desc  功能描述
     * @param key   功能键名
     * @param color 强调色
     * @return 功能卡片
     */
    private VBox buildModuleCard(String name, String desc, String key, String color) {
        VBox card = new VBox(8);
        card.setPrefSize(154, 128);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white;"
                + "-fx-background-radius: 18;"
                + "-fx-border-radius: 18;"
                + "-fx-border-color: rgba(0,0,0,0.06);"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 14, 0.1, 0, 4);");

        Label nameLabel = new Label(name);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameLabel.setTextFill(Color.web("#1d2b39"));

        Label descLabel = new Label(desc);
        descLabel.setWrapText(true);
        descLabel.setTextFill(Color.web("#697687"));
        descLabel.setFont(Font.font("System", 12));

        Label tag = new Label("功能分区");
        tag.setTextFill(Color.WHITE);
        tag.setStyle("-fx-background-color: " + color + ";"
                + "-fx-background-radius: 12;"
                + "-fx-padding: 3 10 3 10;"
                + "-fx-font-size: 11px;"
                + "-fx-font-weight: bold;");

        Button enterButton = new Button("进入");
        enterButton.setPrefWidth(86);
        enterButton.setStyle("-fx-background-color: " + color + ";"
                + "-fx-text-fill: white;"
                + "-fx-background-radius: 18;"
                + "-fx-font-weight: bold;"
                + "-fx-cursor: hand;");
        enterButton.setOnAction(e -> showModulePage(name, desc, key, color));

        card.getChildren().addAll(tag, nameLabel, descLabel, enterButton);
        return card;
    }

    /**
     * 打开模块分区页。
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
        page.setStyle("-fx-background-color: rgba(255,255,255,0.98);"
                + "-fx-background-radius: 20;"
                + "-fx-border-radius: 20;"
                + "-fx-border-color: rgba(45,106,159,0.18);"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 22, 0.12, 0, 8);");

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
        backButton.setStyle("-fx-background-color: #73c553; -fx-text-fill: white;"
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
