/*
 * StoreFrame
 *
 * Version 1.0
 *
 * 2026-09-04
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * 虚拟商店模块客户端主界面（JavaFX）。
 *
 * <p>包含三个页签：商品浏览与购买（普通用户）、我的购买记录、商品管理（仅管理员）。
 * 所有业务请求通过 {@link StoreClientSrv} 发送到服务器，客户端不直接访问数据库。
 * 管理员表单的显示由 {@link User#getURole()} 决定，保证非管理员看不到管理入口。</p>
 */
public class StoreFrame extends Application {

    /** 下单时间格式。 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 当前登录用户。 */
    private User _currentUser;

    /** 商店客户端业务服务。 */
    private final IStoreClientSrv _storeClientSrv = new StoreClientSrv();

    // ---- 商品浏览与购买 ----
    private TableView<Goods> _goodsTable = new TableView<>();
    private final TextField _searchField = new TextField();
    private final ComboBox<String> _categoryBox = new ComboBox<>();
    private final TextField _quantityField = new TextField();

    // ---- 购买记录 ----
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
     * JavaFX 启动入口：搭建界面并加载商品列表。
     *
     * @param stage 舞台
     */
    @Override
    public void start(Stage stage) {
        _categoryBox.getItems().setAll("全部", "食品", "饮料", "文具", "生活用品", "数码");
        _categoryBox.getSelectionModel().selectFirst();
        buildUi(stage);
        loadGoods();
        stage.show();
    }

    /**
     * 搭建窗口布局。
     *
     * @param stage 舞台
     */
    private void buildUi(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPrefSize(980, 680);
        root.setBackground(new Background(new BackgroundFill(
                Color.web("#f4f7fb"), CornerRadii.EMPTY, Insets.EMPTY)));

        root.setTop(buildBanner());

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().add(new Tab("商品浏览与购买", buildBuyTab()));
        tabPane.getTabs().add(new Tab("我的购买记录", buildRecordsTab()));
        if (isAdmin()) {
            tabPane.getTabs().add(new Tab("商品管理", buildManageTab()));
        }
        root.setCenter(tabPane);

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
    }

    /**
     * 构建顶部横幅。
     *
     * @return 横幅面板
     */
    private BorderPane buildBanner() {
        BorderPane banner = new BorderPane();
        banner.setPadding(new Insets(16, 26, 16, 26));
        banner.setStyle("-fx-background-color: linear-gradient(to right, #2d6a9f, #4f8fc6);");

        Label title = new Label("东南大学 · 虚拟商店");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        banner.setLeft(title);

        Label userInfo = new Label("用户：" + safe(_currentUser == null ? "" : _currentUser.getUId())
                + "（" + safe(_currentUser == null ? "" : _currentUser.getURole()) + "）");
        userInfo.setTextFill(Color.WHITE);
        userInfo.setFont(Font.font("System", 14));
        banner.setRight(userInfo);
        BorderPane.setAlignment(userInfo, Pos.CENTER_RIGHT);
        return banner;
    }

    /**
     * 构建"商品浏览与购买"页签。
     *
     * @return 页签内容
     */
    private VBox buildBuyTab() {
        _searchField.setPromptText("按名称/类别查询");
        _searchField.setPrefWidth(220);

        Button queryButton = new Button("查询");
        queryButton.setOnAction(e -> loadGoods());
        Button refreshButton = new Button("刷新");
        refreshButton.setOnAction(e -> loadGoods());
        Button buyButton = new Button("购买");
        buyButton.setOnAction(e -> onBuy());

        _quantityField.setPromptText("数量");

        HBox searchBar = new HBox(10, new Label("类别"), _categoryBox, _searchField,
                queryButton, refreshButton);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(10));

        setUpGoodsColumns(_goodsTable, 150);
        VBox.setVgrow(_goodsTable, javafx.scene.layout.Priority.ALWAYS);

        HBox buyBar = new HBox(10, new Label("购买数量"), _quantityField, buyButton,
                new Label("（选中商品后填写数量并购买）"));
        buyBar.setAlignment(Pos.CENTER_LEFT);
        buyBar.setPadding(new Insets(10));

        VBox box = new VBox(6, searchBar, _goodsTable, buyBar);
        box.setPadding(new Insets(8, 16, 8, 16));
        return box;
    }

    /**
     * 构建"我的购买记录"页签。
     *
     * @return 页签内容
     */
    private VBox buildRecordsTab() {
        setUpRecordsColumns(_recordsTable);

        Button refreshButton = new Button("刷新记录");
        refreshButton.setOnAction(e -> loadRecords());
        HBox bar = new HBox(refreshButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10));

        VBox box = new VBox(6, bar, _recordsTable);
        box.setPadding(new Insets(8, 16, 8, 16));
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
        setUpGoodsColumns(_manageTable, 150);
        VBox.setVgrow(_manageTable, javafx.scene.layout.Priority.ALWAYS);

        VBox box = new VBox(8, new Label("录入商品（管理员）"), form, buttons,
                new Label("商品列表"), _manageTable);
        box.setPadding(new Insets(8, 16, 8, 16));
        return box;
    }

    /**
     * 设置商品表格的列。
     *
     * @param table  表格
     * @param width  列宽
     */
    private void setUpGoodsColumns(TableView<Goods> table, double width) {
        table.getColumns().add(column("商品编号", g -> safe(g.getGoodsId()), width));
        table.getColumns().add(column("名称", g -> safe(g.getGoodsName()), width + 20));
        table.getColumns().add(column("类别", g -> safe(g.getCategory()), width - 20));
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
     * 加载商品列表并刷新表格。
     */
    @SuppressWarnings("unchecked") // 服务器返回的 List 元素类型在运行时是确定的，此处强转安全
    private void loadGoods() {
        String keyword = _searchField.getText().trim();
        String category = _categoryBox.getValue();
        if ("全部".equals(category)) {
            category = null;
        }
        try {
            Message response = _storeClientSrv.queryGoods(keyword, category);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                List<Goods> goods = (List<Goods>) response.getData();
                _goodsTable.getItems().setAll(goods);
                _manageTable.getItems().setAll(goods);
            } else {
                showAlert(Alert.AlertType.ERROR, "查询失败", String.valueOf(response.getData()));
            }
        } catch (IOException | ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "连接失败", "无法连接服务器，请确认服务器已启动：" + e.getMessage());
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
     * 购买商品。
     */
    private void onBuy() {
        Goods selected = _goodsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "提示", "请先选中要购买的商品");
            return;
        }
        int quantity;
        try {
            quantity = Integer.parseInt(_quantityField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "购买数量必须为整数");
            return;
        }
        if (quantity <= 0) {
            showAlert(Alert.AlertType.ERROR, "输入错误", "购买数量必须为正整数");
            return;
        }
        try {
            Message response = _storeClientSrv.purchaseGoods(_currentUser.getUId(), selected.getGoodsId(), quantity);
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
