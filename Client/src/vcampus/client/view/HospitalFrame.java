/*
 * HospitalFrame
 *
 * Version 1.4 医院主题UI改版，业务逻辑不变
 *
 * 2026-09-03
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;
import vcampus.client.biz.HospitalClientSrv;
import vcampus.common.constant.IConstant;
import vcampus.common.vo.Appointment;
import vcampus.common.vo.Doctor;
import vcampus.common.vo.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class HospitalFrame extends Application {
    private Stage _stage;
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
    private TextField tfDoctorId;
    private TextField tfAppointTime;

    private VBox panelMyAppoint;
    private TableView<Appointment> tableMyAppoint;

    private VBox panelCancelAppoint;
    private TextField tfCancelAppointId;

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
    private TextField tfDelDocId;

    public HospitalFrame(String loginUserId, String loginUserRole) {
        this._loginUserId = loginUserId;
        this._loginUserRole = loginUserRole;
    }

    @Override
    public void start(Stage stage) {
        this._stage = stage;
        buildUi(stage);
        stage.setTitle("🏥 医院挂号管理系统 - " + _loginUserId);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.setOnCloseRequest(e -> {});
        stage.show();
    }

    private void buildUi(Stage stage) {
        BorderPane root = new BorderPane();
        //【医院主题背景：淡浅蓝，医疗系统柔和渐变】
        root.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0,0,1,1,true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#f4f8fb")),
                        new Stop(1, Color.web("#e8f0f7"))),
                CornerRadii.EMPTY, Insets.EMPTY)));
        root.setLeft(buildSideBar());
        _contentPane = new StackPane();
        _contentPane.setPadding(new Insets(20));
        root.setCenter(_contentPane);
        initAllPanels();
        switchPanel(panelQueryAllDoctor);
        Scene scene = new Scene(root, 1280, 780);
        stage.setScene(scene);
    }

    private VBox buildSideBar() {
        VBox side = new VBox(12);
        side.setPrefWidth(240);
        side.setPadding(new Insets(28,14,14,14));
        //侧边栏：纯白，淡淡的医疗蓝色边框
        side.setStyle("-fx-background-color: #ffffff;"
                + "-fx-border-color:#cce0ed;"
                + "-fx-border-width:0 1 0 0;"
                + "-fx-effect: dropshadow(gaussian, rgba(80,130,170,0.08),10,0.1,1,0);");
        Label title = new Label("🏥 医院挂号管理系统");
        title.setFont(Font.font("System", FontWeight.BOLD,19));
        title.setTextFill(Color.web("#194b75"));

        Label userInfo = new Label("当前账号："+_loginUserId+"｜"+_loginUserRole);
        userInfo.setFont(Font.font("System",13));
        userInfo.setTextFill(Color.web("#446078"));

        Separator sep1 = new Separator();
        sep1.setPadding(new Insets(10,0,10,0));

        Label lblUserFunc = new Label("▷ 用户操作");
        lblUserFunc.setFont(Font.font("System",FontWeight.BOLD,14));
        lblUserFunc.setTextFill(Color.web("#235782"));

        btnQueryAllDoctor = new Button("查询全部医生");
        btnQueryByDept = new Button("按科室查询医生");
        btnAddAppoint = new Button("预约挂号");
        btnMyAppoint = new Button("我的预约记录");
        btnCancelAppoint = new Button("取消预约");

        Separator sep2 = new Separator();
        sep2.setPadding(new Insets(10,0,10,0));

        Label lblAdminFunc = new Label("▷ 管理员操作");
        lblAdminFunc.setFont(Font.font("System",FontWeight.BOLD,14));
        lblAdminFunc.setTextFill(Color.web("#235782"));

        btnQueryAllAppoint = new Button("查询全部预约");
        btnAddDoctor = new Button("新增医生");
        btnUpdateDoctor = new Button("修改医生");
        btnDeleteDoctor = new Button("删除医生");

        Button[] btns = {
                btnQueryAllDoctor,btnQueryByDept,btnAddAppoint,btnMyAppoint,btnCancelAppoint,
                btnQueryAllAppoint,btnAddDoctor,btnUpdateDoctor,btnDeleteDoctor
        };
        //=====医院风格按钮：柔和医疗蓝，不刺眼=====
        for(Button b : btns){
            b.setMaxWidth(Double.MAX_VALUE);
            b.setPrefHeight(38);
            b.setStyle("-fx-background-color:#367ba9;-fx-text-fill:white;-fx-font-size:13px;-fx-background-radius:6;-fx-cursor:hand;");
            //鼠标悬浮变色
            b.setOnMouseEntered(e->b.setStyle("-fx-background-color:#2b648c;-fx-text-fill:white;-fx-font-size:13px;-fx-background-radius:6;-fx-cursor:hand;"));
            b.setOnMouseExited(e->b.setStyle("-fx-background-color:#367ba9;-fx-text-fill:white;-fx-font-size:13px;-fx-background-radius:6;-fx-cursor:hand;"));
        }

        if(!"管理员".equals(_loginUserRole)){
            btnQueryAllAppoint.setVisible(false);
            btnAddDoctor.setVisible(false);
            btnUpdateDoctor.setVisible(false);
            btnDeleteDoctor.setVisible(false);
            lblAdminFunc.setVisible(false);
            sep2.setVisible(false);
        }

        btnQueryAllDoctor.setOnAction(e->switchPanel(panelQueryAllDoctor));
        btnQueryByDept.setOnAction(e->switchPanel(panelQueryDept));
        btnAddAppoint.setOnAction(e->switchPanel(panelAddAppoint));
        btnMyAppoint.setOnAction(e->switchPanel(panelMyAppoint));
        btnCancelAppoint.setOnAction(e->switchPanel(panelCancelAppoint));
        btnQueryAllAppoint.setOnAction(e->switchPanel(panelQueryAllAppoint));
        btnAddDoctor.setOnAction(e->switchPanel(panelAddDoctor));
        btnUpdateDoctor.setOnAction(e->switchPanel(panelUpdateDoctor));
        btnDeleteDoctor.setOnAction(e->switchPanel(panelDeleteDoctor));

        side.getChildren().addAll(title,userInfo,sep1,
                lblUserFunc,
                btnQueryAllDoctor,btnQueryByDept,btnAddAppoint,btnMyAppoint,btnCancelAppoint,
                sep2,lblAdminFunc,
                btnQueryAllAppoint,btnAddDoctor,btnUpdateDoctor,btnDeleteDoctor);
        return side;
    }

    private void switchPanel(VBox panel){
        _contentPane.getChildren().clear();
        _contentPane.getChildren().add(wrapCard(panel));
    }

    private VBox wrapCard(VBox inner){
        VBox card = new VBox();
        card.setMaxWidth(860);
        card.setPrefWidth(860);
        card.setPadding(new Insets(24,22,22,22));
        //医院业务卡片：纯白，淡蓝色细边框，柔和阴影
        card.setStyle("-fx-background-color: #ffffff;"
                + "-fx-background-radius:12;"
                + "-fx-border-radius:12;"
                + "-fx-border-color:#d0e1ec;"
                + "-fx-border-width:1;"
                + "-fx-effect: dropshadow(gaussian, rgba(60,110,150,0.09),14,0.1,0,4);");
        card.getChildren().add(inner);
        return card;
    }

    private void initAllPanels(){
        // ========== 1. 查询全部医生 ==========
        panelQueryAllDoctor = new VBox(14);
        panelQueryAllDoctor.setAlignment(Pos.TOP_LEFT);
        Label lab1 = new Label("医生信息管理 — 查询全部医生");
        lab1.setFont(Font.font("System",FontWeight.BOLD,19));
        lab1.setTextFill(Color.web("#194b75"));
        Button btnLoadAllDoc = new Button("加载全部医生");
        btnLoadAllDoc.setOnAction(e->actionQueryAllDoctor());
        tableAllDoctor = new TableView<>();
        tableAllDoctor.setPrefHeight(420);
        TableColumn<Doctor,String> colDocId = new TableColumn<>("医生编号");
        colDocId.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        TableColumn<Doctor,String> colName = new TableColumn<>("姓名");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Doctor,String> colDept = new TableColumn<>("科室");
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        TableColumn<Doctor,String> colTitle = new TableColumn<>("职称");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        tableAllDoctor.getColumns().addAll(colDocId,colName,colDept,colTitle);
        colDocId.setPrefWidth(140);
        colName.setPrefWidth(140);
        colDept.setPrefWidth(220);
        colTitle.setPrefWidth(220);
        panelQueryAllDoctor.getChildren().addAll(lab1,btnLoadAllDoc,tableAllDoctor);

        // ==========2.按科室查询医生 ==========
        panelQueryDept = new VBox(14);
        panelQueryDept.setAlignment(Pos.TOP_LEFT);
        Label lab2 = new Label("医生信息管理 — 按科室查询");
        lab2.setFont(Font.font("System",FontWeight.BOLD,19));
        lab2.setTextFill(Color.web("#194b75"));
        tfDept = new TextField();
        tfDept.setPromptText("输入科室名称，例如：内科");
        tfDept.setStyle(fieldStyle());
        Button btnQDept = new Button("查询");
        btnQDept.setOnAction(e->actionQueryDoctorByDept());
        tableDeptDoctor = new TableView<>();
        tableDeptDoctor.setPrefHeight(380);
        TableColumn<Doctor,String> c1 = new TableColumn<>("医生编号");
        c1.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        TableColumn<Doctor,String> c2 = new TableColumn<>("姓名");
        c2.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<Doctor,String> c3 = new TableColumn<>("科室");
        c3.setCellValueFactory(new PropertyValueFactory<>("department"));
        TableColumn<Doctor,String> c4 = new TableColumn<>("职称");
        c4.setCellValueFactory(new PropertyValueFactory<>("title"));
        tableDeptDoctor.getColumns().addAll(c1,c2,c3,c4);
        c1.setPrefWidth(140);c2.setPrefWidth(140);c3.setPrefWidth(220);c4.setPrefWidth(220);
        panelQueryDept.getChildren().addAll(lab2,tfDept,btnQDept,tableDeptDoctor);

        // ==========3.预约挂号 ==========
        panelAddAppoint = new VBox(14);
        panelAddAppoint.setAlignment(Pos.TOP_LEFT);
        Label lab3 = new Label("预约挂号 — 新建就诊预约");
        lab3.setFont(Font.font("System",FontWeight.BOLD,19));
        lab3.setTextFill(Color.web("#194b75"));
        tfDoctorId = new TextField();
        tfDoctorId.setPromptText("医生ID");
        tfDoctorId.setStyle(fieldStyle());
        tfAppointTime = new TextField();
        tfAppointTime.setPromptText("预约时间，例如：2026‑09‑10 09:30");
        tfAppointTime.setStyle(fieldStyle());
        Button btnMakeAppoint = new Button("提交预约");
        btnMakeAppoint.setOnAction(e->actionAddAppointment());
        panelAddAppoint.getChildren().addAll(lab3,tfDoctorId,tfAppointTime,btnMakeAppoint);

        // ==========4.我的预约记录【状态列自定义渲染】 ==========
        panelMyAppoint = new VBox(14);
        panelMyAppoint.setAlignment(Pos.TOP_LEFT);
        Label lab4 = new Label("预约记录 — 我的就诊预约");
        lab4.setFont(Font.font("System",FontWeight.BOLD,19));
        lab4.setTextFill(Color.web("#194b75"));
        Button btnRefreshMy = new Button("刷新我的预约");
        btnRefreshMy.setOnAction(e->actionQueryMyAppointment());
        tableMyAppoint = new TableView<>();
        tableMyAppoint.setPrefHeight(420);
        TableColumn<Appointment,String> aid = new TableColumn<>("预约编号");
        aid.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        TableColumn<Appointment,String> uid = new TableColumn<>("用户ID");
        uid.setCellValueFactory(new PropertyValueFactory<>("userId"));
        TableColumn<Appointment,String> did = new TableColumn<>("医生ID");
        did.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        TableColumn<Appointment,Date> atime = new TableColumn<>("预约时间");
        atime.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        TableColumn<Appointment,String> stat = new TableColumn<>("状态");
        stat.setCellValueFactory(new PropertyValueFactory<>("status"));
        //自定义单元格，null显示待就诊
        stat.setCellFactory(col -> new TableCell<Appointment,String>(){
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if(empty){
                    setText(null);
                }else if(value == null || value.isBlank()){
                    setText("待就诊");
                }else{
                    setText(value);
                }
            }
        });
        tableMyAppoint.getColumns().addAll(aid,uid,did,atime,stat);
        aid.setPrefWidth(140);uid.setPrefWidth(110);did.setPrefWidth(110);atime.setPrefWidth(200);stat.setPrefWidth(140);
        panelMyAppoint.getChildren().addAll(lab4,btnRefreshMy,tableMyAppoint);

        // ==========5.取消预约 ==========
        panelCancelAppoint = new VBox(14);
        panelCancelAppoint.setAlignment(Pos.TOP_LEFT);
        Label lab5 = new Label("预约操作 — 取消就诊预约");
        lab5.setFont(Font.font("System",FontWeight.BOLD,19));
        lab5.setTextFill(Color.web("#194b75"));
        tfCancelAppointId = new TextField();
        tfCancelAppointId.setPromptText("输入预约ID");
        tfCancelAppointId.setStyle(fieldStyle());
        Button btnCancel = new Button("确认取消");
        btnCancel.setOnAction(e->actionCancelAppointment());
        panelCancelAppoint.getChildren().addAll(lab5,tfCancelAppointId,btnCancel);

        // ==========管理员：全部预约表格【状态列自定义渲染】 ==========
        panelQueryAllAppoint = new VBox(14);
        panelQueryAllAppoint.setAlignment(Pos.TOP_LEFT);
        Label labA1 = new Label("【管理员】全部就诊预约记录");
        labA1.setFont(Font.font("System",FontWeight.BOLD,19));
        labA1.setTextFill(Color.web("#194b75"));
        Button btnLoadAllAppoint = new Button("刷新全部预约");
        btnLoadAllAppoint.setOnAction(e->actionQueryAllAppointment());
        tableAllAppoint = new TableView<>();
        tableAllAppoint.setPrefHeight(440);
        TableColumn<Appointment,String> aa = new TableColumn<>("预约编号");
        aa.setCellValueFactory(new PropertyValueFactory<>("appointmentId"));
        TableColumn<Appointment,String> uu = new TableColumn<>("用户ID");
        uu.setCellValueFactory(new PropertyValueFactory<>("userId"));
        TableColumn<Appointment,String> dd = new TableColumn<>("医生ID");
        dd.setCellValueFactory(new PropertyValueFactory<>("doctorId"));
        TableColumn<Appointment,Date> tt = new TableColumn<>("预约时间");
        tt.setCellValueFactory(new PropertyValueFactory<>("appointmentTime"));
        TableColumn<Appointment,String> ss = new TableColumn<>("状态");
        ss.setCellValueFactory(new PropertyValueFactory<>("status"));
        ss.setCellFactory(col -> new TableCell<Appointment,String>(){
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if(empty){
                    setText(null);
                }else if(value == null || value.isBlank()){
                    setText("待就诊");
                }else{
                    setText(value);
                }
            }
        });
        tableAllAppoint.getColumns().addAll(aa,uu,dd,tt,ss);
        aa.setPrefWidth(140);uu.setPrefWidth(110);dd.setPrefWidth(110);tt.setPrefWidth(200);ss.setPrefWidth(140);
        panelQueryAllAppoint.getChildren().addAll(labA1,btnLoadAllAppoint,tableAllAppoint);

        // ==========管理员新增医生 ==========
        panelAddDoctor = new VBox(14);
        panelAddDoctor.setAlignment(Pos.TOP_LEFT);
        Label labA2 = new Label("【管理员】医生信息 — 新增医生");
        labA2.setFont(Font.font("System",FontWeight.BOLD,19));
        labA2.setTextFill(Color.web("#194b75"));
        tfAddDocId = new TextField(); tfAddDocId.setPromptText("医生ID"); tfAddDocId.setStyle(fieldStyle());
        tfAddDocName = new TextField(); tfAddDocName.setPromptText("姓名"); tfAddDocName.setStyle(fieldStyle());
        tfAddDocDept = new TextField(); tfAddDocDept.setPromptText("科室"); tfAddDocDept.setStyle(fieldStyle());
        tfAddDocTitle = new TextField(); tfAddDocTitle.setPromptText("职称"); tfAddDocTitle.setStyle(fieldStyle());
        Button btnAddDoc = new Button("提交新增");
        btnAddDoc.setOnAction(e->actionAddDoctor());
        panelAddDoctor.getChildren().addAll(labA2,tfAddDocId,tfAddDocName,tfAddDocDept,tfAddDocTitle,btnAddDoc);

        // ==========管理员修改医生 ==========
        panelUpdateDoctor = new VBox(14);
        panelUpdateDoctor.setAlignment(Pos.TOP_LEFT);
        Label labA3 = new Label("【管理员】医生信息 — 修改医生");
        labA3.setFont(Font.font("System",FontWeight.BOLD,19));
        labA3.setTextFill(Color.web("#194b75"));
        tfUpdDocId = new TextField(); tfUpdDocId.setPromptText("要修改的医生ID"); tfUpdDocId.setStyle(fieldStyle());
        tfUpdDocName = new TextField(); tfUpdDocName.setPromptText("新姓名"); tfUpdDocName.setStyle(fieldStyle());
        tfUpdDocDept = new TextField(); tfUpdDocDept.setPromptText("新科室"); tfUpdDocDept.setStyle(fieldStyle());
        tfUpdDocTitle = new TextField(); tfUpdDocTitle.setPromptText("新职称"); tfUpdDocTitle.setStyle(fieldStyle());
        Button btnUpdDoc = new Button("提交修改");
        btnUpdDoc.setOnAction(e->actionUpdateDoctor());
        panelUpdateDoctor.getChildren().addAll(labA3,tfUpdDocId,tfUpdDocName,tfUpdDocDept,tfUpdDocTitle,btnUpdDoc);

        // ==========管理员删除医生 ==========
        panelDeleteDoctor = new VBox(14);
        panelDeleteDoctor.setAlignment(Pos.TOP_LEFT);
        Label labA4 = new Label("【管理员】医生信息 — 删除医生");
        labA4.setFont(Font.font("System",FontWeight.BOLD,19));
        labA4.setTextFill(Color.web("#194b75"));
        tfDelDocId = new TextField(); tfDelDocId.setPromptText("待删除医生ID"); tfDelDocId.setStyle(fieldStyle());
        Button btnDelDoc = new Button("确认删除");
        btnDelDoc.setOnAction(e->actionDeleteDoctor());
        panelDeleteDoctor.getChildren().addAll(labA4,tfDelDocId,btnDelDoc);
    }

    private String fieldStyle() {
        //医院系统输入框：简约淡边框
        return "-fx-background-radius:6;"
                + "-fx-border-radius:6;"
                + "-fx-border-color:#b8cddb;"
                + "-fx-border-width:1;"
                + "-fx-background-color: #ffffff;"
                + "-fx-padding: 0 12 0 12;"
                + "-fx-font-size:14px;";
    }

    // ======================业务事件【完全原样，没有改动】======================
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
                        if(rawData instanceof List<?> rawList){
                            List<Doctor> list = (List<Doctor>) rawList;
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
        if(dept.isBlank()){
            showAlert(Alert.AlertType.WARNING,"提示","请填写科室名称");
            return;
        }
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.queryDoctorByDept(dept);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        Object rawData = resp.getData();
                        if(rawData instanceof List<?> rawList){
                            List<Doctor> list = (List<Doctor>) rawList;
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
        String docId = tfDoctorId.getText().trim();
        String appointTimeStr = tfAppointTime.getText().trim();
        if(docId.isBlank() || appointTimeStr.isBlank()){
            showAlert(Alert.AlertType.WARNING,"提示","医生ID、预约时间不能为空");
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date appointDate;
        try {
            appointDate = sdf.parse(appointTimeStr);
        } catch (ParseException e) {
            showAlert(Alert.AlertType.ERROR,"格式错误","时间格式请填写：yyyy‑MM‑dd HH:mm\n例：2026‑09‑05 14:30");
            return;
        }
        Appointment appoint = new Appointment();
        appoint.setUserId(_loginUserId);
        appoint.setDoctorId(docId);
        appoint.setAppointmentTime(appointDate);
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.addAppointment(appoint);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION,"成功", String.valueOf(resp.getData()));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
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
                        if(rawData instanceof List<?> rawList){
                            List<Appointment> list = (List<Appointment>) rawList;
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
        String aid = tfCancelAppointId.getText().trim();
        if(aid.isBlank()){
            showAlert(Alert.AlertType.WARNING,"提示","请输入预约ID");
            return;
        }
        new Thread(() -> {
            try {
                //传入登录用户ID
                Message resp = _hospitalSrv.cancelAppointment(_loginUserId, aid);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION,"成功",String.valueOf(resp.getData()));
                    } else {
                        showAlert(Alert.AlertType.ERROR, "业务失败", String.valueOf(resp.getData()));
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
                        if(rawData instanceof List<?> rawList){
                            List<Appointment> list = (List<Appointment>) rawList;
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
        if(id.isBlank()||name.isBlank()||dept.isBlank()||title.isBlank()){
            showAlert(Alert.AlertType.WARNING,"提示","全部字段不能为空");
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
                        showAlert(Alert.AlertType.INFORMATION,"成功",String.valueOf(resp.getData()));
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
        if(id.isBlank()){
            showAlert(Alert.AlertType.WARNING,"提示","医生ID不能为空");
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
                        showAlert(Alert.AlertType.INFORMATION,"成功",String.valueOf(resp.getData()));
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
        String did = tfDelDocId.getText().trim();
        if(did.isBlank()){
            showAlert(Alert.AlertType.WARNING,"提示","请输入医生ID");
            return;
        }
        new Thread(() -> {
            try {
                Message resp = _hospitalSrv.deleteDoctor(_loginUserId, did);
                Platform.runLater(() -> {
                    if (IConstant.STATUS_SUCCESS.equals(resp.getStatusCode())) {
                        showAlert(Alert.AlertType.INFORMATION,"成功",String.valueOf(resp.getData()));
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

    public static void main(String[] args) {
        Application.launch(TestLauncher.class);
    }
    public static class TestLauncher extends Application{
        @Override
        public void start(Stage primaryStage) throws Exception {
            //切换账号在这里改："09010210","学生"  / "admin001","管理员"
            new HospitalFrame("admin001","管理员").start(primaryStage);
        }
    }
}
