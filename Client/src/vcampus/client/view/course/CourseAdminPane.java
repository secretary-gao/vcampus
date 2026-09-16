package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;
import vcampus.common.vo.TeachingClass;

import java.util.Comparator;
import java.util.List;

/** 管理员课程与教学班主从工作区：左侧选择，右侧直接维护详情。 */
public class CourseAdminPane extends VBox {
    private final ICourseClientSrv client;
    private final Runnable changed;
    private final TableView<Course> courses = new TableView<>();
    private final TableView<TeachingClass> classes = new TableView<>();
    private final TextField courseSearch = text("搜索课程");
    private final TextField classSearch = text("搜索教学班");
    private final Label courseStatus = new Label();
    private final Label classStatus = new Label();
    private final Label courseDetailStatus = new Label("请从左侧选择课程，或点击“新增课程”");
    private final Label classDetailStatus = new Label("请从左侧选择教学班，或点击“新增教学班”");
    private final Button courseSave = button("确认新增课程", "primary", this::saveCourse);
    private final Button courseDelete = button("删除课程", "danger", this::deleteCourse);
    private final Button classSave = button("确认新增教学班", "primary", this::saveClass);
    private final Button classDelete = button("删除教学班", "danger", this::deleteClass);
    private List<Course> allCourses = List.of();
    private List<TeachingClass> allClasses = List.of();

    private final TextField courseId = text("课程号");
    private final TextField courseName = text("课程名称");
    private final TextField courseTeacher = text("默认教师");
    private final TextField courseCredit = text("学分");
    private final TextField courseCapacity = text("默认容量");
    private final TextField courseNature = text("课程性质");
    private final TextField courseUnit = text("开课单位");
    private Course selectedCourse;

    private final ComboBox<Course> classCourse = new ComboBox<>();
    private final TextField classId = text("教学班 ID");
    private final TextField classNo = text("班号");
    private final TextField classTeacher = text("授课教师");
    private final TextField classCapacity = text("容量");
    private final TextField classLanguage = text("授课语言");
    private final TextField classRemark = text("备注");
    private TeachingClass selectedClass;

    public CourseAdminPane(ICourseClientSrv client, Runnable changed) {
        this.client = client; this.changed = changed;
        getStyleClass().add("course-page"); build(); newCourse(); newClass(); refresh();
    }

    public void refresh() {
        courseStatus.setText("正在读取课程…"); classStatus.setText("正在读取教学班…");
        CourseViewSupport.runAsync(this, () -> new Snapshot(client.queryCourse("").stream()
                .sorted(Comparator.comparing(Course::getCourseId)).toList(), client.queryTeachingClass("").stream()
                .sorted(Comparator.comparing(TeachingClass::getTeachingClassId)).toList()), snap -> {
            allCourses = snap.courses; allClasses = snap.classes;
            classCourse.setItems(FXCollections.observableArrayList(allCourses));
            filterCourses(); filterClasses();
        });
    }

    private void build() {
        Label title = new Label("课程与教学班管理"); title.getStyleClass().add("page-title");
        Label hint = new Label("选中左侧记录后，可在右侧直接编辑、保存或删除"); hint.getStyleClass().add("page-description");
        TabPane tabs = new TabPane(); tabs.getStyleClass().add("admin-subtabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(new Tab("课程目录", courseWorkspace()), new Tab("教学班", classWorkspace()));
        VBox.setVgrow(tabs, Priority.ALWAYS); getChildren().addAll(title, hint, tabs);
    }

    private VBox courseWorkspace() {
        HBox.setHgrow(courseSearch, Priority.ALWAYS);
        HBox bar = toolbar(courseSearch, button("搜索", "secondary", this::filterCourses),
                button("刷新", "secondary", this::refresh), button("+ 新增课程", "primary", this::newCourse));
        courses.getColumns().addAll(CourseViewSupport.textColumn("课程号", 115, Course::getCourseId),
                CourseViewSupport.wrappingTextColumn("课程名称", 170, Course::getCourseName),
                CourseViewSupport.textColumn("默认教师", 90, Course::getTeacher),
                CourseViewSupport.textColumn("性质", 75, v -> safe(v.getCourseNature())),
                CourseViewSupport.textColumn("开课单位", 145, v -> safe(v.getOpeningUnit())),
                CourseViewSupport.textColumn("学分", 50, v -> String.valueOf(v.getCredit())),
                CourseViewSupport.textColumn("已选 / 容量", 92,
                        v -> v.getSelectedCount() + " / " + v.getCapacity()));
        CourseViewSupport.configureTable(courses, "暂无课程数据");
        courses.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> showCourse(value));
        VBox left = new VBox(8, courses, courseStatus); VBox.setVgrow(courses, Priority.ALWAYS);
        VBox right = detail("课程详情", courseDetailStatus, courseForm(),
                courseSave, courseDelete);
        return workspace(bar, left, right);
    }

    private VBox classWorkspace() {
        HBox.setHgrow(classSearch, Priority.ALWAYS);
        HBox bar = toolbar(classSearch, button("搜索", "secondary", this::filterClasses),
                button("刷新", "secondary", this::refresh), button("+ 新增教学班", "primary", this::newClass));
        classes.getColumns().addAll(CourseViewSupport.textColumn("教学班 ID", 145, TeachingClass::getTeachingClassId),
                CourseViewSupport.wrappingTextColumn("课程", 205, this::courseLabel),
                CourseViewSupport.textColumn("班号", 55, TeachingClass::getClassNumber),
                CourseViewSupport.textColumn("教师", 100, TeachingClass::getTeacher),
                CourseViewSupport.textColumn("授课语言", 80, v -> safe(v.getTeachingLanguage())),
                CourseViewSupport.textColumn("已选 / 容量", 90, v -> v.getSelectedCount()+" / "+v.getCapacity()));
        CourseViewSupport.configureTable(classes, "暂无教学班数据");
        classes.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> showClass(value));
        VBox left = new VBox(8, classes, classStatus); VBox.setVgrow(classes, Priority.ALWAYS);
        classCourse.setMaxWidth(Double.MAX_VALUE); classCourse.setConverter(new StringConverter<>() {
            public String toString(Course v) { return v == null ? "" : v.getCourseId()+" · "+v.getCourseName(); }
            public Course fromString(String s) { return null; }
        });
        VBox right = detail("教学班详情", classDetailStatus, classForm(),
                classSave, classDelete);
        return workspace(bar, left, right);
    }

    private VBox workspace(HBox toolbar, VBox left, VBox right) {
        SplitPane split = new SplitPane(left, right); split.setOrientation(Orientation.HORIZONTAL); split.setDividerPositions(.66);
        VBox.setVgrow(split, Priority.ALWAYS); VBox root = new VBox(10, toolbar, split); VBox.setVgrow(root, Priority.ALWAYS); return root;
    }
    private HBox toolbar(javafx.scene.Node... nodes) { HBox h = new HBox(9, nodes); h.setPadding(new Insets(0,0,4,0)); h.getStyleClass().add("tool-bar-card"); return h; }
    private VBox detail(String title, Label status, GridPane form, Button... buttons) {
        Label label = new Label(title); label.getStyleClass().add("section-title"); HBox actions = new HBox(8, buttons);
        status.getStyleClass().add("status-label"); status.setWrapText(true);
        VBox box = new VBox(12, label, actions, status, form); box.setPadding(new Insets(12)); box.getStyleClass().add("course-card"); return box;
    }
    private GridPane courseForm() { GridPane g = form(); add(g,0,"课程号",courseId); add(g,1,"课程名称",courseName); add(g,2,"默认教师",courseTeacher); add(g,3,"学分",courseCredit); add(g,4,"默认容量",courseCapacity); add(g,5,"课程性质",courseNature); add(g,6,"开课单位",courseUnit); return g; }
    private GridPane classForm() { GridPane g=form(); add(g,0,"所属课程",classCourse); add(g,1,"教学班 ID",classId); add(g,2,"教学班号",classNo); add(g,3,"授课教师",classTeacher); add(g,4,"容量",classCapacity); add(g,5,"授课语言",classLanguage); add(g,6,"备注",classRemark); return g; }
    private GridPane form() { GridPane g=new GridPane(); g.setHgap(10); g.setVgap(10); g.getStyleClass().add("form-grid"); return g; }
    private void add(GridPane g,int row,String name,javafx.scene.Node value){ Label l=new Label(name);l.setMinWidth(76);l.getStyleClass().add("field-label");g.add(l,0,row);g.add(value,1,row);GridPane.setHgrow(value,Priority.ALWAYS); }

    private void filterCourses() { String k=normal(courseSearch.getText()); List<Course> rows=allCourses.stream().filter(v->k.isBlank()||has(v.getCourseId(),k)||has(v.getCourseName(),k)||has(v.getCourseNature(),k)||has(v.getOpeningUnit(),k)).toList(); courses.setItems(FXCollections.observableArrayList(rows));courseStatus.setText("显示 "+rows.size()+" 门，共 "+allCourses.size()+" 门课程"); }
    private void filterClasses() { String k=normal(classSearch.getText()); List<TeachingClass> rows=allClasses.stream().filter(v->k.isBlank()||has(v.getTeachingClassId(),k)||has(v.getCourseId(),k)||has(v.getClassNumber(),k)||has(v.getTeacher(),k)).toList();classes.setItems(FXCollections.observableArrayList(rows));classStatus.setText("显示 "+rows.size()+" 个，共 "+allClasses.size()+" 个教学班"); }
    private void showCourse(Course v) { selectedCourse=v; if(v==null)return; courseId.setText(v.getCourseId());courseId.setDisable(true);courseName.setText(v.getCourseName());courseTeacher.setText(v.getTeacher());courseCredit.setText(String.valueOf(v.getCredit()));courseCapacity.setText(String.valueOf(v.getCapacity()));courseNature.setText(safe(v.getCourseNature()));courseUnit.setText(safe(v.getOpeningUnit()));courseSave.setText("保存课程修改");courseDelete.setDisable(false);courseDetailStatus.setText("正在编辑："+v.getCourseId()+" · "+v.getCourseName()); }
    private void newCourse() { selectedCourse=null;courses.getSelectionModel().clearSelection();courseId.setDisable(false); clear(courseId,courseName,courseTeacher,courseCredit,courseCapacity,courseNature,courseUnit);courseCredit.setText("2");courseCapacity.setText("50");courseSave.setText("确认新增课程");courseDelete.setDisable(true);courseDetailStatus.setText("新增模式：填写完整信息后点击“确认新增课程”"); }
    private void saveCourse() { try { boolean editing=selectedCourse!=null;Course v=new Course(required(courseId,"课程号"),required(courseName,"课程名称"),required(courseTeacher,"默认教师"),positive(courseCredit,"学分"),positive(courseCapacity,"默认容量"),editing?selectedCourse.getSelectedCount():0);v.setCourseNature(trim(courseNature));v.setOpeningUnit(trim(courseUnit));CourseViewSupport.runAsync(this,()->{if(!editing)return client.addCourse(v);client.updateCourse(v);return v;},x->{courseDetailStatus.setText(editing?"课程修改成功："+v.getCourseId():"课程新增成功："+v.getCourseId());changed.run();refresh();}); } catch(RuntimeException e){CourseViewSupport.showError(e);} }
    private void deleteCourse() { if(selectedCourse==null){CourseViewSupport.showError(new IllegalArgumentException("请先选择课程"));return;} String deletedId=selectedCourse.getCourseId();if(!CourseViewSupport.confirm("删除课程？",selectedCourse.getCourseId()+" · "+selectedCourse.getCourseName()))return;CourseViewSupport.runAsync(this,()->client.deleteCourse(deletedId),x->{newCourse();courseDetailStatus.setText("课程删除成功："+deletedId);changed.run();refresh();}); }
    private void showClass(TeachingClass v) { selectedClass=v;if(v==null)return;classCourse.getItems().stream().filter(c->c.getCourseId().equals(v.getCourseId())).findFirst().ifPresent(classCourse::setValue);classId.setText(v.getTeachingClassId());classId.setDisable(true);classNo.setText(v.getClassNumber());classTeacher.setText(v.getTeacher());classCapacity.setText(String.valueOf(v.getCapacity()));classLanguage.setText(safe(v.getTeachingLanguage()));classRemark.setText(safe(v.getRemark()));classSave.setText("保存教学班修改");classDelete.setDisable(false);classDetailStatus.setText("正在编辑："+v.getTeachingClassId()); }
    private void newClass() { selectedClass=null;classes.getSelectionModel().clearSelection();classId.setDisable(false);classCourse.setValue(null);clear(classId,classNo,classTeacher,classCapacity,classLanguage,classRemark);classCapacity.setText("50");classSave.setText("确认新增教学班");classDelete.setDisable(true);classDetailStatus.setText("新增模式：填写完整信息后点击“确认新增教学班”"); }
    private void saveClass() { try { boolean editing=selectedClass!=null;if(classCourse.getValue()==null)throw new IllegalArgumentException("请选择所属课程");TeachingClass v=new TeachingClass(required(classId,"教学班 ID"),classCourse.getValue().getCourseId(),required(classNo,"教学班号"),required(classTeacher,"授课教师"),nonnegative(classCapacity,"容量"),editing?selectedClass.getSelectedCount():0,trim(classLanguage),trim(classRemark));CourseViewSupport.runAsync(this,()->{if(!editing)return client.addTeachingClass(v);client.updateTeachingClass(v);return v;},x->{classDetailStatus.setText(editing?"教学班修改成功："+v.getTeachingClassId():"教学班新增成功："+v.getTeachingClassId());changed.run();refresh();}); }catch(RuntimeException e){CourseViewSupport.showError(e);} }
    private void deleteClass() { if(selectedClass==null){CourseViewSupport.showError(new IllegalArgumentException("请先选择教学班"));return;}String deletedId=selectedClass.getTeachingClassId();if(!CourseViewSupport.confirm("删除教学班？",deletedId))return;CourseViewSupport.runAsync(this,()->client.deleteTeachingClass(deletedId),x->{newClass();classDetailStatus.setText("教学班删除成功："+deletedId);changed.run();refresh();}); }
    private String courseLabel(TeachingClass value) { return allCourses.stream().filter(course -> course.getCourseId().equals(value.getCourseId())).findFirst().map(course -> course.getCourseId()+" · "+course.getCourseName()).orElse(value.getCourseId()); }
    private static TextField text(String prompt){TextField f=new TextField();f.setPromptText(prompt);f.setMaxWidth(Double.MAX_VALUE);return f;} private static Button button(String t,String css,Runnable run){Button b=new Button(t);b.getStyleClass().add(css);b.setOnAction(e->run.run());return b;} private static void clear(TextField...fs){for(TextField f:fs)f.clear();} private static String normal(String s){return s==null?"":s.trim().toLowerCase();}private static boolean has(String s,String q){return s!=null&&s.toLowerCase().contains(q);}private static String safe(String s){return s==null?"":s;}private static String trim(TextField f){return f.getText()==null?"":f.getText().trim();}private static String required(TextField f,String n){String v=trim(f);if(v.isBlank())throw new IllegalArgumentException(n+"不能为空");return v;}private static int positive(TextField f,String n){int v=number(f,n);if(v<=0)throw new IllegalArgumentException(n+"必须大于 0");return v;}private static int nonnegative(TextField f,String n){int v=number(f,n);if(v<0)throw new IllegalArgumentException(n+"不能为负数");return v;}private static int number(TextField f,String n){try{return Integer.parseInt(required(f,n));}catch(NumberFormatException e){throw new IllegalArgumentException(n+"必须是整数");}}
    private record Snapshot(List<Course> courses,List<TeachingClass> classes){}
}
