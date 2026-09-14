package vcampus.server.srv;

import vcampus.common.vo.Course;
import vcampus.common.vo.TeachingClass;
import vcampus.server.dao.CourseDAO;
import vcampus.server.dao.CourseTestData;
import vcampus.server.dao.SelectCourseDAO;
import vcampus.server.dao.TeachingClassDAO;

/** Executable Service test for section enrollment and same-Course exclusion. */
public class TeachingClassServerSrvTest {
    private static final String COURSE_ID = "T_TCSRV_260909";
    private static final String CLASS_TWO = COURSE_ID + "-02";
    private static final String USER_ID = "TC090909";
    private static final String STUDENT_ID = "TC09090001";

    public static void main(String[] args) throws Exception {
        CourseServerSrv service = new CourseServerSrv();
        cleanup();
        try {
            CourseTestData.prepareStudent(USER_ID, STUDENT_ID);
            service.addCourse(new Course(COURSE_ID, "教学班 Service 测试",
                    "教师甲", 2, 2, 0));
            TeachingClass second = service.addTeachingClass(new TeachingClass(
                    CLASS_TWO, COURSE_ID, "02", "教师乙", 1, 99, null, null));
            require(second.getSelectedCount() == 0, "新增教学班人数由服务端置零");
            require(service.selectCourse(STUDENT_ID, CLASS_TWO), "选择指定教学班");
            require(service.querySelectedCourse(STUDENT_ID).stream()
                    .anyMatch(row -> CLASS_TWO.equals(row.getTeachingClassId())),
                    "选课记录指向指定教学班");
            expectFailure(() -> service.selectCourse(STUDENT_ID, COURSE_ID + "-01"),
                    "同一学生不能选择同一 Course 的另一教学班");
            require(new TeachingClassDAO().findById(CLASS_TWO).getSelectedCount() == 1,
                    "教学班人数与选课同步");
            require(service.dropCourse(STUDENT_ID, CLASS_TWO), "退选指定教学班");
            second.setCapacity(3);
            require(service.updateTeachingClass(second), "修改教学班");
            require(service.deleteTeachingClass(CLASS_TWO), "删除无引用教学班");
            require(service.deleteCourse(COURSE_ID), "删除课程及默认教学班");
            System.out.println("TEACHING_CLASS_SERVER_SRV_TEST=PASS");
        } finally {
            cleanup();
            int residue = (new CourseDAO().findById(COURSE_ID) == null ? 0 : 1)
                    + CourseTestData.countResidue(USER_ID, STUDENT_ID);
            System.out.println("TEACHING_CLASS_SERVER_SRV_TEST_RESIDUE=" + residue);
            require(residue == 0, "测试数据清理完成");
        }
    }

    private static void cleanup() throws Exception {
        SelectCourseDAO selectDAO = new SelectCourseDAO();
        selectDAO.deleteSelectCourse(STUDENT_ID, COURSE_ID);
        CourseDAO courseDAO = new CourseDAO();
        if (courseDAO.findById(COURSE_ID) != null) {
            CourseTestData.cleanupCourse(COURSE_ID);
        }
        CourseTestData.cleanupStudent(USER_ID, STUDENT_ID);
    }

    private static void expectFailure(Action action, String message) throws Exception {
        boolean failed = false;
        try { action.run(); } catch (CourseServiceException expected) { failed = true; }
        require(failed, message);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("FAIL: " + message);
        System.out.println("PASS: " + message);
    }

    @FunctionalInterface
    private interface Action { void run() throws Exception; }
}
