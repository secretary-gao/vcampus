package vcampus.client.view.course;

import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import vcampus.client.biz.ICourseClientSrv;
import vcampus.common.vo.CourseScore;
import java.util.List;

/** 管理员成绩审核队列，支持逐条及批量审核。 */
public class ScoreReviewPane extends VBox {
    private final ICourseClientSrv client; private final TableView<CourseScore> table=new TableView<>(); private final Label status=new Label();
    public ScoreReviewPane(ICourseClientSrv client){this.client=client;getStyleClass().add("course-page");
        Label title=new Label("成绩审核");title.getStyleClass().add("page-title");Label hint=new Label("审核通过后，学生端立即可见；批量操作处理当前待审核记录");hint.getStyleClass().add("page-description");
        HBox tools=new HBox(8,button("刷新","secondary",this::refresh),button("全部通过","success",()->reviewAll(true)),button("全部拒绝","danger",()->reviewAll(false)));tools.getStyleClass().add("tool-bar-card");
        table.getColumns().addAll(CourseViewSupport.textColumn("学生",175,v->v.getStudentName()+" · "+v.getStudentId()),CourseViewSupport.textColumn("课程",190,CourseScore::getCourseName),CourseViewSupport.textColumn("教学班",155,CourseScore::getTeachingClassId),CourseViewSupport.textColumn("教师",110,CourseScore::getTeacher),CourseViewSupport.textColumn("成绩",70,v->String.valueOf(v.getScore())),actions());
        CourseViewSupport.configureTable(table,"暂无待审核成绩");VBox.setVgrow(table,Priority.ALWAYS);status.getStyleClass().add("status-label");getChildren().addAll(title,hint,tools,table,status);refresh();}
    private TableColumn<CourseScore,Void> actions(){TableColumn<CourseScore,Void> c=new TableColumn<>("操作");c.setPrefWidth(160);c.setCellFactory(x->new TableCell<>(){final Button ok=button("通过","success",()->{CourseScore row=getTableRow().getItem();if(row!=null)review(row,true);});final Button no=button("拒绝","danger",()->{CourseScore row=getTableRow().getItem();if(row!=null)review(row,false);});final HBox box=new HBox(7,ok,no);{ok.getStyleClass().add("table-action");no.getStyleClass().add("table-action");}@Override protected void updateItem(Void v,boolean empty){super.updateItem(v,empty);setGraphic(empty?null:box);}});return c;}
    private void review(CourseScore score,boolean approved){CourseViewSupport.runAsync(this,()->client.reviewScore(score.getScoreId(),approved),x->{status.setText(approved?"已通过并发布成绩":"已拒绝成绩");refresh();});}
    private void reviewAll(boolean approved){List<CourseScore> values=List.copyOf(table.getItems());if(values.isEmpty()){status.setText("当前没有待审核成绩");return;}String action=approved?"全部通过":"全部拒绝";if(!CourseViewSupport.confirm(action+"？","将处理 "+values.size()+" 条待审核成绩。"))return;status.setText("正在"+action+" "+values.size()+" 条成绩…");CourseViewSupport.runAsync(this,()->{int count=0;for(CourseScore value:values)if(client.reviewScore(value.getScoreId(),approved))count++;return count;},count->{status.setText(action+"完成："+count+" 条");refresh();});}
    public void refresh(){status.setText("正在读取待审核成绩…");CourseViewSupport.runAsync(this,client::queryPendingScores,rows->{table.setItems(FXCollections.observableArrayList(rows));status.setText("待审核 "+rows.size()+" 条");});}
    private static Button button(String text,String css,Runnable action){Button b=new Button(text);b.getStyleClass().add(css);b.setOnAction(e->action.run());return b;}
}
