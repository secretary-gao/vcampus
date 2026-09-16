package vcampus.client.view.course;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.converter.IntegerStringConverter;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.CourseScore;
import vcampus.common.vo.TeacherCourseEnrollment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Teacher score-entry workspace, limited by server-side teaching-class ownership checks. */
public class TeacherScorePane extends VBox {
    private final ICourseClientSrv client; private final String teacher; private final ComboBox<String> classes=new ComboBox<>();
    private final TableView<ScoreRow> table=new TableView<>(); private final Label status=new Label();
    public TeacherScorePane(ICourseClientSrv client,String teacher){this.client=client;this.teacher=teacher;getStyleClass().add("course-page");
        Label title=new Label("成绩录入");title.getStyleClass().add("page-title");Label hint=new Label("提交后进入待教务审核；仅可录入本人教学班");hint.getStyleClass().add("page-description");
        classes.setPromptText("选择教学班");classes.setOnAction(e->loadRows());Button refresh=new Button("刷新");refresh.getStyleClass().add("secondary");refresh.setOnAction(e->refresh());Button submit=new Button("提交待审核");submit.getStyleClass().add("primary");submit.setOnAction(e->submit());HBox bar=new HBox(9,classes,refresh,submit);bar.getStyleClass().add("tool-bar-card");
        table.setEditable(true);table.getColumns().addAll(CourseViewSupport.textColumn("学号",125,r->r.score.getStudentId()),CourseViewSupport.textColumn("姓名",100,r->r.score.getStudentName()),CourseViewSupport.textColumn("课程",180,r->r.score.getCourseName()),scoreColumn(),CourseViewSupport.textColumn("状态",110,r->statusText(r.score.getStatus())));CourseViewSupport.configureTable(table,"请选择有学生的教学班");VBox.setVgrow(table,Priority.ALWAYS);status.getStyleClass().add("status-label");getChildren().addAll(title,hint,bar,table,status);refresh();}
    private TableColumn<ScoreRow,Integer> scoreColumn(){TableColumn<ScoreRow,Integer> col=new TableColumn<>("成绩");col.setPrefWidth(100);col.setCellValueFactory(v->new SimpleObjectProperty<>(v.getValue().score.getScore()));col.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));col.setOnEditCommit(e->{Integer value=e.getNewValue();if(value==null||value<0||value>100){CourseViewSupport.showError(new IllegalArgumentException("成绩必须在 0 到 100 之间"));table.refresh();return;}e.getRowValue().score.setScore(value);});return col;}
    private void refresh(){status.setText("正在读取本人教学班…");CourseViewSupport.runAsync(this,()->client.queryTeacherCourseEnrollments(teacher),rows->{List<String> ids=rows.stream().map(TeacherCourseEnrollment::getTeachingClassId).filter(v->v!=null).distinct().toList();classes.setItems(FXCollections.observableArrayList(ids));if(!ids.isEmpty())classes.setValue(ids.get(0));loadRows();});}
    private void loadRows(){String classId=classes.getValue();if(classId==null)return;status.setText("正在读取成绩…");CourseViewSupport.runAsync(this,()->{List<TeacherCourseEnrollment> roster=client.queryTeacherCourseEnrollments(teacher).stream().filter(v->classId.equals(v.getTeachingClassId())&&v.getStudentId()!=null).toList();Map<String,CourseScore> old=new LinkedHashMap<>();for(CourseScore s:client.queryTeacherScores(teacher))old.put(s.getStudentId()+"@"+s.getTeachingClassId(),s);return roster.stream().map(r->{CourseScore s=old.get(r.getStudentId()+"@"+classId);if(s==null){s=new CourseScore();s.setStudentId(r.getStudentId());s.setStudentName(r.getStudentName());s.setCourseId(r.getCourseId());s.setCourseName(r.getCourseName());s.setTeachingClassId(classId);s.setTeacher(teacher);s.setStatus("未录入");}return new ScoreRow(s);}).toList();},rows->{table.setItems(FXCollections.observableArrayList(rows));status.setText("当前教学班 "+rows.size()+" 名学生");});}
    private void submit(){List<CourseScore> values=table.getItems().stream().map(r->r.score).filter(s->s.getScore()!=null).toList();if(values.isEmpty()){CourseViewSupport.showError(new IllegalArgumentException("请至少录入一名学生的成绩"));return;}CourseViewSupport.runAsync(this,()->client.submitScores(teacher,values),count->{status.setText("已提交 "+count+" 条成绩，等待管理员审核");loadRows();});}
    private String statusText(String value){return "APPROVED".equals(value)?"已发布":"PENDING".equals(value)?"待审核":"REJECTED".equals(value)?"已拒绝":"未录入";}
    private static class ScoreRow { final CourseScore score; ScoreRow(CourseScore score){this.score=score;} }
}
