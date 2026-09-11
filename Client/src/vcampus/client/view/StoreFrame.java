/*
 * StoreFrame
 *
 * Version 2.0
 *
 * 2026-09-07
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;

import vcampus.client.biz.IStoreClientSrv;
import vcampus.client.biz.StoreClientSrv;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Goods;
import vcampus.common.vo.Message;
import vcampus.common.vo.PurchaseRecord;
import vcampus.common.vo.User;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 虚拟商店模块客户端主界面（JavaFX，电商卡片风格）。
 *
 * <p>包含三个页签：商品商城（卡片网格 + 搜索/分类）、我的订单（购买记录，仅本人/管理员全部）、
 * 商品管理（仅管理员）。所有业务请求通过 {@link StoreClientSrv} 发送到服务器，客户端不直接访问数据库。
 * 管理员表单的显示由 {@link User#getURole()} 决定，保证非管理员看不到管理入口。</p>
 *
 * <p>说明：按说明书"一次购买仅针对单一商品"，本模块不提供购物车；商品卡片不设图片字段，
 * 用"类别色块 + 名称首字"作为占位图。</p>
 */
public class StoreFrame extends Application {

    /** 下单时间格式。 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 当前登录用户。 */
    private User _currentUser;

    /** 商店客户端业务服务。 */
    private final IStoreClientSrv _storeClientSrv = new StoreClientSrv();

    // ---- 商品商城 ----
    private FlowPane _goodsCards = new FlowPane(16, 16);
    private final TextField _searchField = new TextField();
    private final ComboBox<String> _categoryBox = new ComboBox<>();

    // ---- 我的订单 ----
    private TableView<PurchaseRecord> _recordsTable = new TableView<>();

    // ---- 商品管理（管理员）----
    private TableView<Goods> _manageTable = new TableView<>();
    private final TextField _goodsIdField = new TextField();
    private final TextField _goodsNameField = new TextField();
    private final TextField _categoryField = new TextField();
    private final TextField _priceField = new TextField();
    private final TextField _stockField = new TextField();

    /**
     * 无参构造方法（供 {@code launch()} 使用），默认使用一个演示学生用户。
     */
    public StoreFrame() {
        this._currentUser = new User("09010101", "张三", 20, "男", null, "学生");
    }

    /**
     * 带当前登录用户的构造方法（供主界面或登录流程传入真实用户）。
     *
     * @param currentUser 当前登录用户
     */
    public StoreFrame(User currentUser) {
        this._currentUser = currentUser;
    }

    /**
     * 判断当前用户是否为管理员。
     *
     * @return {@code true} 表示管理员
     */
    private boolean isAdmin() {
        return _currentUser != null && "管理员".equals(_currentUser.getURole());
    }

    /**
     * 独立窗口启动入口（{@code launch} 用）：构建内容、加底部退出按钮后显示。
     *
     * @param stage 舞台
     */
    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane(buildContent());
        root.setPrefSize(1080, 720);

        Button exitButton = new Button("退出");
        exitButton.setStyle("-fx-background-radius: 20; -fx-background-color: #ecf3fb;"
                + " -fx-text-fill: #2d6a9f; -fx-font-size: 14px; -fx-font-weight: bold;");
        exitButton.setOnAction(e -> Platform.exit());
        HBox bottom = new HBox(exitButton);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(0, 24, 14, 0));
        root.setBottom(bottom);

        Scene scene = new Scene(root);
        stage.setTitle("虚拟商店 - 当前用户：" + safe(_currentUser == null ? "" : _currentUser.getUName()));
        stage.setScene(scene);
        stage.centerOnScreen();
        loadGoods();
        stage.show();
    }

    /**
     * 构建商店模块的可嵌入内容（顶部横幅 + 商品/订单/管理页签），
     * 供独立窗口与主界面嵌入共用。
     *
     * @return 内容面板
     */
    private BorderPane buildContent() {
        _categoryBox.getItems().setAll("全部商品", "食品", "饮料", "文具", "生活用品", "数码");
        _categoryBox.getSelectionModel().selectFirst();

        BorderPane root = new BorderPane();
        root.setPrefSize(1080, 720);
        root.setBackground(new Background(new BackgroundFill(
                Color.web("#f4f7fb"), CornerRadii.EMPTY, Insets.EMPTY)));
        root.setTop(buildBanner());

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().add(new Tab("商品商城", buildBuyTab()));
        tabPane.getTabs().add(new Tab("我的订单", buildRecordsTab()));
        if (isAdmin()) {
            tabPane.getTabs().add(new Tab("商品管理", buildManageTab()));
        }
        root.setCenter(tabPane);
        return root;
    }

    /**
     * 生成一个可嵌入主界面内容区的商店视图（{@code Node}），并加载商品。
     *
     * @return 商店内容节点
     */
    public javafx.scene.Node createView() {
        BorderPane content = buildContent();
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        loadGoods();
        return content;
    }

    /**
     * 构建顶部横幅。
     *
     * @return 横幅面板
     */
    private BorderPane buildBanner() {
        BorderPane banner = new BorderPane();
        banner.setPadding(new Insets(16, 26, 16, 26));
        banner.setStyle("-fx-background-color: linear-gradient(to right, #0f8f8f, #38b5a6);"
                + "-fx-background-radius: 0 0 22 22;");

        VBox titleBlock = new VBox(2);
        Label title = new Label("东南大学 · 虚拟商店");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        Label crumb = new Label("数字校园 / 虚拟商店");
        crumb.setTextFill(Color.web("#d6f0ee"));
        crumb.setFont(Font.font("System", 12));
        titleBlock.getChildren().addAll(title, crumb);
        banner.setLeft(titleBlock);

        Label userInfo = new Label("用户：" + safe(_currentUser == null ? "" : _currentUser.getUId())
                + "（" + safe(_currentUser == null ? "" : _currentUser.getURole()) + "）");
        userInfo.setTextFill(Color.WHITE);
        userInfo.setFont(Font.font("System", 14));
        banner.setRight(userInfo);
        BorderPane.setAlignment(userInfo, Pos.CENTER_RIGHT);
        return banner;
    }

    /**
     * 构建"商品商城"页签：搜索栏 + 商品卡片网格。
     *
     * @return 页签内容
     */
    private VBox buildBuyTab() {
        _searchField.setPromptText("标题、描述或分类关键字");
        _searchField.setPrefWidth(300);

        Button queryButton = new Button("查询");
        queryButton.setOnAction(e -> loadGoods());
        Button refreshButton = new Button("刷新");
        refreshButton.setOnAction(e -> loadGoods());

        HBox searchBar = new HBox(10, _categoryBox, _searchField, queryButton, refreshButton);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(10, 12, 10, 12));
        searchBar.setStyle("-fx-background-color: white; -fx-background-radius: 12;"
                + " -fx-border-radius: 12; -fx-border-color: rgba(0,0,0,0.06);"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0.1, 0, 2);");

        _goodsCards.setPadding(new Insets(12));
        ScrollPane scroll = new ScrollPane(_goodsCards);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox box = new VBox(10, searchBar, scroll);
        box.setPadding(new Insets(12, 16, 12, 16));
        return box;
    }

    /**
     * 构建"我的订单"页签。
     *
     * @return 页签内容
     */
    private VBox buildRecordsTab() {
        setUpRecordsColumns(_recordsTable);
        VBox.setVgrow(_recordsTable, Priority.ALWAYS);

        Button refreshButton = new Button("刷新订单");
        refreshButton.setOnAction(e -> loadRecords());
        HBox bar = new HBox(refreshButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6));

        VBox box = new VBox(8, bar, _recordsTable);
        box.setPadding(new Insets(12, 16, 12, 16));
        return box;
    }

    /**
     * 构建"商品管理"页签（仅管理员可见）。
     *
     * @return 页签内容
     */
    private VBox buildManageTab() {
        _goodsIdField.setPromptText("商品编号");
        _goodsNameField.setPromptText("名称");
        _categoryField.setPromptText("类别");
        _priceField.setPromptText("单价");
        _stockField.setPromptText("库存");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.add(new Label("编号"), 0, 0);
        form.add(_goodsIdField, 1, 0);
        form.add(new Label("名称"), 0, 1);
        form.add(_goodsNameField, 1, 1);
        form.add(new Label("类别"), 0, 2);
        form.add(_categoryField, 1, 2);
        form.add(new Label("单价"), 0, 3);
        form.add(_priceField, 1, 3);
        form.add(new Label("库存"), 0, 4);
        form.add(_stockField, 1, 4);

        Button addButton = new Button("新增");
        addButton.setOnAction(e -> onAddGoods());
        Button updateButton = new Button("修改");
        updateButton.setOnAction(e -> onUpdateGoods());
        Button deleteButton = new Button("删除");
        deleteButton.setOnAction(e -> onDeleteGoods());
        Button clearButton = new Button("清空表单");
        clearButton.setOnAction(e -> clearForm());

        HBox buttons = new HBox(10, addButton, updateButton, deleteButton, clearButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        _manageTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                fillForm(sel);
            }
        });
        setUpGoodsColumns(_manageTable);
        VBox.setVgrow(_manageTable, Priority.ALWAYS);

        VBox box = new VBox(8, new Label("录入商品（管理员）"), form, buttons,
                new Label("商品列表"), _manageTable);
        box.setPadding(new Insets(12, 16, 12, 16));
        return box;
    }

    /**
     * 设置商品表格的列（用于商品管理）。
     *
     * @param table 表格
     */
    private void setUpGoodsColumns(TableView<Goods> table) {
        table.getColumns().add(column("商品编号", g -> safe(g.getGoodsId()), 100));
        table.getColumns().add(column("名称", g -> safe(g.getGoodsName()), 160));
        table.getColumns().add(column("类别", g -> safe(g.getCategory()), 110));
        table.getColumns().add(column("单价", g -> g.getPrice() == null ? "" : g.getPrice().toPlainString(), 90));
        table.getColumns().add(column("库存", g -> String.valueOf(g.getStock()), 70));
    }

    /**
     * 设置购买记录表格的列。
     *
     * @param table 表格
     */
    private void setUpRecordsColumns(TableView<PurchaseRecord> table) {
        table.getColumns().add(column("订单号", r -> safe(r.getOrderId()), 200));
        table.getColumns().add(column("商品名称", r -> safe(r.getGoodsName()), 150));
        table.getColumns().add(column("数量", r -> String.valueOf(r.getQuantity()), 70));
        table.getColumns().add(column("总价", r -> r.getTotalPrice() == null ? "" : r.getTotalPrice().toPlainString(), 90));
        table.getColumns().add(column("下单时间", r -> r.getOrderTime() == null ? "" : r.getOrderTime().format(TIME_FMT), 180));
    }

    /**
     * 生成一个文本列。
     *
     * @param title  列标题
     * @param getter 取值函数
     * @param width  列宽
     * @param <T>    行数据类型
     * @return 表格列
     */
    private <T> TableColumn<T, String> column(String title, Function<T, String> getter, double width) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> new SimpleStringProperty(getter.apply(cd.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    /**
     * 加载商品列表并刷新卡片网格与管理列表。
     */
    @SuppressWarnings("unchecked") // 服务器返回的 List 元素类型在运行时是确定的，此处强转安全
    private void loadGoods() {
        String keyword = _searchField.getText().trim();
        String category = _categoryBox.getValue();
        if ("全部商品".equals(category)) {
            category = null;
        }
        try {
            Message response = _storeClientSrv.queryGoods(keyword, category);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                List<Goods> goods = (List<Goods>) response.getData();
                renderGoodsCards(goods);
                _manageTable.getItems().setAll(goods);
            } else {
                showAlert(Alert.AlertType.ERROR, "查询失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器，请确认服务器已启动：" + e.getMessage());
        }
    }

    /**
     * 把商品集合渲染为卡片网格。
     *
     * @param goods 商品集合
     */
    private void renderGoodsCards(List<Goods> goods) {
        _goodsCards.getChildren().clear();
        if (goods == null || goods.isEmpty()) {
            Label empty = new Label("暂无商品");
            empty.setTextFill(Color.web("#9aa5b1"));
            empty.setFont(Font.font("System", 14));
            _goodsCards.getChildren().add(empty);
            return;
        }
        for (Goods g : goods) {
            _goodsCards.getChildren().add(buildGoodsCard(g));
        }
    }

    /**
     * 构建单个商品卡片。
     *
     * @param g 商品
     * @return 卡片
     */
    private VBox buildGoodsCard(Goods g) {
        VBox card = new VBox(8);
        card.setPrefWidth(210);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;"
                + " -fx-border-radius: 14; -fx-border-color: rgba(0,0,0,0.06);"
                + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0.1, 0, 4);");

        // 占位"图"：类别色块 + 类别 emoji；有图片地址则后台加载真实图片
        StackPane img = buildPlaceholderImage(g);
        final StackPane imgHolder = img;
        String url = g.getImageUrl();
        if (url != null && !url.isBlank()) {
            // 网络地址直接加载；相对路径（相对于项目根目录）转成 file: URI，与 CampusBackground 一致
            final String imageUri = (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file:"))
                    ? url
                    : new java.io.File(url).toURI().toString();
            new Thread(() -> {
                Image im = null;
                try {
                    im = new Image(imageUri, 186, 186, true, true);
                } catch (Exception e) {
                    im = null;
                }
                if (im != null && !im.isError()) {
                    ImageView iv = new ImageView(im);
                    iv.setFitWidth(186);
                    iv.setFitHeight(186);
                    iv.setPreserveRatio(true);
                    Platform.runLater(() -> {
                        imgHolder.setStyle(""); // 去掉占位色块，只显示商品图本身
                        imgHolder.getChildren().setAll(iv);
                    });
                }
            }).start();
        }

        Label name = new Label(safe(g.getGoodsName()));
        name.setWrapText(true);
        name.setFont(Font.font("System", FontWeight.BOLD, 15));
        name.setTextFill(Color.web("#1d2b39"));

        Label price = new Label("¥ " + g.getPrice().toPlainString());
        price.setFont(Font.font("System", FontWeight.BOLD, 18));
        price.setTextFill(Color.web("#e6604f"));

        Label stock = new Label("库存 " + g.getStock());
        stock.setTextFill(Color.web("#9aa5b1"));
        stock.setFont(Font.font("System", 12));

        Button buy = new Button("立即下单");
        buy.setMaxWidth(Double.MAX_VALUE);
        buy.setStyle("-fx-background-color: #2fa89a; -fx-text-fill: white;"
                + " -fx-background-radius: 18; -fx-font-weight: bold; -fx-cursor: hand;");
        buy.setOnAction(e -> promptAndBuy(g));

        card.getChildren().addAll(img, name, price, stock, buy);
        return card;
    }

    /**
     * 根据类别返回占位色块颜色。
     *
     * @param category 类别
     * @return 颜色
     */
    private String categoryColor(String category) {
        if (category == null) {
            return "#8d99ae";
        }
        switch (category) {
            case "饮料":
                return "#38b5a6";
            case "食品":
                return "#e0a53b";
            case "文具":
                return "#4f8fc6";
            case "数码":
                return "#7c65e6";
            case "生活用品":
                return "#2fa89a";
            default:
                return "#8d99ae";
        }
    }

    /**
     * 构建卡片占位图（类别色块 + 类别 emoji）。
     *
     * @param g 商品
     * @return 占位图
     */
    private StackPane buildPlaceholderImage(Goods g) {
        StackPane img = new StackPane();
        img.setPrefSize(186, 186);
        img.setMinSize(186, 186);
        img.setMaxSize(186, 186);
        img.setStyle("-fx-background-radius: 10; -fx-background-color: " + categoryColor(g.getCategory()) + ";");
        Rectangle clip = new Rectangle(186, 186);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        img.setClip(clip);
        Label emoji = new Label(categoryEmoji(g.getCategory()));
        emoji.setFont(Font.font("System", 48));
        img.getChildren().add(emoji);
        return img;
    }

    /**
     * 根据类别返回占位 emoji。
     *
     * @param category 类别
     * @return emoji
     */
    private String categoryEmoji(String category) {
        if (category == null) {
            return "🛒";
        }
        switch (category) {
            case "饮料":
                return "🥤";
            case "食品":
                return "🍞";
            case "文具":
                return "📓";
            case "数码":
                return "💾";
            case "生活用品":
                return "🧺";
            default:
                return "🛒";
        }
    }

    /**
     * 弹出购买数量输入框并执行购买。
     *
     * @param g 商品
     */
    private void promptAndBuy(Goods g) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("购买数量");
        dialog.setHeaderText(safe(g.getGoodsName()));
        dialog.setContentText("购买数量：");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(result.get().trim());
        } catch (NumberFormatException e) {
            quantity = -1;
        }
        if (quantity <= 0) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "购买数量必须为正整数");
            return;
        }
        doBuy(g, quantity);
    }

    /**
     * 执行购买。
     *
     * @param g        商品
     * @param quantity 数量
     */
    private void doBuy(Goods g, int quantity) {
        try {
            Message response = _storeClientSrv.purchaseGoods(_currentUser.getUId(), g.getGoodsId(), quantity);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                PurchaseRecord rec = (PurchaseRecord) response.getData();
                showAlert(Alert.AlertType.INFORMATION, "购买成功",
                        "订单号：" + rec.getOrderId()
                                + "\n商品：" + rec.getGoodsName()
                                + "\n数量：" + rec.getQuantity()
                                + "\n总价：" + rec.getTotalPrice().toPlainString() + " 元");
                loadGoods();
            } else {
                showAlert(Alert.AlertType.ERROR, "购买失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 加载购买记录并刷新表格。
     */
    @SuppressWarnings("unchecked") // 服务器返回的 List 元素类型在运行时是确定的，此处强转安全
    private void loadRecords() {
        String userId = isAdmin() ? null : _currentUser.getUId();
        try {
            Message response = _storeClientSrv.queryPurchaseRecords(userId);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                List<PurchaseRecord> records = (List<PurchaseRecord>) response.getData();
                _recordsTable.getItems().setAll(records);
            } else {
                showAlert(Alert.AlertType.ERROR, "查询失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 新增商品（管理员）。
     */
    private void onAddGoods() {
        Goods goods = readFormGoods();
        if (goods == null) {
            return;
        }
        try {
            Message response = _storeClientSrv.addGoods(goods);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                showAlert(Alert.AlertType.INFORMATION, "提示", "新增商品成功");
                clearForm();
                loadGoods();
            } else {
                showAlert(Alert.AlertType.ERROR, "新增失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 修改商品（管理员）。
     */
    private void onUpdateGoods() {
        Goods selected = _manageTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先在列表中选中要修改的商品");
            return;
        }
        Goods goods = readFormGoods();
        if (goods == null) {
            return;
        }
        goods.setGoodsId(selected.getGoodsId());
        try {
            Message response = _storeClientSrv.updateGoods(goods);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                showAlert(Alert.AlertType.INFORMATION, "提示", "修改商品成功");
                clearForm();
                loadGoods();
            } else {
                showAlert(Alert.AlertType.ERROR, "修改失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 删除商品（管理员）。
     */
    private void onDeleteGoods() {
        Goods selected = _manageTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先在列表中选中要删除的商品");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText(null);
        confirm.setContentText("确定删除商品【" + selected.getGoodsName() + "】吗？");
        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == javafx.scene.control.ButtonType.OK) {
                try {
                    Message response = _storeClientSrv.deleteGoods(selected.getGoodsId());
                    if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "提示", "删除商品成功");
                        clearForm();
                        loadGoods();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "删除失败", String.valueOf(response.getData()));
                    }
                } catch (IOException | ClassNotFoundException e) {
                    showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
                }
            }
        });
    }

    /**
     * 从管理表单读取商品对象，并做基本校验。
     *
     * @return 校验通过时返回 {@link Goods}；否则返回 {@code null}
     */
    private Goods readFormGoods() {
        String goodsId = _goodsIdField.getText().trim();
        String goodsName = _goodsNameField.getText().trim();
        String category = _categoryField.getText().trim();
        String priceText = _priceField.getText().trim();
        String stockText = _stockField.getText().trim();

        if (goodsId.isEmpty() || goodsName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "商品编号和名称不能为空");
            return null;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(priceText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "单价必须为数字");
            return null;
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "单价不能为负");
            return null;
        }
        int stock;
        try {
            stock = Integer.parseInt(stockText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "库存必须为整数");
            return null;
        }
        if (stock < 0) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "库存不能为负");
            return null;
        }
        Goods goods = new Goods();
        goods.setGoodsId(goodsId);
        goods.setGoodsName(goodsName);
        goods.setCategory(category);
        goods.setPrice(price);
        goods.setStock(stock);
        return goods;
    }

    /**
     * 选中商品后回填表单。
     *
     * @param goods 选中的商品
     */
    private void fillForm(Goods goods) {
        _goodsIdField.setText(goods.getGoodsId());
        _goodsNameField.setText(goods.getGoodsName());
        _categoryField.setText(safe(goods.getCategory()));
        _priceField.setText(goods.getPrice() == null ? "" : goods.getPrice().toPlainString());
        _stockField.setText(String.valueOf(goods.getStock()));
    }

    /**
     * 清空管理表单。
     */
    private void clearForm() {
        _goodsIdField.clear();
        _goodsNameField.clear();
        _categoryField.clear();
        _priceField.clear();
        _stockField.clear();
        _manageTable.getSelectionModel().clearSelection();
    }

    /**
     * 显示提示对话框。
     *
     * @param type    提示类型
     * @param title   标题
     * @param content 内容
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 安全转文本，避免空值。
     *
     * @param value 原始值
     * @return 非空文本
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 客户端程序入口：直接启动本窗口（可独立演示；真实流程由主界面跳转进入）。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        launch(args);
    }
}
