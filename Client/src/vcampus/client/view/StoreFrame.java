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
import vcampus.common.vo.CartItem;
import vcampus.common.vo.Goods;
import vcampus.common.vo.Message;
import vcampus.common.vo.Order;
import vcampus.common.vo.Promotion;
import vcampus.common.vo.PurchaseRecord;
import vcampus.common.vo.User;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 虚拟商店模块客户端主界面（JavaFX，电商卡片风格）。
 *
 * <p>包含四个页签：商品商城（卡片网格 + 搜索/分类 + 加入购物车/立即下单）、购物车（改数量、
 * 删除、清空、结算）、我的订单（订单主从视图：上表订单、下表明细）、商品管理（仅管理员）。
 * 所有业务请求通过 {@link StoreClientSrv} 发送到服务器，客户端不直接访问数据库。
 * 管理员表单的显示由 {@link User#getURole()} 决定，保证非管理员看不到管理入口。</p>
 *
 * <p>界面样式集中在模块样式表 {@code store.css} 中（与选课模块 {@code course.css} 统一设计语言），
 * 样式类在代码里挂载，颜色、圆角、按钮形态均在样式表中统一调整。</p>
 *
 * <p>购物车说明：购物车只保存在客户端内存（{@code _cart}），不建数据库表，关闭窗口或重新
 * 进入模块即清空；结算时客户端只提交"商品编号 + 数量"，单价、库存、余额一律由服务器在同一个
 * 事务中校验与扣减，一次结算生成一个含多条明细的订单（对应设计说明书中"同一订单可包含多个
 * 商品"这一开放问题的实现）。商品卡片优先显示 {@link Goods#getImageUrl()} 指向的商品图片，
 * 无图时以"类别色块 + 类别图标"作为占位图。</p>
 */
public class StoreFrame extends Application {

    /** 下单时间格式。 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 当前登录用户。 */
    private User _currentUser;

    /** 商店客户端业务服务。 */
    private final IStoreClientSrv _storeClientSrv = new StoreClientSrv();

    // ---- 校园卡余额 ----
    private final Label _balanceLabel = new Label("余额：--");

    /** 最近一次查询到的余额数值（弹窗里展示用）。 */
    private BigDecimal _balanceValue;

    /** 商店界面的根节点（弹窗用它取所属窗口，保证弹窗居中显示）。 */
    private Node _ownerNode;

    // ---- 今日特价 ----
    /** 横幅上的"今日特价 N 件"提示。 */
    private final Label _promoCountLabel = new Label("今日特价：--");

    /** "只看特价"筛选开关（纯客户端筛选，不额外请求服务器）。 */
    private final ToggleButton _onlyPromoFilter = new ToggleButton("只看特价");

    /** 最近一次查询到的商品（用于本地重新筛选，不再打服务器）。 */
    private final List<Goods> _loadedGoods = new ArrayList<>();

    // ---- 商品商城 ----
    private FlowPane _goodsCards = new FlowPane(16, 16);
    private final TextField _searchField = new TextField();
    private final ComboBox<String> _categoryBox = new ComboBox<>();

    // ---- 购物车（仅保存在客户端内存中，关闭窗口或重新进入模块即清空）----
    private final List<CartItem> _cart = new ArrayList<>();
    private final VBox _cartRows = new VBox(8);
    private final Label _cartSummaryLabel = new Label("合计：¥0.00");
    private final Label _cartEmptyLabel = new Label("购物车是空的，去「商品商城」挑几件吧");
    private Tab _cartTab;
    private final Label _hintLabel = new Label();

    // ---- 我的订单（主从视图：上表为订单，下表为选中订单的明细）----
    private TableView<Order> _ordersTable = new TableView<>();
    private TableView<PurchaseRecord> _orderItemsTable = new TableView<>();

    // ---- 商品管理（管理员）----
    private TableView<Goods> _manageTable = new TableView<>();
    private final TextField _goodsIdField = new TextField();
    private final TextField _goodsNameField = new TextField();
    private final TextField _categoryField = new TextField();
    private final TextField _priceField = new TextField();
    private final TextField _stockField = new TextField();
    private final TextField _imageUrlField = new TextField();

    // ---- 活动管理（管理员：配置每日特价）----
    private TableView<Promotion> _promoTable = new TableView<>();
    private final TextField _promoIdField = new TextField();
    private final ComboBox<Goods> _promoGoodsBox = new ComboBox<>();
    private final ComboBox<String> _promoRateBox = new ComboBox<>();
    private final ComboBox<String> _promoWeekdayBox = new ComboBox<>();
    private final TextField _promoRemarkField = new TextField();

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
        applyStylesheet(root);
        _ownerNode = root;

        Button exitButton = new Button("退出");
        exitButton.getStyleClass().add("secondary");
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
        loadBalance();
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
        root.getStyleClass().add("store-root");
        root.setTop(buildBanner());

        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("store-tabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().add(new Tab("商品商城", buildBuyTab()));
        _cartTab = new Tab("购物车", buildCartTab());
        _cartTab.setClosable(false);
        tabPane.getTabs().add(_cartTab);
        tabPane.getTabs().add(new Tab("我的订单", buildOrdersTab()));
        if (isAdmin()) {
            tabPane.getTabs().add(new Tab("商品管理", buildManageTab()));
            tabPane.getTabs().add(new Tab("活动管理", buildPromotionTab()));
        }
        // 切到"我的订单"时才查询：进入模块不必多发一次请求，且每次查看都是最新记录
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && "我的订单".equals(selected.getText())) {
                loadOrders();
            } else if (selected != null && "活动管理".equals(selected.getText())) {
                loadPromotions();
            }
        });
        root.setCenter(tabPane);
        return root;
    }

    /**
     * 生成一个可嵌入主界面内容区的商店视图（{@code Node}），并加载商品。
     *
     * @return 商店内容节点
     */
    public Node createView() {
        BorderPane content = buildContent();
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        applyStylesheet(content);
        _ownerNode = content;
        loadGoods();
        loadBalance();
        return content;
    }

    /**
     * 为商店界面挂载模块样式表 {@code store.css}。
     *
     * <p>样式表以 classpath 资源方式加载（{@code bin/vcampus/client/view/store.css}，
     * 由 {@code build.bat} 从源码目录拷贝），与选课模块加载 {@code course.css} 的方式一致；
     * 挂在商店内容根节点上，因此只影响商店子树。</p>
     *
     * @param root 商店内容根节点
     */
    private void applyStylesheet(Parent root) {
        java.net.URL css = getClass().getResource("store.css");
        if (css != null) {
            root.getStylesheets().add(css.toExternalForm());
        }
    }

    /**
     * 构建顶部横幅。
     *
     * @return 横幅面板
     */
    private BorderPane buildBanner() {
        BorderPane banner = new BorderPane();
        banner.getStyleClass().add("store-header");

        VBox titleBlock = new VBox(2);
        Label title = new Label("东南大学 · 虚拟商店");
        title.getStyleClass().add("store-title");
        Label crumb = new Label("数字校园 / 虚拟商店");
        crumb.getStyleClass().add("store-subtitle");
        titleBlock.getChildren().addAll(title, crumb);
        banner.setLeft(titleBlock);

        _balanceLabel.getStyleClass().add("balance-badge");
        _promoCountLabel.getStyleClass().add("promo-badge");

        Button rechargeButton = new Button("充值");
        rechargeButton.getStyleClass().add("accent");
        rechargeButton.setOnAction(e -> onRecharge());

        Label userInfo = new Label("用户：" + safe(_currentUser == null ? "" : _currentUser.getUId())
                + "（" + safe(_currentUser == null ? "" : _currentUser.getURole()) + "）");
        userInfo.getStyleClass().add("store-subtitle");

        HBox right = new HBox(14, _balanceLabel, _promoCountLabel, rechargeButton, userInfo);
        right.setAlignment(Pos.CENTER_RIGHT);
        banner.setRight(right);
        BorderPane.setAlignment(right, Pos.CENTER_RIGHT);
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
        queryButton.getStyleClass().add("primary");
        queryButton.setOnAction(e -> loadGoods());
        Button refreshButton = new Button("刷新");
        refreshButton.getStyleClass().add("secondary");
        refreshButton.setOnAction(e -> loadGoods());

        // "只看特价"：纯客户端筛选，不打服务器
        _onlyPromoFilter.getStyleClass().add("filter-toggle");
        _onlyPromoFilter.setOnAction(e -> renderGoodsCards(_loadedGoods));

        // 加入购物车后的轻提示（2.5 秒后自动消失），靠右显示
        _hintLabel.getStyleClass().add("cart-hint");
        _hintLabel.setMaxWidth(Double.MAX_VALUE);
        _hintLabel.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(_hintLabel, Priority.ALWAYS);

        HBox searchBar = new HBox(10, _categoryBox, _searchField, queryButton, refreshButton,
                _onlyPromoFilter, _hintLabel);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.getStyleClass().add("tool-bar-card");

        _goodsCards.setPadding(new Insets(12));
        ScrollPane scroll = new ScrollPane(_goodsCards);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("store-scroll");

        VBox box = new VBox(10, searchBar, scroll);
        box.getStyleClass().add("store-page");
        return box;
    }

    /**
     * 构建"我的订单"页签（主从视图）：上表为订单（一行一个订单），
     * 下表为选中订单的商品明细（一个订单可含多个商品）。
     *
     * @return 页签内容
     */
    private VBox buildOrdersTab() {
        setUpOrderColumns(_ordersTable);
        setUpOrderItemColumns(_orderItemsTable);
        VBox.setVgrow(_ordersTable, Priority.ALWAYS);

        // 选中订单后，把该订单的明细显示在下方表格
        _ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            _orderItemsTable.getItems().setAll(selected == null ? List.of() : selected.getItems());
        });

        Button refreshButton = new Button("刷新订单");
        refreshButton.getStyleClass().add("secondary");
        refreshButton.setOnAction(e -> loadOrders());
        Label tip = new Label(isAdmin() ? "显示全部用户的订单，选中一行查看明细" : "显示本人订单，选中一行查看明细");
        tip.getStyleClass().add("cart-hint");
        HBox bar = new HBox(12, refreshButton, tip);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("tool-bar-card");

        Label itemsTitle = sectionTitle("订单明细");
        _orderItemsTable.setPrefHeight(200);
        _orderItemsTable.setMinHeight(140);

        VBox box = new VBox(10, bar, _ordersTable, itemsTitle, _orderItemsTable);
        box.getStyleClass().add("store-page");
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
        _imageUrlField.setPromptText("图片路径（可留空，留空则用类别图标）");
        _imageUrlField.setPrefWidth(360);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.getStyleClass().add("tool-bar-card");
        form.add(formLabel("编号"), 0, 0);
        form.add(_goodsIdField, 1, 0);
        form.add(formLabel("名称"), 0, 1);
        form.add(_goodsNameField, 1, 1);
        form.add(formLabel("类别"), 0, 2);
        form.add(_categoryField, 1, 2);
        form.add(formLabel("单价"), 0, 3);
        form.add(_priceField, 1, 3);
        form.add(formLabel("库存"), 0, 4);
        form.add(_stockField, 1, 4);
        form.add(formLabel("图片路径"), 0, 5);
        form.add(_imageUrlField, 1, 5);

        Button addButton = new Button("新增");
        addButton.getStyleClass().add("primary");
        addButton.setOnAction(e -> onAddGoods());
        Button updateButton = new Button("修改");
        updateButton.getStyleClass().add("primary");
        updateButton.setOnAction(e -> onUpdateGoods());
        Button deleteButton = new Button("删除");
        deleteButton.getStyleClass().add("danger");
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

        VBox box = new VBox(10, sectionTitle("录入商品（管理员）"), form, buttons,
                sectionTitle("商品列表"), _manageTable);
        box.getStyleClass().add("store-page");
        return box;
    }

    /**
     * 生成一个带样式的小节标题。
     *
     * @param text 标题文字
     * @return 标题标签
     */
    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    /**
     * 生成一个带样式的表单字段标签。
     *
     * @param text 标签文字
     * @return 标签
     */
    private Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    /**
     * 设置商品表格的列（用于商品管理）。
     *
     * @param table 表格
     */
    private void setUpGoodsColumns(TableView<Goods> table) {
        table.getStyleClass().add("store-table");
        // 与选课模块一致：列宽自适应填满表格，消除右侧空白
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("商品编号", g -> safe(g.getGoodsId()), 100));
        table.getColumns().add(column("名称", g -> safe(g.getGoodsName()), 160));
        table.getColumns().add(column("类别", g -> safe(g.getCategory()), 110));
        table.getColumns().add(column("单价", g -> g.getPrice() == null ? "" : g.getPrice().toPlainString(), 90));
        table.getColumns().add(column("库存", g -> String.valueOf(g.getStock()), 70));
    }

    /**
     * 设置订单表格的列（一行一个订单，订单可含多个商品）。
     *
     * @param table 表格
     */
    private void setUpOrderColumns(TableView<Order> table) {
        table.getStyleClass().add("store-table");
        // 与选课模块一致：列宽自适应填满表格，订单号不会被截断
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("订单号", o -> safe(o.getOrderId()), 260));
        table.getColumns().add(column("下单时间",
                o -> o.getOrderTime() == null ? "" : o.getOrderTime().format(TIME_FMT), 170));
        table.getColumns().add(column("商品种类", o -> o.getItemCount() + " 种", 90));
        table.getColumns().add(column("总件数", o -> String.valueOf(o.getTotalQuantity()), 80));
        table.getColumns().add(column("订单金额",
                o -> o.getTotalAmount() == null ? "" : "¥" + o.getTotalAmount().toPlainString(), 100));
    }

    /**
     * 设置订单明细表格的列（一行一个商品）。
     *
     * @param table 表格
     */
    private void setUpOrderItemColumns(TableView<PurchaseRecord> table) {
        table.getStyleClass().add("store-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("商品名称", r -> safe(r.getGoodsName()), 200));
        table.getColumns().add(column("单价", r -> unitPriceOf(r).toPlainString(), 100));
        table.getColumns().add(column("数量", r -> String.valueOf(r.getQuantity()), 80));
        table.getColumns().add(column("小计",
                r -> r.getTotalPrice() == null ? "" : r.getTotalPrice().toPlainString(), 100));
    }

    /**
     * 由"本行小计 ÷ 数量"反推单价（保留两位小数，避免除不尽抛异常）。
     *
     * @param record 明细行
     * @return 单价
     */
    private BigDecimal unitPriceOf(PurchaseRecord record) {
        if (record.getTotalPrice() == null || record.getQuantity() <= 0) {
            return BigDecimal.ZERO;
        }
        return record.getTotalPrice().divide(BigDecimal.valueOf(record.getQuantity()), 2, RoundingMode.HALF_UP);
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
                _loadedGoods.clear();
                _loadedGoods.addAll(goods);
                _promoCountLabel.setText("今日特价 " + countPromotion(goods) + " 件");
                refreshPromoGoods(goods); // 活动表单的商品下拉框跟着刷新
                renderGoodsCards(_loadedGoods);
                _manageTable.getItems().setAll(goods);
            } else {
                showAlert(Alert.AlertType.ERROR, "查询失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器，请确认服务器已启动：" + e.getMessage());
        }
    }

    /**
     * 统计商品列表中今日有折扣的商品数量（服务器返回时会带上午夜至今生效的活动）。
     *
     * @param goods 商品集合
     * @return 特价商品数量
     */
    private int countPromotion(List<Goods> goods) {
        int count = 0;
        for (Goods g : goods) {
            if (g.hasDiscount()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 把商品集合渲染为卡片网格（"只看特价"打开时只显示今日有折扣的商品）。
     *
     * @param goods 商品集合
     */
    private void renderGoodsCards(List<Goods> goods) {
        _goodsCards.getChildren().clear();
        boolean onlyPromo = _onlyPromoFilter.isSelected();
        List<Goods> show = new ArrayList<>();
        for (Goods g : goods == null ? List.<Goods>of() : goods) {
            if (!onlyPromo || g.hasDiscount()) {
                show.add(g);
            }
        }
        if (show.isEmpty()) {
            Label empty = new Label(onlyPromo ? "今天没有特价商品" : "暂无商品");
            empty.getStyleClass().add("empty-state-label");
            _goodsCards.getChildren().add(empty);
            return;
        }
        for (Goods g : show) {
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
        card.getStyleClass().add("goods-card");

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
        name.getStyleClass().add("goods-name");

        // 价格区：今日特价时显示"折扣角标 + 特价 + 划线原价"
        HBox priceRow = new HBox(6);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        if (g.hasDiscount()) {
            Label badge = new Label(g.getDiscountLabel());
            badge.getStyleClass().add("discount-badge");
            Label promoPrice = new Label("¥ " + g.getDiscountPrice().toPlainString());
            promoPrice.getStyleClass().add("goods-price");
            Label originPrice = new Label("¥ " + g.getPrice().toPlainString());
            originPrice.getStyleClass().add("goods-price-original");
            priceRow.getChildren().addAll(badge, promoPrice, originPrice);
        } else {
            Label price = new Label("¥ " + g.getPrice().toPlainString());
            price.getStyleClass().add("goods-price");
            priceRow.getChildren().add(price);
        }

        String stockText = "库存 " + g.getStock();
        if (g.hasDiscount() && g.getPromotionRemark() != null && !g.getPromotionRemark().isBlank()) {
            stockText += " · " + g.getPromotionRemark();
        }
        Label stock = new Label(stockText);
        stock.getStyleClass().add("goods-stock");

        Button buy = new Button("立即下单");
        buy.setMaxWidth(Double.MAX_VALUE);
        buy.getStyleClass().addAll("primary", "card-action");
        buy.setOnAction(e -> promptAndBuy(g));

        Button addToCart = new Button("加入购物车");
        addToCart.setMaxWidth(Double.MAX_VALUE);
        addToCart.getStyleClass().addAll("secondary", "card-action");
        addToCart.setOnAction(e -> addToCart(g, 1));

        HBox actions = new HBox(8, buy, addToCart);
        HBox.setHgrow(buy, Priority.ALWAYS);
        HBox.setHgrow(addToCart, Priority.ALWAYS);

        card.getChildren().addAll(img, name, priceRow, stock, actions);
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
        img.getStyleClass().add("goods-thumb");
        img.setStyle("-fx-background-color: " + categoryColor(g.getCategory()) + ";");
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
        String headline = safe(g.getGoodsName())
                + (g.hasDiscount()
                        ? "　今日特价 ¥" + plain(g.getDiscountPrice()) + "（" + g.getDiscountLabel()
                                + "，原价 ¥" + plain(g.getPrice()) + "）"
                        : "　单价 ¥" + plain(g.getPrice()))
                + "　|　当前库存 " + g.getStock();
        Optional<String> result = promptInput("立即下单", headline, "购买数量：", "1",
                "1", "2", "3", "5", "");
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
        if (quantity > g.getStock()) {
            showAlert(Alert.AlertType.WARNING, "库存不足",
                    "「" + safe(g.getGoodsName()) + "」当前库存只有 " + g.getStock() + " 件");
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
                loadBalance();
            } else {
                showAlert(Alert.AlertType.ERROR, "购买失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 查询并刷新校园卡余额显示。
     */
    private void loadBalance() {
        String userId = _currentUser == null ? null : _currentUser.getUId();
        if (userId == null || userId.isBlank()) {
            _balanceLabel.setText("余额：--");
            _balanceValue = null;
            return;
        }
        try {
            Message response = _storeClientSrv.queryBalance(userId);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode()) && response.getData() != null) {
                _balanceLabel.setText("余额：¥" + response.getData());
                _balanceValue = response.getData() instanceof BigDecimal
                        ? (BigDecimal) response.getData() : new BigDecimal(String.valueOf(response.getData()));
            } else {
                _balanceLabel.setText("余额：--");
                _balanceValue = null;
            }
        } catch (IOException | ClassNotFoundException e) {
            _balanceLabel.setText("余额：--");
            _balanceValue = null;
        }
    }

    /**
     * 校园卡充值（演示用：输入金额后直接加到余额上）。
     */
    private void onRecharge() {
        String userId = _currentUser == null ? null : _currentUser.getUId();
        if (userId == null || userId.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "提示", "未登录，无法充值");
            return;
        }
        Optional<String> result = promptInput("校园卡充值",
                "为账号 " + userId + " 充值　|　当前余额 " + plain(_balanceValue),
                "充值金额（元）：", "100", "50", "100", "200", "500", "元");
        if (result.isEmpty()) {
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(result.get().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "充值金额必须为数字");
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "充值金额必须为正数");
            return;
        }
        try {
            Message response = _storeClientSrv.recharge(userId, amount);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                showAlert(Alert.AlertType.INFORMATION, "充值成功",
                        "充值 " + amount.toPlainString() + " 元成功，当前余额：" + response.getData() + " 元");
                loadBalance();
            } else {
                showAlert(Alert.AlertType.ERROR, "充值失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 加载订单列表（含明细）并刷新订单表格；明细表格先清空，等用户选中订单再显示。
     */
    @SuppressWarnings("unchecked") // 服务器返回的 List 元素类型在运行时是确定的，此处强转安全
    private void loadOrders() {
        String userId = isAdmin() ? null : _currentUser.getUId();
        try {
            Message response = _storeClientSrv.queryOrders(userId);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                List<Order> orders = (List<Order>) response.getData();
                _ordersTable.getItems().setAll(orders);
                _orderItemsTable.getItems().clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "查询失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    // ==================== 活动管理（管理员：配置每日特价） ====================

    /**
     * 构建"活动管理"页签（仅管理员）：录入/修改/删除每日特价活动。
     *
     * <p>折扣率用可编辑下拉框（预设 9 折~5 折，也允许手填 0~1 之间的小数），
     * 生效星期用下拉框（每天 / 周一~周日），商品用下拉框（直接从商品列表里选，
     * 避免手打商品编号打错）。所有校验在服务器端还会再做一遍。</p>
     *
     * @return 页签内容
     */
    private VBox buildPromotionTab() {
        _promoIdField.setPromptText("活动编号，如 P023");
        _promoIdField.setPrefWidth(150);

        _promoGoodsBox.setPrefWidth(220);
        _promoGoodsBox.setPromptText("选择活动商品");
        _promoGoodsBox.setConverter(new StringConverter<Goods>() {
            @Override
            public String toString(Goods goods) {
                return goods == null ? "" : goods.getGoodsId() + " " + goods.getGoodsName();
            }

            @Override
            public Goods fromString(String string) {
                return null;
            }
        });

        _promoRateBox.getItems().setAll("0.90", "0.85", "0.80", "0.75", "0.70", "0.60", "0.50");
        _promoRateBox.setEditable(true);
        _promoRateBox.setValue("0.80");
        _promoRateBox.setPrefWidth(110);

        _promoWeekdayBox.getItems().setAll("每天", "周一", "周二", "周三", "周四", "周五", "周六", "周日");
        _promoWeekdayBox.getSelectionModel().selectFirst();
        _promoWeekdayBox.setPrefWidth(110);

        _promoRemarkField.setPromptText("活动说明，如「周三文具日」");
        _promoRemarkField.setPrefWidth(240);

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.getStyleClass().add("tool-bar-card");
        form.add(formLabel("活动编号"), 0, 0);
        form.add(_promoIdField, 1, 0);
        form.add(formLabel("商品"), 0, 1);
        form.add(_promoGoodsBox, 1, 1);
        form.add(formLabel("折扣率"), 0, 2);
        form.add(_promoRateBox, 1, 2);
        form.add(formLabel("生效星期"), 0, 3);
        form.add(_promoWeekdayBox, 1, 3);
        form.add(formLabel("活动说明"), 0, 4);
        form.add(_promoRemarkField, 1, 4);

        Button addButton = new Button("新增活动");
        addButton.getStyleClass().add("primary");
        addButton.setOnAction(e -> onAddPromotion());
        Button updateButton = new Button("修改活动");
        updateButton.getStyleClass().add("primary");
        updateButton.setOnAction(e -> onUpdatePromotion());
        Button deleteButton = new Button("删除活动");
        deleteButton.getStyleClass().add("danger");
        deleteButton.setOnAction(e -> onDeletePromotion());
        Button clearButton = new Button("清空表单");
        clearButton.setOnAction(e -> clearPromoForm());
        HBox buttons = new HBox(10, addButton, updateButton, deleteButton, clearButton);
        buttons.setAlignment(Pos.CENTER_LEFT);

        setUpPromotionColumns(_promoTable);
        VBox.setVgrow(_promoTable, Priority.ALWAYS);
        _promoTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                fillPromoForm(selected);
            }
        });

        Label tip = new Label("折扣率 0.80 表示 8 折（可手填 0~1 之间的小数）；"
                + "生效星期选「每天」就是常年特价；同一商品同一天只能有一条活动");
        tip.getStyleClass().add("cart-hint");

        VBox box = new VBox(10, sectionTitle("配置每日特价活动（管理员）"), form, buttons, tip,
                sectionTitle("全部活动"), _promoTable);
        box.getStyleClass().add("store-page");
        return box;
    }

    /**
     * 设置活动表格的列。
     *
     * @param table 表格
     */
    private void setUpPromotionColumns(TableView<Promotion> table) {
        table.getStyleClass().add("store-table");
        table.getStyleClass().add("promo-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("活动编号", p -> safe(p.getPromoId()), 110));
        table.getColumns().add(column("商品", p -> safe(p.getGoodsId()) + " " + safe(p.getGoodsName()), 220));
        table.getColumns().add(column("折扣", p -> p.getDiscountLabel(), 80));
        table.getColumns().add(column("生效", p -> p.getWeekdayLabel(), 90));
        table.getColumns().add(column("说明", p -> safe(p.getRemark()), 220));
    }

    /**
     * 加载全部活动并刷新活动表格。
     */
    @SuppressWarnings("unchecked") // 服务器返回的 List 元素类型在运行时是确定的，此处强转安全
    private void loadPromotions() {
        try {
            Message response = _storeClientSrv.queryPromotions();
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                List<Promotion> promotions = (List<Promotion>) response.getData();
                _promoTable.getItems().setAll(promotions);
            } else {
                showAlert(Alert.AlertType.ERROR, "查询失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 把表格中选中行的活动回填到表单（活动编号是主键，回填后锁定不可改）。
     *
     * @param promotion 活动
     */
    private void fillPromoForm(Promotion promotion) {
        _promoIdField.setText(safe(promotion.getPromoId()));
        _promoIdField.setDisable(true);
        _promoGoodsBox.getSelectionModel().select(findGoodsInBox(promotion.getGoodsId(), promotion.getGoodsName()));
        _promoRateBox.setValue(promotion.getDiscountRate() == null
                ? "0.80" : promotion.getDiscountRate().toPlainString());
        _promoWeekdayBox.getSelectionModel().select(promotion.getWeekday());
        _promoRemarkField.setText(safe(promotion.getRemark()));
    }

    /**
     * 在商品下拉框中查找商品；找不到时补一个占位项（例如商品列表被筛选过）。
     *
     * @param goodsId   商品编号
     * @param goodsName 商品名称
     * @return 下拉框中的商品项
     */
    private Goods findGoodsInBox(String goodsId, String goodsName) {
        for (Goods g : _promoGoodsBox.getItems()) {
            if (goodsId != null && goodsId.equals(g.getGoodsId())) {
                return g;
            }
        }
        Goods placeholder = new Goods(goodsId, goodsName, null, BigDecimal.ZERO, 0);
        _promoGoodsBox.getItems().add(placeholder);
        return placeholder;
    }

    /**
     * 清空活动表单并解除活动编号的锁定。
     */
    private void clearPromoForm() {
        _promoIdField.clear();
        _promoIdField.setDisable(false);
        _promoGoodsBox.getSelectionModel().clearSelection();
        _promoRateBox.setValue("0.80");
        _promoWeekdayBox.getSelectionModel().selectFirst();
        _promoRemarkField.clear();
        _promoTable.getSelectionModel().clearSelection();
    }

    /**
     * 从表单读取一个活动对象；字段不合法时弹提示并返回 {@code null}。
     *
     * @return 活动对象；校验不通过时返回 {@code null}
     */
    private Promotion readPromoForm() {
        String promoId = _promoIdField.getText() == null ? "" : _promoIdField.getText().trim();
        if (promoId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请填写活动编号（如 P023）");
            return null;
        }
        Goods goods = _promoGoodsBox.getSelectionModel().getSelectedItem();
        if (goods == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请选择活动商品");
            return null;
        }
        BigDecimal rate;
        try {
            rate = new BigDecimal(String.valueOf(_promoRateBox.getValue()).trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "折扣率必须是数字，例如 0.80 表示 8 折");
            return null;
        }
        if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "折扣率必须在 0 到 1 之间（0.80 表示 8 折）");
            return null;
        }
        int weekday = Math.max(0, _promoWeekdayBox.getSelectionModel().getSelectedIndex());
        String remark = _promoRemarkField.getText() == null ? "" : _promoRemarkField.getText().trim();
        return new Promotion(promoId, goods.getGoodsId(), rate, weekday, remark);
    }

    /**
     * 新增活动（管理员）。
     */
    private void onAddPromotion() {
        Promotion promotion = readPromoForm();
        if (promotion == null) {
            return;
        }
        try {
            Message response = _storeClientSrv.addPromotion(promotion);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                showAlert(Alert.AlertType.INFORMATION, "提示", "新增活动成功");
                clearPromoForm();
                loadPromotions();
                loadGoods(); // 商品列表上的折扣展示同步刷新
            } else {
                showAlert(Alert.AlertType.ERROR, "新增失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 修改活动（管理员，按活动编号定位）。
     */
    private void onUpdatePromotion() {
        Promotion promotion = readPromoForm();
        if (promotion == null) {
            return;
        }
        try {
            Message response = _storeClientSrv.updatePromotion(promotion);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                showAlert(Alert.AlertType.INFORMATION, "提示", "修改活动成功");
                clearPromoForm();
                loadPromotions();
                loadGoods();
            } else {
                showAlert(Alert.AlertType.ERROR, "修改失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 删除活动（管理员，二次确认）。
     */
    private void onDeletePromotion() {
        Promotion selected = _promoTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先在列表中选中要删除的活动");
            return;
        }
        if (!confirm("确认删除", "确定删除活动【" + safe(selected.getPromoId()) + " "
                + safe(selected.getGoodsName()) + " " + selected.getDiscountLabel() + "】吗？")) {
            return;
        }
        try {
            Message response = _storeClientSrv.deletePromotion(selected.getPromoId());
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                showAlert(Alert.AlertType.INFORMATION, "提示", "删除活动成功");
                clearPromoForm();
                loadPromotions();
                loadGoods();
            } else {
                showAlert(Alert.AlertType.ERROR, "删除失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 刷新活动表单里的商品下拉框（尽量保持当前选中项）。
     *
     * @param goods 商品列表
     */
    private void refreshPromoGoods(List<Goods> goods) {
        Goods selected = _promoGoodsBox.getSelectionModel().getSelectedItem();
        _promoGoodsBox.getItems().setAll(goods);
        if (selected != null) {
            _promoGoodsBox.getSelectionModel().select(findGoodsInBox(selected.getGoodsId(), selected.getGoodsName()));
        }
    }

    // ==================== 购物车（仅保存在客户端内存中） ====================

    /**
     * 构建"购物车"页签：条目列表 + 合计/清空/结算。
     *
     * @return 页签内容
     */
    private VBox buildCartTab() {
        _cartEmptyLabel.getStyleClass().add("empty-state-label");
        _cartSummaryLabel.getStyleClass().add("cart-total");

        ScrollPane scroll = new ScrollPane(_cartRows);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("store-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button clearButton = new Button("清空购物车");
        clearButton.getStyleClass().add("secondary");
        clearButton.setOnAction(e -> {
            if (_cart.isEmpty()) {
                return;
            }
            if (confirm("清空购物车", "确定要清空购物车中的 " + _cart.size() + " 种商品吗？")) {
                _cart.clear();
                renderCart();
            }
        });

        Button checkoutButton = new Button("结算");
        checkoutButton.getStyleClass().add("primary");
        checkoutButton.setOnAction(e -> doCheckout());

        HBox footer = new HBox(12, _cartSummaryLabel, clearButton, checkoutButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("tool-bar-card");

        VBox box = new VBox(10, scroll, footer);
        box.getStyleClass().add("store-page");
        renderCart();
        return box;
    }

    /**
     * 把商品加入购物车；购物车中已有同一商品时累加数量。
     *
     * @param goods    商品
     * @param quantity 数量（正整数）
     */
    private void addToCart(Goods goods, int quantity) {
        if (goods == null || quantity <= 0) {
            return;
        }
        if (goods.getStock() <= 0) {
            showAlert(Alert.AlertType.WARNING, "无法加入购物车", "「" + safe(goods.getGoodsName()) + "」已无库存");
            return;
        }
        CartItem exist = findCartItem(goods.getGoodsId());
        if (exist == null) {
            _cart.add(new CartItem(goods.getGoodsId(), goods.getGoodsName(), goods.getEffectivePrice(), quantity));
        } else {
            if (exist.getQuantity() + quantity > goods.getStock()) {
                showAlert(Alert.AlertType.WARNING, "数量超出库存",
                        "「" + safe(goods.getGoodsName()) + "」当前库存 " + goods.getStock()
                                + "，购物车中已有 " + exist.getQuantity() + " 件");
                return;
            }
            exist.setQuantity(exist.getQuantity() + quantity);
            // 价格可能被管理员改过、也可能今天新上了活动，刷新为最新的成交价快照
            exist.setUnitPrice(goods.getEffectivePrice());
        }
        renderCart();
        String hint = "已加入购物车：" + safe(goods.getGoodsName()) + " ×" + quantity;
        if (goods.hasDiscount()) {
            hint += "（" + goods.getDiscountLabel() + " ¥" + plain(goods.getDiscountPrice()) + "）";
        }
        showHint(hint);
    }

    /**
     * 在购物车中查找某个商品的条目。
     *
     * @param goodsId 商品编号
     * @return 条目；不存在返回 {@code null}
     */
    private CartItem findCartItem(String goodsId) {
        for (CartItem item : _cart) {
            if (goodsId != null && goodsId.equals(item.getGoodsId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 修改购物车条目的数量；减到 0 时把该条目移出购物车。
     *
     * @param item  条目
     * @param delta 变化量（+1 / -1）
     */
    private void changeCartQuantity(CartItem item, int delta) {
        int next = item.getQuantity() + delta;
        if (next <= 0) {
            _cart.remove(item);
        } else {
            item.setQuantity(next);
        }
        renderCart();
    }

    /**
     * 重新渲染购物车：条目行、合计金额与页签标题（含件数）。
     */
    private void renderCart() {
        _cartRows.getChildren().clear();
        if (_cart.isEmpty()) {
            _cartRows.getChildren().add(_cartEmptyLabel);
        } else {
            for (CartItem item : _cart) {
                _cartRows.getChildren().add(buildCartRow(item));
            }
        }
        _cartSummaryLabel.setText("合计 " + _cart.size() + " 种 / " + cartQuantity()
                + " 件，共 ¥" + plain(cartTotal()));
        if (_cartTab != null) {
            _cartTab.setText(_cart.isEmpty() ? "购物车" : "购物车 (" + cartQuantity() + ")");
        }
    }

    /**
     * 构建购物车中的一行：名称、单价、数量增减、小计、删除。
     *
     * @param item 购物车条目
     * @return 行容器
     */
    private HBox buildCartRow(CartItem item) {
        Label name = new Label(safe(item.getGoodsName()));
        name.getStyleClass().add("cart-name");
        name.setMinWidth(160);
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);

        Label unit = new Label("¥" + plain(item.getUnitPrice()) + " / 件");
        unit.getStyleClass().add("cart-unit");
        unit.setMinWidth(110);

        Button minus = new Button("－");
        minus.getStyleClass().add("qty-button");
        minus.setOnAction(e -> changeCartQuantity(item, -1));

        Label qty = new Label(String.valueOf(item.getQuantity()));
        qty.getStyleClass().add("cart-qty");
        qty.setMinWidth(50);
        qty.setAlignment(Pos.CENTER);

        Button plus = new Button("＋");
        plus.getStyleClass().add("qty-button");
        plus.setOnAction(e -> changeCartQuantity(item, 1));

        Label subtotal = new Label("¥" + plain(item.getSubtotal()));
        subtotal.getStyleClass().add("cart-subtotal");
        subtotal.setMinWidth(100);

        Button remove = new Button("删除");
        remove.getStyleClass().add("danger");
        remove.setOnAction(e -> {
            _cart.remove(item);
            renderCart();
        });

        HBox row = new HBox(10, name, unit, minus, qty, plus, subtotal, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("cart-row");
        return row;
    }

    /**
     * 购物车合计金额。
     *
     * @return 合计金额
     */
    private BigDecimal cartTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : _cart) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }

    /**
     * 购物车总件数。
     *
     * @return 件数
     */
    private int cartQuantity() {
        int sum = 0;
        for (CartItem item : _cart) {
            sum += item.getQuantity();
        }
        return sum;
    }

    /**
     * 购物车结算：把购物车中的多个商品作为一个订单提交给服务器。
     *
     * <p>只把"商品编号 + 数量"发给服务器，单价与总价由服务器按数据库实时数据计算；
     * 成功后清空购物车并刷新余额、商品与订单列表，失败（库存不足/余额不足）时保留购物车。</p>
     */
    private void doCheckout() {
        if (_cart.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "提示", "购物车是空的，请先挑选商品");
            return;
        }
        String userId = _currentUser == null ? null : _currentUser.getUId();
        if (userId == null || userId.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "提示", "未登录，无法结算");
            return;
        }

        StringBuilder message = new StringBuilder();
        for (CartItem item : _cart) {
            message.append("· ").append(safe(item.getGoodsName())).append(" ×").append(item.getQuantity())
                    .append(" = ¥").append(plain(item.getSubtotal())).append('\n');
        }
        message.append("\n合计 ").append(cartQuantity()).append(" 件，应付 ¥").append(plain(cartTotal()))
                .append(" 元。\n结算后将生成一个订单并扣减校园卡余额；\n金额以服务器按当日活动与库存实时计算为准。");
        if (!confirm("确认结算", message.toString())) {
            return;
        }

        List<CartItem> items = new ArrayList<>();
        for (CartItem item : _cart) {
            items.add(new CartItem(item.getGoodsId(), item.getQuantity()));
        }
        try {
            Message response = _storeClientSrv.checkout(userId, items);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                Order order = (Order) response.getData();
                StringBuilder detail = new StringBuilder();
                for (PurchaseRecord record : order.getItems()) {
                    detail.append("· ").append(safe(record.getGoodsName())).append(" ×").append(record.getQuantity())
                            .append(" = ").append(plain(record.getTotalPrice())).append(" 元\n");
                }
                _cart.clear();
                renderCart();
                loadBalance();
                loadGoods();
                showAlert(Alert.AlertType.INFORMATION, "结算成功",
                        "订单号：" + order.getOrderId()
                                + "\n商品共 " + order.getItemCount() + " 种 / " + order.getTotalQuantity() + " 件"
                                + "\n订单金额：" + plain(order.getTotalAmount()) + " 元\n\n" + detail);
            } else {
                showAlert(Alert.AlertType.ERROR, "结算失败", String.valueOf(response.getData()));
                loadGoods(); // 失败多半是库存/价格发生变化，刷新商品列表让用户看到最新库存
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器：" + e.getMessage());
        }
    }

    /**
     * 在搜索栏右侧显示一条轻提示，2.5 秒后自动消失。
     *
     * @param text 提示文字
     */
    private void showHint(String text) {
        _hintLabel.setText(text);
        PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
        pause.setOnFinished(e -> _hintLabel.setText(""));
        pause.play();
    }

    /**
     * 弹出一个与商店风格一致的二次确认框（青绿渐变标题栏 + 白底内容 + 药丸按钮）。
     *
     * @param title   标题
     * @param message 提示内容
     * @return 用户点击"确定"返回 {@code true}
     */
    private boolean confirm(String title, String message) {
        ButtonType okType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        DialogPane pane = buildStyledPane(title, "请确认下面的操作", okType, cancelType);
        Label content = new Label(message);
        content.getStyleClass().add("dialog-prompt");
        content.setWrapText(true);
        content.setMaxWidth(420);
        pane.setContent(content);
        styleButton(pane, okType, "primary");
        styleButton(pane, cancelType, "secondary");
        dialog.setDialogPane(pane);
        initOwnerIfPossible(dialog);
        return dialog.showAndWait().orElse(cancelType) == okType;
    }

    /**
     * 构建一个与商店风格一致的输入弹窗（充值金额、下单数量用）。
     *
     * @param title       窗口标题
     * @param headline    副标题（显示商品/账号与价格等上下文）
     * @param promptText  输入框前面的说明文字
     * @param defaultValue 输入框默认值
     * @param quickValues 快捷按钮的取值（可为空）
     * @param quickSuffix 快捷按钮文字后缀（如"元"，数量场景传空串）
     * @return 用户输入的内容；点取消返回 {@link Optional#empty()}
     */
    private Optional<String> promptInput(String title, String headline, String promptText, String defaultValue,
                                         String quick1, String quick2, String quick3, String quick4,
                                         String quickSuffix) {
        ButtonType okType = new ButtonType("确认", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        DialogPane pane = buildStyledPane(title, headline, okType, cancelType);

        Label prompt = new Label(promptText);
        prompt.getStyleClass().add("dialog-prompt");
        TextField field = new TextField(defaultValue);
        field.setPrefWidth(200);
        HBox inputRow = new HBox(10, prompt, field);
        inputRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12, inputRow);
        String[] quicks = {quick1, quick2, quick3, quick4};
        HBox quickRow = new HBox(8);
        quickRow.setAlignment(Pos.CENTER_LEFT);
        Label quickLabel = new Label("快捷：");
        quickLabel.getStyleClass().add("dialog-hint");
        quickRow.getChildren().add(quickLabel);
        for (String value : quicks) {
            if (value == null || value.isBlank()) {
                continue;
            }
            Button chip = new Button(value + quickSuffix);
            chip.getStyleClass().add("dialog-chip");
            chip.setOnAction(e -> {
                field.setText(value);
                field.requestFocus();
            });
            quickRow.getChildren().add(chip);
        }
        if (quickRow.getChildren().size() > 1) {
            content.getChildren().add(quickRow);
        }
        pane.setContent(content);
        styleButton(pane, okType, "primary");
        styleButton(pane, cancelType, "secondary");

        dialog.setDialogPane(pane);
        dialog.setResultConverter(buttonType -> buttonType == okType ? field.getText() : null);
        initOwnerIfPossible(dialog);
        Platform.runLater(() -> {
            field.requestFocus();
            field.selectAll();
        });
        Optional<String> result = dialog.showAndWait();
        return result == null ? Optional.empty() : result;
    }

    /**
     * 构建弹窗面板：顶部青绿渐变标题栏 + 白底内容区，并挂上商店样式表。
     *
     * @param title       标题（白色大字）
     * @param headline    副标题（浅色小字，可为空）
     * @param buttonTypes 按钮类型
     * @return 弹窗面板
     */
    private DialogPane buildStyledPane(String title, String headline, ButtonType... buttonTypes) {
        DialogPane pane = new DialogPane();
        pane.getStyleClass().addAll("store-root", "store-dialog");
        applyStylesheet(pane);
        pane.getButtonTypes().setAll(buttonTypes);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");
        VBox header = new VBox(4);
        header.getStyleClass().add("dialog-header");
        header.getChildren().add(titleLabel);
        if (headline != null && !headline.isBlank()) {
            Label headlineLabel = new Label(headline);
            headlineLabel.getStyleClass().add("dialog-headline");
            headlineLabel.setWrapText(true);
            headlineLabel.setMaxWidth(420);
            header.getChildren().add(headlineLabel);
        }
        pane.setHeader(header);
        return pane;
    }

    /**
     * 给弹窗里的按钮套上商店的按钮样式。
     *
     * @param pane        弹窗面板
     * @param buttonType  按钮类型
     * @param styleClass  样式类（primary / danger / secondary）
     */
    private void styleButton(DialogPane pane, ButtonType buttonType, String styleClass) {
        Node node = pane.lookupButton(buttonType);
        if (node instanceof Button button) {
            button.getStyleClass().add(styleClass);
            button.setDefaultButton(ButtonBar.ButtonData.OK_DONE.equals(buttonType.getButtonData()));
            button.setCancelButton(ButtonBar.ButtonData.CANCEL_CLOSE.equals(buttonType.getButtonData()));
        }
    }

    /**
     * 把弹窗挂到当前窗口上（居中显示、保持模态关系）；拿不到窗口时忽略。
     *
     * @param dialog 弹窗
     */
    private void initOwnerIfPossible(Dialog<?> dialog) {
        try {
            if (_ownerNode != null && _ownerNode.getScene() != null && _ownerNode.getScene().getWindow() != null) {
                dialog.initOwner(_ownerNode.getScene().getWindow());
            }
        } catch (Exception ignored) {
            // 独立窗口/测试场景下拿不到 owner，忽略即可
        }
    }

    /**
     * 把金额格式化为字符串（{@code null} 视为 0.00）。
     *
     * @param value 金额
     * @return 金额文本
     */
    private String plain(BigDecimal value) {
        return value == null ? "0.00" : value.toPlainString();
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
        String imageUrl = _imageUrlField.getText().trim();

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
        goods.setImageUrl(imageUrl.isEmpty() ? null : imageUrl);
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
        _imageUrlField.setText(safe(goods.getImageUrl()));
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
        _imageUrlField.clear();
        _manageTable.getSelectionModel().clearSelection();
    }

    /**
     * 显示提示对话框（与商店风格一致：青绿渐变标题栏 + 白底内容 + 药丸按钮；
     * 成功/提示用主色按钮，错误用红色按钮，一眼能看出结果）。
     *
     * @param type    提示类型
     * @param title   标题
     * @param content 内容
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        ButtonType okType = new ButtonType("知道了", ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        DialogPane pane = buildStyledPane(title, null, okType);
        Label label = new Label(content);
        label.getStyleClass().add("dialog-prompt");
        label.setWrapText(true);
        label.setMaxWidth(420);
        pane.setContent(label);
        boolean error = type == Alert.AlertType.ERROR;
        styleButton(pane, okType, error ? "danger" : "primary");
        dialog.setDialogPane(pane);
        initOwnerIfPossible(dialog);
        dialog.showAndWait();
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
