package vcampus.server.dao;

import vcampus.common.vo.Course;
import vcampus.common.vo.TeachingClass;

/** Executable CRUD test for the normalized teaching-class DAO. */
public class TeachingClassDAOTest {
    private static final String COURSE_ID = "T_TCDAO_260909";
    private static final String CLASS_ID = COURSE_ID + "-02";

    public static void main(String[] args) throws Exception {
        CourseDAO courseDAO = new CourseDAO();
        TeachingClassDAO dao = new TeachingClassDAO();
        cleanup();
        try {
            require(courseDAO.insertCourse(new Course(
                    COURSE_ID, "教学班 DAO 测试", "兼容教师", 2, 20, 0)), "准备课程本体");
            TeachingClass value = new TeachingClass(CLASS_ID, COURSE_ID, "02",
                    "教学班教师", 25, 0, "中文", "测试");
            require(dao.insert(value), "插入教学班");
            TeachingClass found = dao.findById(CLASS_ID);
            require(found != null && "02".equals(found.getClassNumber())
                    && found.getCapacity() == 25, "按 ID 查询并映射教学班");
            require(dao.findByKeyword("教学班教师").stream()
                    .anyMatch(item -> CLASS_ID.equals(item.getTeachingClassId())), "按教师查询教学班");
            value.setCapacity(30);
            try (var conn = DbHelper.getConnection()) {
                require(dao.update(conn, value), "修改教学班");
            }
            require(dao.findById(CLASS_ID).getCapacity() == 30, "回查教学班容量");
            try (var conn = DbHelper.getConnection()) {
                require(dao.delete(conn, CLASS_ID), "删除教学班");
            }
            require(dao.findById(CLASS_ID) == null, "删除后不存在");
            System.out.println("TEACHING_CLASS_DAO_TEST=PASS");
        } finally {
            cleanup();
            int residue = (courseDAO.findById(COURSE_ID) == null ? 0 : 1)
                    + (dao.findById(CLASS_ID) == null ? 0 : 1);
            System.out.println("TEACHING_CLASS_DAO_TEST_RESIDUE=" + residue);
            require(residue == 0, "测试数据清理完成");
        }
    }

    private static void cleanup() throws Exception {
        TeachingClassDAO dao = new TeachingClassDAO();
        try (var conn = DbHelper.getConnection()) {
            if (dao.findById(conn, CLASS_ID, false) != null) dao.delete(conn, CLASS_ID);
        }
        CourseDAO courseDAO = new CourseDAO();
        if (courseDAO.findById(COURSE_ID) != null) courseDAO.deleteCourse(COURSE_ID);
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("FAIL: " + message);
        System.out.println("PASS: " + message);
    }
}
