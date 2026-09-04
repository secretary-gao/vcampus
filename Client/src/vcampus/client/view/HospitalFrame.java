/*
 * HospitalFrame
 *
 * Version 2.0 改造为可嵌入主界面面板，不再独立Application
 *
 * 2026-09-04
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;
import vcampus.client.biz.HospitalClientSrv;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.Message;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 医院挂号管理面板，改造为可嵌入MainFrame，不再独立Application。
 * 使用方式：
 * HospitalFrame hospitalFrame = new HospitalFrame(userId,role);
 * BorderPane view = hospitalFrame.createView();
 * hospitalFrame.attachStyleSheet(scene);
 * Platform.runLater(hospitalFrame::refresh);
 */
public class HospitalFrame {
    private BorderPane _root;
    private final String _loginUserId;
    private final String _loginUserRole;
    private final HospitalClientSrv _hospitalSrv = new HospitalClientSrv();
    // 导航按钮
    private Button btnQueryAllDoctor;
    private Button btnQueryByDept;
    private Button btnAddAppoint;
    private Button btnMyAppoint;
    private Button btnCancelAppoint;
    private Button btnQueryAllAppoint;
    private Button btnAddDoctor;
    private Button btnUpdateDoctor;
    private Button btnDeleteDoctor;
    private StackPane _contentPane;
    // ---------------------- 面板控件（TableView） ----------------------
    private VBox panelQueryAllDoctor;
    private TableView<Doctor> tableAllDoctor;
    private VBox panelQueryDept;
    private TextField tfDept;
    private TableView<Doctor> tableDeptDoctor;
    private VBox panelAddAppoint;
    private VBox panelMyAppoint;
    private TableView<Appointment> tableMyAppoint;
    private VBox panelCancelAppoint;
    private VBox panelQueryAllAppoint;
    private TableView<Appointment> tableAllAppoint;
    private VBox panelAddDoctor;
    private TextField tfAddDocId;
    private TextField tfAddDocName;
    private TextField tfAddDocDept;
    private TextField tfAddDocTitle;
    private VBox panelUpdateDoctor;
    private TextField tfUpdDocId;
    private TextField tfUpdDocName;
    private TextField tfUpdDocDept;
    private TextField tfUpdDocTitle;
    private VBox panelDeleteDoctor;

    public HospitalFrame(String loginUserId, String loginUserRole) {
        this._loginUserId = loginUserId;
        this._loginUserRole = loginUserRole;
    }

    /**
     * 创建可嵌入主界面的根面板，对应学籍模块 createView()
     * @return BorderPane 根UI
     */
    public BorderPane createView() {
        if (_root != null) {
            return _root;
        }
        _root = new BorderPane();
        _root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#f4f8fb")),
                        new Stop(1, Color.web("#e8f0f7"))),
                CornerRadii.EMPTY, Insets.EMPTY)));
        HBox topBanner = buildTopBanner();
        VBox mainContainer = new VBox(0);
        BorderPane bodyPane = new BorderPane();
        bodyPane.setLeft(buildSideBar());
        _contentPane = new StackPane();
        _contentPane.setPadding(new Insets(20));
        bodyPane.setCenter(_contentPane);
        mainContainer.getChildren().addAll(topBanner, bodyPane);
        _root.setCenter(mainContainer);
        initAllPanels();
        switchPanel(panelQueryAllDoctor);
        return _root;
    }

    /**
     * 嵌入完成后调用刷新初始数据，对应学籍 refresh()
     */
    public void refresh() {
        actionQueryAllDoctor();
    }

    /**
     * 样式表附加接口，当前医院无独立css，预留接口，和学籍模块保持一致
     */
    public void attachStyleSheet(Scene scene) {
        // 本模块无独立css文件，预留接口
    }

    private HBox buildTopBanner() {
        HBox banner = new HBox();
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(12, 30, 12, 30));
        banner.setStyle("-fx-background-color:#ffffff;-fx-border-color:#cce0ed;-fx-border-width:0 0 1 0;");
        Image logoImg;
        try {
            logoImg = new Image(getClass().getResource("seu_logo.jpeg").toExternalForm(), 120, 120, true, true, false);
        } catch (Exception e) {
            logoImg = null;
        }
        ImageView logoView = new ImageView(logoImg);
        Label systemTitle = new Label("🏥 医院挂号管理系统");
        systemTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
        systemTitle.setTextFill(Color.web("#194b75"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label userTopTip = new Label("使用者：" + _loginUserId + " | " + _loginUserRole);
        userTopTip.setFont(Font.font("System", 14));
        userTopTip.setTextFill(Color.web("#446078"));
        if (logoImg != null) {
            banner.getChildren().addAll(logoView, new Region(), systemTitle, spacer, userTopTip);
        } else {
            banner.getChildren().addAll(systemTitle, spacer, userTopTip);
        }
        HBox.setMargin(systemTitle, new Insets(0, 0, 0, 40));
        return banner;
    }

    private VBox buildSideBar() {
        VBox side = new VBox(12);
        side.setPrefWidth(240);
        side.setPadding(new Insets(28, 14, 14, 14));
        side.setStyle("-fx-background-color: #ffffff;"
                + "-fx-border-color:#cce0ed;"
                + "-fx-border-width:0 1 0 0;"
                + "-fx-effect: dropshadow(gaussian, rgba(80,130,170,0.08),10,0.1,1,0);");
        Label title = new Label("功能导航");
        title.setFont(Font.font("System", FontWeight.BOLD, 19));
        title.setTextFill(Color.web("#194b75"));
        Separator sep1 = new Separator();
        sep1.setPadding(new Insets(10, 0, 10, 0));
        Label lblUserFunc = new Label("▷ 用户操作");
        lblUserFunc.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblUserFunc.setTextFill(Color.web("#235782"));
        btnQueryAllDoctor = new Button("查询全部医生");
        btnQueryByDept = new Button("按科室查询医生");
        btnAddAppoint = new Button("预约挂号");
        btnMyAppoint = new Button("我的预约记录");
        btnCancelAppoint = new Button("取消预约");
        Separator sep2 = new Separator();
        sep2.setPadding(new Insets(10, 0, 10, 0));
        Label lblAdminFunc = new Label("▷ 管理员操作");
        lblAdminFunc.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblAdminFunc.setTextFill(Color.web("#235782"));
        btnQueryAllAppoint = new Button("查询全部预约");
        btnAddDoctor = new Button("新增医生");
        btnUpdateDoctor = new Button("修改医生");
        btnDeleteDoctor = new Button("删除医生");
        Button[] btns = {
                btnQueryAllDoctor, btnQueryByDept, btnAddAppoint, btnMyAppoint, btnCancelAppoint,
                btnQueryAllAppoint, btnAddDoctor, btnUpdateDoctor, btnDeleteDoctor
        };
        for (Button b : btns) {
            b.setMaxWidth(Double.MAX_VALUE);
            b.setPrefHeight(38);
            b.setStyle("-fx-background-color:#367ba9;-fx-text-fill:white;-fx-font-size:13px;-fx-background-radius:6;-fx-cursor:hand;");
            b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:#2b648c;-fx-text-fill:white;-fx-font-size:13px;-fx-background-radius:6;-fx-cursor:hand;"));
            b.setOnMouseExited(e -> b.setStyle("-fx-background-color:#367ba9;-fx-text-fill:white;-fx-font-size:13px;-fx-background-radius:6;-fx-cursor:hand;"));
        }
        // =========权限控制：管理员隐藏预约挂号、我的预约、取消预约=========
        if (!"管理员".equals(_loginUserRole)) {
            //普通用户：隐藏管理员按钮，显示全部用户按钮
            btnQueryAllAppoint.setVisible(false);
            btnAddDoctor.setVisible(false);
            btnUpdateDoctor.setVisible(false);
            btnDeleteDoctor.setVisible(false);
            lblAdminFunc.setVisible(false);
            sep2.setVisible(false);
        } else {
            //管理员：隐藏普通用户的3个预约功能
            btnAddAppoint.setVisible(false);
            btnMyAppoint.setVisible(false);
            btnCancelAppoint.setVisible(false);
        }
        btnQueryAllDoctor.setOnAction(e -> switchPanel(panelQueryAllDoctor));
        btnQueryByDept.setOnAction(e -> switchPanel(panelQueryDept));
        btnAddAppoint.setOnAction(e -> switchPanel(panelAddAppoint));
        btnMyAppoint.setOnAction(e -> switchPanel(panelMyAppoint));
        btnCancelAppoint.setOnAction(e -> switchPanel(panelCancelAppoint));
        btnQueryAllAppoint.setOnAction(e -> switchPanel(panelQueryAllAppoint));
        btnAddDoctor.setOnAction(e -> switchPanel(panelAddDoctor));
        btnUpdateDoctor.setOnAction(e -> switchPanel(panelUpdateDoctor));
        btnDeleteDoctor.setOnAction(e -> switchPanel(panelDeleteDoctor));
        side.getChildren().addAll(title, sep1,
                lblUserFunc,
                btnQueryAllDoctor, btnQueryByDept, btnAddAppoint, btnMyAppoint, btnCancelAppoint,
                sep2, lblAdminFunc,
                btnQueryAllAppoint, btnAddDoctor, btnUpdateDoctor, btnDeleteDoctor);
        return side;
    }

    private void switchPanel(VBox panel) {
        _contentPane.getChildren().clear();
        _contentPane.getChildren().add(wrapCard(panel));
    }

    private VBox wrapCard(VBox inner) {
        VBox card = new VBox();
        card.setMaxWidth(860);
        card.setPrefWidth(860);
        card.setPadding(new Insets(24, 22, 22, 22));
        card.setStyle("-fx-background-color: #ffffff;"
                + "-fx-background-radius:12;"
                + "-fx-border-radius:12;"
                + "-fx-border-color:#d0e1ec;"
                + "-fx-border-width:1;"
                + "-fx-effect: dropshadow(gaussian, rgba(60,110,150,0.09),14,0.1,0,4);");
        card.getChildren().add(inner);
        return card;
    }

    private void initAllPanels() {
        // ========== 1. 查询全部医生 ==========
        panelQueryAllDoctor = new VBox(14);
        panelQueryAllDoctor.setAlignment(Pos.TOP_LEFT);
        Label lab1 = new Label("医生信息管理 — 查询全部医生");
        lab1.setFont(Font.font("System", FontWeight.BOLD, 19));
        lab1.setTextFill(Color.web("#194b75"));
        Button btnLoadAllDoc = new Button("加载全部医生");
        btnLoadAllDoc.setOnAction(e -> actionQueryAllDoctor());
        tableAllDoctor = new TableView<>();
        tableAllDoctor.setPrefHeight(420);
        TableColumn<Doctor, String> colDocId = new TableColumn<>("医生编号");
        colDocId.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        TableColumn<Doctor, String> colName = new TableColumn<>("姓名");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Doctor, String> colDept = new TableColumn<>("科室");
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        TableColumn<Doctor, String> colTitle = new TableColumn<>("职称");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        tableAllDoctor.getColumns().addAll(colDocId, colName, colDept, colTitle);
        colDocId.setPrefWidth(140);
        colName.setPrefWidth(140);
        colDept.setPrefWidth(220);
        colTitle.setPrefWidth(220);
        panelQueryAllDoctor.getChildren().addAll(lab1, btnLoadAllDoc, tableAllDoctor);
        // ==========2.按科室查询医生 ==========
        panelQueryDept = new VBox(14);
        panelQueryDept.setAlignment(Pos.TOP_LEFT);
        Label lab2 = new Label("医生信息管理 — 按科室查询");
        lab2.setFont(Font.font("System", FontWeight.BOLD, 19));
        lab2.setTextFill(Color.web("#194b75"));
        tfDept = new TextField();
        tfDept.setPromptText("输入科室名称，例如：内科");
        tfDept.setStyle(fieldStyle());
        Button btnQDept = new Button("查询");
        btnQDept.setOnAction(e -> actionQueryDoctorByDept());
        tableDeptDoctor = new TableView<>();
        tableDeptDoctor.setPrefHeight(380);
        TableColumn<Doctor, String> c1 = new TableColumn<>("医生编号");
        c1.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        TableColumn<Doctor, String> c2 = new TableColumn<>("姓名");
        c2.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Doctor, String> c3 = new TableColumn<>("科室");
        c3.setCellValueFactory(new PropertyValueFactory<>("department"));
        TableColumn<Doctor, String> c4 = new TableColumn<>("职称");
        c4.setCellValueFactory(new PropertyValueFactory<>("title"));
        tableDeptDoctor.getColumns().addAll(c1, c2, c3, c4);
        c1.setPrefWidth(140);
        c2.setPrefWidth(140);
        c3.setPrefWidth(220);
        c4.setPrefWidth(220);
        panelQueryDept.getChildren().addAll(lab2, tfDept, btnQDept, tableDeptDoctor);
        // ==========3.预约挂号 ==========
        panelAddAppoint = new VBox(14);
        panelAddAppoint.setAlignment(Pos.TOP_LEFT);
        Label lab3 = new Label("预约挂号 — 新建就诊预约");
        lab3.setFont(Font.font("System", FontWeight.BOLD, 19));
        lab3.setTextFill(Color.web("#194b75"));
        Label tipAppoint = new Label("点击下方按钮，弹窗选择医生、日期和就诊时段");
        tipAppoint.setFont(Font.font("System", 14));
        tipAppoint.setTextFill(Color.web("#555555"));
        Button btnMakeAppoint = new Button("打开预约选择弹窗");
        btnMakeAppoint.setOnAction(e -> actionAddAppointment());
        panelAddAppoint.getChildren().addAll(lab3, tipAppoint, btnMakeAppoint);
        // ==========4.我的预约记录 ==========
        panelMyAppoint = new VBox(14);
        panelMyAppoint.setAlignment(Pos.TOP_LEFT);
        Label lab4 = new Label("预约记录 — 我的就诊预约");
        lab4.setFont(Font.font("System", FontWeight.BOLD, 19));
        lab4.setTextFill(Color.web("#194b75"));
        Button btnRefreshMy = new Button("刷新我的预约");
        btnRefreshMy.setOnAction(e -> actionQueryMyAppointment());
        tableMyAppoint = new TableView<>();
        tableMyAppoint.setPrefHeight(420);
        TableColumn<Appointment, String> aid = new TableColumn<>("预约编号");
        aid.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        TableColumn<Appointment, String> uid = new TableColumn<>("用户ID");
        uid.setCellValueFactory(new PropertyValueFactory<>("userId"));
        TableColumn<Appointment, String> did = new TableColumn<>("医生ID");
        did.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        TableColumn<Appointment, Date> atime = new TableColumn<>("预约时间");
        atime.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        TableColumn<Appointment, String> stat = new TableColumn<>("状态");
        stat.setCellValueFactory(new PropertyValueFactory<>("status"));
        stat.setCellFactory(col -> new TableCell<Appointment, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                } else if (value == null || value.isBlank()) {
                    setText("待就诊");
                } else {
                    setText(value);
                }
            }
        });
        tableMyAppoint.getColumns().addAll(aid, uid, did, atime, stat);
        aid.setPrefWidth(140);
        uid.setPrefWidth(110);
        did.setPrefWidth(110);
        atime.setPrefWidth(200);
        stat.setPrefWidth(140);
        panelMyAppoint.getChildren().addAll(lab4, btnRefreshMy, tableMyAppoint);
        // ==========5.取消预约 ==========
        panelCancelAppoint = new VBox(14);
        panelCancelAppoint.setAlignment(Pos.TOP_LEFT);
        Label lab5 = new Label("预约操作 — 取消就诊预约");
        lab5.setFont(Font.font("System", FontWeight.BOLD, 19));
        lab5.setTextFill(Color.web("#194b75"));
        Label tipCancel = new Label("点击按钮弹窗，选择本人【待就诊】预约记录进行取消");
        tipCancel.setFont(Font.font("System", 14));
        tipCancel.setTextFill(Color.web("#555555"));
        Button btnCancel = new Button("打开取消预约选择弹窗");
        btnCancel.setOnAction(e -> actionCancelAppointment());
        panelCancelAppoint.getChildren().addAll(lab5, tipCancel, btnCancel);
        // ==========管理员：全部预约表格【增加操作删除列】 ==========
        panelQueryAllAppoint = new VBox(14);
        panelQueryAllAppoint.setAlignment(Pos.TOP_LEFT);
        Label labA1 = new Label("【管理员】全部就诊预约记录");
        labA1.setFont(Font.font("System", FontWeight.BOLD, 19));
        labA1.setTextFill(Color.web("#194b75"));
        Button btnLoadAllAppoint = new Button("刷新全部预约");
        btnLoadAllAppoint.setOnAction(e -> actionQueryAllAppointment());
        tableAllAppoint = new TableView<>();
        tableAllAppoint.setPrefHeight(440);
        TableColumn<Appointment, String> aa = new TableColumn<>("预约编号");
        aa.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        TableColumn<Appointment, String> uu = new TableColumn<>("用户ID");
        uu.setCellValueFactory(new PropertyValueFactory<>("userId"));
        TableColumn<Appointment, String> dd = new TableColumn<>("医生ID");
        dd.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        TableColumn<Appointment, Date> tt = new TableColumn<>("预约时间");
        tt.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        TableColumn<Appointment, String> ss = new TableColumn<>("状态");
        ss.setCellValueFactory(new PropertyValueFactory<>("status"));
        ss.setCellFactory(col -> new TableCell<Appointment, String>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                } else if (value == null || value.isBlank()) {
                    setText("待就诊");
                } else {
                    setText(value);
                }
            }
        });
        tableAllAppoint.getColumns().addAll(aa, uu, dd, tt, ss);
        //新增操作删除按钮列：仅状态=已取消显示
        TableColumn<Appointment, Void> colOpt = new TableColumn<>("操作");
        colOpt.setPrefWidth(110);
        colOpt.setCellFactory(param -> new TableCell<Appointment, Void>() {
            private final Button btnDel = new Button("删除");
            {
                btnDel.setStyle("-fx-background-color:#dc3545;-fx-text-fill:white;-fx-font-size:12px;-fx-background-radius:4px;");
                btnDel.setOnAction(e -> {
                    Appointment rowData = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("确认删除");
                    confirm.setContentText("确定要永久删除这条【已取消】预约记录吗？该操作不可恢复！");
                    Optional<ButtonType> res = confirm.showAndWait();
                    if (res.isPresent() && res.get() == ButtonType.OK) {
                        String aid = rowData.getAppointmentId();
                        new Thread(() -> {
                            try {
                                Message resp = _hospitalSrv.deleteCancelAppointment(aid);
                                Platform.runLater(() -> {
                                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                                        showAlert(Alert.AlertType.INFORMATION, "成功", String.valueOf(resp.getData()));
                                        actionQueryAllAppointment();
                                    } else {
                                        showAlert(Alert.AlertType.ERROR, "失败", String.valueOf(resp.getData()));
                                    }
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", ex.getMessage()));
                            }
                        }).start();
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Appointment apt = getTableView().getItems().get(getIndex());
                    if ("已取消".equals(apt.getStatus())) {
                        setGraphic(btnDel);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
        tableAllAppoint.getColumns().add(colOpt);
        aa.setPrefWidth(140);
        uu.setPrefWidth(110);
        dd.setPrefWidth(110);
        tt.setPrefWidth(200);
        ss.setPrefWidth(140);
        panelQueryAllAppoint.getChildren().addAll(labA1, btnLoadAllAppoint, tableAllAppoint);
        // ==========管理员新增医生 ==========
        panelAddDoctor = new VBox(14);
        panelAddDoctor.setAlignment(Pos.TOP_LEFT);
        Label labA2 = new Label("【管理员】医生信息 — 新增医生");
        labA2.setFont(Font.font("System", FontWeight.BOLD, 19));
        labA2.setTextFill(Color.web("#194b75"));
        tfAddDocId = new TextField();
        tfAddDocId.setPromptText("医生ID");
        tfAddDocId.setStyle(fieldStyle());
        tfAddDocName = new TextField();
        tfAddDocName.setPromptText("姓名");
        tfAddDocName.setStyle(fieldStyle());
        tfAddDocDept = new TextField();
        tfAddDocDept.setPromptText("科室");
        tfAddDocDept.setStyle(fieldStyle());
        tfAddDocTitle = new TextField();
        tfAddDocTitle.setPromptText("职称");
        tfAddDocTitle.setStyle(fieldStyle());
        Button btnAddDoc = new Button("提交新增");
        btnAddDoc.setOnAction(e -> actionAddDoctor());
        panelAddDoctor.getChildren().addAll(labA2, tfAddDocId, tfAddDocName, tfAddDocDept, tfAddDocTitle, btnAddDoc);
        // ==========管理员修改医生 ==========
        panelUpdateDoctor = new VBox(14);
        panelUpdateDoctor.setAlignment(Pos.TOP_LEFT);
        Label labA3 = new Label("【管理员】医生信息 — 修改医生");
        labA3.setFont(Font.font("System", FontWeight.BOLD, 19));
        labA3.setTextFill(Color.web("#194b75"));
        tfUpdDocId = new TextField();
        tfUpdDocId.setPromptText("要修改的医生ID");
        tfUpdDocId.setStyle(fieldStyle());
        tfUpdDocName = new TextField();
        tfUpdDocName.setPromptText("新姓名");
        tfUpdDocName.setStyle(fieldStyle());
        tfUpdDocDept = new TextField();
        tfUpdDocDept.setPromptText("新科室");
        tfUpdDocDept.setStyle(fieldStyle());
        tfUpdDocTitle = new TextField();
        tfUpdDocTitle.setPromptText("新职称");
        tfUpdDocTitle.setStyle(fieldStyle());
        Button btnUpdDoc = new Button("提交修改");
        btnUpdDoc.setOnAction(e -> actionUpdateDoctor());
        panelUpdateDoctor.getChildren().addAll(labA3, tfUpdDocId, tfUpdDocName, tfUpdDocDept, tfUpdDocTitle, btnUpdDoc);
        // ==========管理员删除医生 ==========
        panelDeleteDoctor = new VBox(14);
        panelDeleteDoctor.setAlignment(Pos.TOP_LEFT);
        Label labA4 = new Label("【管理员】医生信息 — 删除医生");
        labA4.setFont(Font.font("System", FontWeight.BOLD, 19));
        labA4.setTextFill(Color.web("#194b75"));
        Label tipDel = new Label("点击按钮，弹窗选择需要删除的医生（仅展示完全没有任何预约记录的医生）");
        tipDel.setFont(Font.font("System", 14));
        tipDel.setTextFill(Color.web("#555555"));
        Button btnDelDoc = new Button("打开删除医生选择弹窗");
        btnDelDoc.setOnAction(e -> actionDeleteDoctor());
        panelDeleteDoctor.getChildren().addAll(labA4, tipDel, btnDelDoc);
    }

    private String fieldStyle() {
        return "-fx-background-radius:6;"
                + "-fx-border-radius:6;"
                + "-fx-border-color:#b8cddb;"
                + "-fx-border-width:1;"
                + "-fx-background-color: #ffffff;"
                + "-fx-padding: 0 12 0 12;"
                + "-fx-font-size:14px;";
    }

    private Optional<Appointment> showAppointSelectDialog() {
        Dialog<Appointment> dialog = new Dialog<>();
        dialog.setTitle("选择预约信息");
        dialog.setHeaderText("请选择医生与就诊时间");
        ComboBox<Doctor> cbDoctor = new ComboBox<>();
        cbDoctor.setPromptText("请选择医生");
        try {
            Message msg = _hospitalSrv.queryAllDoctor();
            if (IConstant.STATUS_SUCCESS.equals(msg.getStatusCode())) {
                List<Doctor> docList = (List<Doctor>) msg.getData();
                cbDoctor.getItems().addAll(docList);
                cbDoctor.setCellFactory(listView -> new ListCell<Doctor>() {
                    @Override
                    protected void updateItem(Doctor item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null)
                            setText(item.getDoctorId() + " | " + item.getName() + " (" + item.getDepartment() + ")");
                    }
                });
                cbDoctor.setButtonCell(new ListCell<Doctor>() {
                    @Override
                    protected void updateItem(Doctor item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) setText(item.getDoctorId() + " | " + item.getName());
                    }
                });
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "加载医生失败", e.getMessage());
            return Optional.empty();
        }
        DatePicker datePicker = new DatePicker();
        ComboBox<String> cbTime = new ComboBox<>();
        cbTime.getItems().addAll("08:30", "09:00", "09:30", "10:00", "10:30", "14:00", "14:30", "15:00", "15:30");
        cbTime.setPromptText("选择就诊时刻");
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.add(new Label("选择医生："), 0, 0);
        grid.add(cbDoctor, 1, 0);
        grid.add(new Label("选择就诊日期："), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("选择就诊时段："), 0, 2);
        grid.add(cbTime, 1, 2);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Doctor selectDoc = cbDoctor.getValue();
                LocalDate selectLocalDate = datePicker.getValue();
                String selectTime = cbTime.getValue();
                if (selectDoc == null || selectLocalDate == null || selectTime == null) {
                    showAlert(Alert.AlertType.WARNING, "校验失败", "医生、日期、时段必须全部选择！");
                    return null;
                }
                LocalDateTime localDateTime = LocalDateTime.of(selectLocalDate, LocalTime.parse(selectTime));
                Date appointDate = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
                Appointment appoint = new Appointment();
                appoint.setDoctorId(selectDoc.getDoctorId());
                appoint.setUserId(_loginUserId);
                appoint.setAppointmentTime(appointDate);
                return appoint;
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private Optional<String> showSelectMyAppointDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("选择要取消的预约记录");
        dialog.setHeaderText("仅显示【待就诊】状态的预约单");
        TableView<Appointment> table = new TableView<>();
        TableColumn<Appointment, String> colId = new TableColumn<>("预约编号");
        colId.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        TableColumn<Appointment, Date> colTime = new TableColumn<>("预约时间");
        colTime.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        TableColumn<Appointment, String> colDoc = new TableColumn<>("医生ID");
        colDoc.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        table.getColumns().addAll(colId, colTime, colDoc);
        table.setPrefSize(600, 320);
        try {
            Message resp = _hospitalSrv.queryMyAppointment(_loginUserId);
            if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                List<Appointment> allList = (List<Appointment>) resp.getData();
                List<Appointment> waitList = allList.stream()
                        .filter(a -> a.getStatus() == null || "待就诊".equals(a.getStatus()))
                        .collect(Collectors.toList());
                table.getItems().addAll(waitList);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "加载预约失败", e.getMessage());
            return Optional.empty();
        }
        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Appointment selected = table.getSelectionModel().getSelectedItem();
                if (selected == null) {
                    showAlert(Alert.AlertType.WARNING, "提示", "请在表格点击选中一条预约！");
                    return null;
                }
                return selected.getAppointmentId();
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private Optional<String> showSelectDeleteDoctorDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("选择待删除的医生");
        dialog.setHeaderText("列表仅展示：完全没有任何预约记录、允许删除的医生");
        TableView<Doctor> table = new TableView<>();
        TableColumn<Doctor, String> cId = new TableColumn<>("医生编号");
        cId.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        TableColumn<Doctor, String> cName = new TableColumn<>("姓名");
        cName.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Doctor, String> cDept = new TableColumn<>("科室");
        cDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        TableColumn<Doctor, String> cTitle = new TableColumn<>("职称");
        cTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        table.getColumns().addAll(cId, cName, cDept, cTitle);
        table.setPrefSize(650, 350);
        try {
            Message msg = _hospitalSrv.queryCanDeleteDoctor();
            if (IConstant.STATUS_SUCCESS.equals(msg.getStatusCode())) {
                List<Doctor> docList = (List<Doctor>) msg.getData();
                table.getItems().addAll(docList);
            } else {
                showAlert(Alert.AlertType.ERROR, "获取医生列表失败", String.valueOf(msg.getData()));
                return Optional.empty();
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "加载医生列表失败", e.getMessage());
            return Optional.empty();
        }
        dialog.getDialogPane().setContent(table);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Doctor selectedDoc = table.getSelectionModel().getSelectedItem();
                if (selectedDoc == null) {
                    showAlert(Alert.AlertType.WARNING, "提示", "请先选中表格中的一行医生！");
                    return null;
                }
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("删除确认");
                confirmAlert.setContentText("确定要删除医生【" + selectedDoc.getDoctorId() + " " + selectedDoc.getName() + "】吗？该操作不可恢复！");
                Optional<ButtonType> res = confirmAlert.showAndWait();
                if (res.isPresent() && res.get() == ButtonType.OK) {
                    return selectedDoc.getDoctorId();
                } else {
                    return null;
                }
            }
            return null;
        });
        return dialog.showAndWait();
    }

    private void actionQueryAllDoctor() {
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.queryAllDoctor();
                if (resp == null) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "错误", "服务器返回空消息"));
                    return;
                }
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        Object rawData = resp.getData();
                        if (rawData instanceof List<?>) {
                            List<Doctor> list = (List<Doctor>) rawData;
                            tableAllDoctor.getItems().clear();
                            tableAllDoctor.getItems().addAll(list);
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionQueryDoctorByDept() {
        String dept = tfDept.getText().trim();
        if (dept.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "提示", "请填写科室名称");
            return;
        }
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.queryDoctorByDept(dept);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        Object rawData = resp.getData();
                        if (rawData instanceof List<?>) {
                            List<Doctor> list = (List<Doctor>) rawData;
                            tableDeptDoctor.getItems().clear();
                            tableDeptDoctor.getItems().addAll(list);
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionAddAppointment() {
        Optional<Appointment> opt = showAppointSelectDialog();
        if (opt.isEmpty()) return;
        Appointment appoint = opt.get();
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.addAppointment(appoint);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "预约成功", String.valueOf(resp.getData()));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "预约失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionQueryMyAppointment() {
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.queryMyAppointment(_loginUserId);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        Object rawData = resp.getData();
                        if (rawData instanceof List<?>) {
                            List<Appointment> list = (List<Appointment>) rawData;
                            tableMyAppoint.getItems().clear();
                            tableMyAppoint.getItems().addAll(list);
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionCancelAppointment() {
        Optional<String> optAppointId = showSelectMyAppointDialog();
        if (optAppointId.isEmpty()) return;
        String cancelId = optAppointId.get();
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.cancelAppointment(_loginUserId, cancelId);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "取消成功", String.valueOf(resp.getData()));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "取消失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionQueryAllAppointment() {
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.queryAllAppointment(_loginUserId);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        Object rawData = resp.getData();
                        if (rawData instanceof List<?>) {
                            List<Appointment> list = (List<Appointment>) rawData;
                            tableAllAppoint.getItems().clear();
                            tableAllAppoint.getItems().addAll(list);
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionAddDoctor() {
        String id = tfAddDocId.getText().trim();
        String name = tfAddDocName.getText().trim();
        String dept = tfAddDocDept.getText().trim();
        String title = tfAddDocTitle.getText().trim();
        if (id.isBlank() || name.isBlank() || dept.isBlank() || title.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "提示", "全部字段不能为空");
            return;
        }
        Doctor doc = new Doctor();
        doc.setDoctorId(id);
        doc.setName(name);
        doc.setDepartment(dept);
        doc.setTitle(title);
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.addDoctor(_loginUserId, doc);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "成功", String.valueOf(resp.getData()));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionUpdateDoctor() {
        String id = tfUpdDocId.getText().trim();
        String name = tfUpdDocName.getText().trim();
        String dept = tfUpdDocDept.getText().trim();
        String title = tfUpdDocTitle.getText().trim();
        if (id.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "提示", "医生ID不能为空");
            return;
        }
        Doctor doc = new Doctor();
        doc.setDoctorId(id);
        doc.setName(name);
        doc.setDepartment(dept);
        doc.setTitle(title);
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.updateDoctor(_loginUserId, doc);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "成功", String.valueOf(resp.getData()));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void actionDeleteDoctor() {
        Optional<String> optDelDocId = showSelectDeleteDoctorDialog();
        if (optDelDocId.isEmpty()) {
            return;
        }
        String delDoctorId = optDelDocId.get();
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.deleteDoctor(_loginUserId, delDoctorId);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION, "成功", String.valueOf(resp.getData()));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "异常", e.getMessage()));
            }
        }).start();
    }

    private void showAlert(Alert.AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
