package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.CourseScore;

/** Student-facing score publication page; only approved results reach this pane. */
public class StudentScoresPane extends VBox {
    private final ICourseClientSrv client; private final String studentId;
    private final TableView<CourseScore> table = new TableView<>(); private final Label status = new Label();
    public StudentScoresPane(ICourseClientSrv client, String studentId) {
        this.client=client; this.studentId=studentId; getStyleClass().add("course-page");
        Label title=new Label("成绩查询"); title.getStyleClass().add("page-title");
        Label description=new Label("仅显示已由教务审核发布的成绩"); description.getStyleClass().add("page-description");
        table.getColumns().addAll(CourseViewSupport.textColumn("课程",220,CourseScore::getCourseName),
                CourseViewSupport.textColumn("教学班",145,CourseScore::getTeachingClassId),
                CourseViewSupport.textColumn("教师",120,CourseScore::getTeacher),
                CourseViewSupport.textColumn("成绩",80,v->String.valueOf(v.getScore())),
                CourseViewSupport.textColumn("状态",100,v->"已发布"));
        CourseViewSupport.configureTable(table,"暂无已发布成绩"); VBox.setVgrow(table, Priority.ALWAYS);
        status.getStyleClass().add("status-label"); getChildren().addAll(title,description,table,status); refresh();
    }
    public void refresh(){ status.setText("正在读取成绩…"); CourseViewSupport.runAsync(this,()->client.queryStudentScores(studentId), rows->{table.setItems(FXCollections.observableArrayList(rows));status.setText("已发布 "+rows.size()+" 门课程成绩");}); }
}
