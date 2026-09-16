package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.TeachingClass;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 排课主从工作区；右侧表单的提交始终经过服务端冲突校验。 */
public class ScheduleAdminPane extends VBox {
    private final ICourseClientSrv client;
    private final TableView<Row> table = new TableView<>();
    private final Label status = new Label();
    private List<Choice> choices = List.of();
    private final ComboBox<Choice> teachingClass = new ComboBox<>();
    private final ComboBox<Integer> day = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5,6,7));
    private final TextField room = field("例如：教三-303");
    private final TextField weekStart = field("1");
    private final TextField weekEnd = field("16");
    private final TextField periodStart = field("1");
    private final TextField periodEnd = field("2");
    private final TextField startTime = field("08:00");
    private final TextField endTime = field("09:35");
    private Row selected;

    public ScheduleAdminPane(ICourseClientSrv client) { this.client=client;getStyleClass().add("course-page");build();refresh(); }
    public void refresh() {
        status.setText("正在读取排课…");
        CourseViewSupport.runAsync(this, () -> {
            Map<String,Course> courses=client.queryCourse("").stream().collect(Collectors.toMap(Course::getCourseId,Function.identity(),(a,b)->a));
            Map<String,TeachingClass> classes=client.queryTeachingClass("").stream().collect(Collectors.toMap(TeachingClass::getTeachingClassId,Function.identity(),(a,b)->a));
            List<Choice> values=classes.values().stream().map(c->new Choice(c,courses.get(c.getCourseId()))).toList();
            return new Snapshot(values,client.querySchedule().stream().map(s->new Row(s,courses.get(s.getCourseId()),classes.get(s.getTeachingClassId()))).toList());
        }, snapshot->{choices=snapshot.choices;teachingClass.setItems(FXCollections.observableArrayList(choices));table.setItems(FXCollections.observableArrayList(snapshot.rows));status.setText("共 "+snapshot.rows.size()+" 条排课");});
    }
    private void build() {
        Label title=new Label("排课管理");title.getStyleClass().add("page-title");
        Label hint=new Label("选中左侧排课后，在右侧直接编辑；保存和删除会再次执行服务端冲突校验");hint.getStyleClass().add("page-description");
        Button refresh=button("刷新","secondary",this::refresh), add=button("+ 新增排课","primary",this::fresh);
        HBox header=new HBox(9,title,refresh,add);HboxGrow(title);header.getStyleClass().add("tool-bar-card");
        table.getColumns().addAll(CourseViewSupport.textColumn("课程 / 教学班",230,Row::name),CourseViewSupport.textColumn("教师",100,Row::teacher),CourseViewSupport.textColumn("教室",105,v->v.schedule.getClassroom()),CourseViewSupport.textColumn("星期",75,v->CourseViewSupport.dayName(v.schedule.getDayOfWeek())),CourseViewSupport.textColumn("周次",80,v->v.schedule.getWeekStart()+"-"+v.schedule.getWeekEnd()+" 周"),CourseViewSupport.textColumn("节次",80,v->v.schedule.getStartPeriod()+"-"+v.schedule.getEndPeriod()+" 节"),CourseViewSupport.textColumn("时间",115,v->CourseViewSupport.timeRange(v.schedule.getStartTime(),v.schedule.getEndTime())));
        CourseViewSupport.configureTable(table,"暂无排课数据");table.getSelectionModel().selectedItemProperty().addListener((o,a,v)->show(v));
        VBox left=new VBox(8,table,status);VBox.setVgrow(table,Priority.ALWAYS);
        teachingClass.setMaxWidth(Double.MAX_VALUE);teachingClass.setConverter(new StringConverter<>(){public String toString(Choice c){return c==null?"":c.course.getCourseId()+" · "+c.course.getCourseName()+" · "+c.teachingClass.getClassNumber()+" 班";}public Choice fromString(String s){return null;}});
        day.setMaxWidth(Double.MAX_VALUE);day.setConverter(new StringConverter<>(){public String toString(Integer n){return n==null?"":CourseViewSupport.dayName(n);}public Integer fromString(String s){return null;}});
        GridPane f=new GridPane();f.setHgap(10);f.setVgap(10);f.getStyleClass().add("form-grid");add(f,0,"教学班",teachingClass);add(f,1,"教室",room);add(f,2,"起始周",weekStart);add(f,3,"结束周",weekEnd);add(f,4,"星期",day);add(f,5,"起始节",periodStart);add(f,6,"结束节",periodEnd);add(f,7,"开始时间",startTime);add(f,8,"结束时间",endTime);
        Label formTitle=new Label("排课详情");formTitle.getStyleClass().add("section-title");HBox actions=new HBox(8,button("新建","secondary",this::fresh),button("保存","primary",this::save),button("删除","danger",this::delete));VBox right=new VBox(12,formTitle,f,actions);right.setPadding(new Insets(12));right.getStyleClass().add("course-card");
        SplitPane split=new SplitPane(left,right);split.setOrientation(Orientation.HORIZONTAL);split.setDividerPositions(.60);VBox.setVgrow(split,Priority.ALWAYS);getChildren().addAll(hint,header,split);
    }
    private void show(Row v){selected=v;if(v==null)return;choices.stream().filter(c->c.teachingClass.getTeachingClassId().equals(v.schedule.getTeachingClassId())).findFirst().ifPresent(teachingClass::setValue);room.setText(v.schedule.getClassroom());day.setValue(v.schedule.getDayOfWeek());weekStart.setText(""+v.schedule.getWeekStart());weekEnd.setText(""+v.schedule.getWeekEnd());periodStart.setText(""+v.schedule.getStartPeriod());periodEnd.setText(""+v.schedule.getEndPeriod());startTime.setText(v.schedule.getStartTime().toString());endTime.setText(v.schedule.getEndTime().toString());}
    private void fresh(){selected=null;table.getSelectionModel().clearSelection();teachingClass.setValue(null);day.setValue(1);room.clear();weekStart.setText("1");weekEnd.setText("16");periodStart.setText("1");periodEnd.setText("2");startTime.setText("08:00");endTime.setText("09:35");}
    private void save(){try{if(teachingClass.getValue()==null||day.getValue()==null)throw new IllegalArgumentException("教学班和星期不能为空");LocalTime start=CourseViewSupport.parseTime(startTime.getText()),end=CourseViewSupport.parseTime(endTime.getText());if(!start.isBefore(end))throw new IllegalArgumentException("结束时间必须晚于开始时间");Choice c=teachingClass.getValue();CourseSchedule v=new CourseSchedule(selected==null?null:selected.schedule.getScheduleId(),c.course.getCourseId(),required(room,"教室"),day.getValue(),start,end);v.setTeachingClassId(c.teachingClass.getTeachingClassId());v.setWeekStart(number(weekStart,"起始周"));v.setWeekEnd(number(weekEnd,"结束周"));v.setStartPeriod(number(periodStart,"起始节"));v.setEndPeriod(number(periodEnd,"结束节"));CourseViewSupport.runAsync(this,()->{if(selected==null)return client.addSchedule(v);client.updateSchedule(v);return v;},x->{status.setText(selected==null?"排课新增成功":"排课保存成功");refresh();});}catch(RuntimeException e){CourseViewSupport.showError(e);}}
    private void delete(){if(selected==null){CourseViewSupport.showError(new IllegalArgumentException("请先选择排课"));return;}if(!CourseViewSupport.confirm("删除该条排课？",selected.name()))return;CourseViewSupport.runAsync(this,()->client.deleteSchedule(selected.schedule.getScheduleId()),x->{fresh();status.setText("排课删除成功");refresh();});}
    private static TextField field(String t){TextField f=new TextField();f.setPromptText(t);f.setMaxWidth(Double.MAX_VALUE);return f;}private static void add(GridPane g,int r,String n,javafx.scene.Node v){Label l=new Label(n);l.getStyleClass().add("field-label");g.add(l,0,r);g.add(v,1,r);GridPane.setHgrow(v,Priority.ALWAYS);}private static Button button(String t,String css,Runnable r){Button b=new Button(t);b.getStyleClass().add(css);b.setOnAction(e->r.run());return b;}private static void HboxGrow(Label l){HBox.setHgrow(l,Priority.ALWAYS);}private static String required(TextField f,String n){String v=f.getText()==null?"":f.getText().trim();if(v.isBlank())throw new IllegalArgumentException(n+"不能为空");return v;}private static int number(TextField f,String n){try{return Integer.parseInt(required(f,n));}catch(NumberFormatException e){throw new IllegalArgumentException(n+"必须是整数");}}
    private record Snapshot(List<Choice> choices,List<Row> rows){} private record Choice(TeachingClass teachingClass,Course course){} private record Row(CourseSchedule schedule,Course course,TeachingClass teachingClass){String name(){return course==null?schedule.getCourseId():course.getCourseId()+" · "+course.getCourseName()+" · "+(teachingClass==null?"—":teachingClass.getClassNumber()+" 班");}String teacher(){return teachingClass==null?"—":teachingClass.getTeacher();}}
}
