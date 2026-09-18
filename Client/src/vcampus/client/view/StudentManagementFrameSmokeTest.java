package vcampus.client.view;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import vcampus.common.vo.Student;
import vcampus.common.vo.StudentCampusOverview;
import vcampus.common.vo.StudentStatus;
import vcampus.common.vo.TeacherCourseEnrollment;
import vcampus.common.vo.User;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** 构建三种角色界面并验证筛选、控件可见性和空字段画像标签。 */
public class StudentManagementFrameSmokeTest {

    public static void main(String[] args) throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            try {
                runAssertions();
            } catch (Throwable exception) {
                failure.set(exception);
            } finally {
                completed.countDown();
            }
        });
        completed.await();
        Platform.exit();
        if (failure.get() != null) throw new AssertionError(failure.get());
        System.out.println("STUDENT_MANAGEMENT_FRAME_SMOKE_TEST=PASS");
    }

    private static void runAssertions() throws Exception {
        List<Student> rows = sampleRows();
        User admin = user("ADMIN001", "管理员");
        User teacher = user("TCH00001", "教师");
        User student = user("STU00001", "学生");

        StudentManagementFrame adminFrame = new StudentManagementFrame(admin);
        BorderPane adminRoot = adminFrame.createView();
        @SuppressWarnings("unchecked")
        java.util.Map<String, Button> yearButtons =
                (java.util.Map<String, Button>) fieldValue(adminFrame, "_yearButtons");
        require(yearButtons.keySet().containsAll(List.of("2023", "2024", "2025", "2026")),
                "管理员界面缺少四个年份按钮");
        ComboBox<?> adminMajor = fieldValue(adminFrame, "_majorFilterBox");
        require("选择专业".equals(adminMajor.getPromptText()),
                "管理员界面缺少专业下拉框");
        require(hasButton(adminRoot, "年级对比"), "管理员界面缺少年级对比入口");

        StudentManagementFrame teacherFrame = new StudentManagementFrame(teacher);
        BorderPane teacherRoot = teacherFrame.createView();
        ComboBox<?> teacherCourse = fieldValue(teacherFrame, "_classFilterBox");
        require("选择课程名称".equals(teacherCourse.getPromptText()),
                "教师界面缺少课程名称下拉框");
        @SuppressWarnings("unchecked")
        java.util.Map<String, Button> teacherYearButtons =
                (java.util.Map<String, Button>) fieldValue(teacherFrame, "_yearButtons");
        require(teacherYearButtons.isEmpty()
                        && !hasButton(teacherRoot, "年级对比") && !hasText(teacherRoot, "选择年份"),
                "教师界面不应显示年份对比控件");

        StudentManagementFrame studentFrame = new StudentManagementFrame(student);
        studentFrame.createView();
        Button studentUndo = fieldValue(studentFrame, "_undoButton");
        require(studentUndo.getParent() == null,
                "学生界面仍显示撤回按钮");

        setField(adminFrame, "_selectedYear", "2024");
        setField(adminFrame, "_selectedMajor", "测试专业B");
        List<Student> majorRows = invokeScopeFilter(adminFrame, rows, "学号", "", "全部状态");
        require(majorRows.size() == 1 && "2024".equals(majorRows.get(0).getGrade()),
                "管理员年级和专业筛选失败");
        setField(adminFrame, "_selectedYear", "2025");
        require(invokeScopeFilter(adminFrame, rows, "学号", "", "全部状态").isEmpty(),
                "无学生年级没有返回空列表");
        Map<String, List<Student>> cohortGroups = StudentManagementFrame.buildCohortGroups(rows);
        require(cohortGroups.keySet().equals(
                        new java.util.LinkedHashSet<>(List.of("2023", "2024", "2025", "2026"))),
                "年级对比没有保留四个固定年级选项");
        require(cohortGroups.get("2025").isEmpty(), "无学生年级没有显示为 0 人");

        setField(teacherFrame, "_teacherCourseEnrollments", List.of(
                enrollment("测试课程A", rows.get(0)),
                enrollment("测试课程A", rows.get(1)),
                enrollment("测试课程B", rows.get(2))));
        setField(teacherFrame, "_selectedClass", "测试课程A");
        List<Student> classRows = invokeScopeFilter(teacherFrame, rows, "姓名", "重名", "在读");
        require(classRows.size() == 1 && "测试班A".equals(classRows.get(0).getClassName()),
                "教师课程、姓名和状态筛选失败");

        Method tagsMethod = StudentManagementFrame.class.getDeclaredMethod(
                "buildAutoTags", Student.class, StudentCampusOverview.class);
        tagsMethod.setAccessible(true);
        Student incomplete = rows.get(3);
        StudentCampusOverview overview = new StudentCampusOverview();
        @SuppressWarnings("unchecked") List<String> tags =
                (List<String>) tagsMethod.invoke(null, incomplete, overview);
        require(tags.contains("资料待补全"), "空班级/专业没有被标记为资料待补全");
    }

    private static List<Student> invokeScopeFilter(StudentManagementFrame frame,
                                                    List<Student> rows, String type,
                                                    String keyword, String status) throws Exception {
        Method method = StudentManagementFrame.class.getDeclaredMethod(
                "filterStudentsForCurrentScope", List.class, String.class, String.class,
                String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked") List<Student> result =
                (List<Student>) method.invoke(frame, rows, type, keyword, status);
        return result;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = StudentManagementFrame.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T fieldValue(Object target, String name) throws Exception {
        Field field = StudentManagementFrame.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static List<Student> sampleRows() {
        return List.of(row("2023", "测试班A", "测试专业A", "重名", StudentStatus.ENROLLED),
                row("2023", "测试班A", "测试专业A", "重名", StudentStatus.SUSPENDED),
                row("2024", "测试班B", "测试专业B", "毕业生", StudentStatus.GRADUATED),
                row("2024", "", "", "空字段", StudentStatus.ENROLLED));
    }

    private static Student row(String grade, String className, String major, String name,
                               StudentStatus status) {
        Student student = new Student();
        student.setStudentId("SMOKE" + grade + name);
        student.setCampusCardNo("CARD-" + grade + name);
        student.setName(name);
        student.setClassName(className);
        student.setMajor(major);
        student.setGrade(grade);
        student.setEnrollmentDate(LocalDate.of(Integer.parseInt(grade), 9, 1));
        student.setStatus(status);
        return student;
    }

    private static TeacherCourseEnrollment enrollment(String courseName, Student student) {
        TeacherCourseEnrollment enrollment = new TeacherCourseEnrollment();
        enrollment.setCourseName(courseName);
        enrollment.setStudentId(student.getStudentId());
        return enrollment;
    }

    private static User user(String id, String role) {
        User user = new User();
        user.setUId(id);
        user.setURole(role);
        return user;
    }

    private static boolean hasButton(Node root, String text) {
        return nodes(root, Button.class).stream().anyMatch(button -> text.equals(button.getText()));
    }

    private static boolean hasText(Node root, String text) {
        return nodes(root, Label.class).stream().anyMatch(label -> text.equals(label.getText()));
    }

    private static <T extends Node> List<T> nodes(Node root, Class<T> type) {
        List<T> result = new ArrayList<>();
        if (type.isInstance(root)) result.add(type.cast(root));
        if (root instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) result.addAll(nodes(child, type));
        }
        return result;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
