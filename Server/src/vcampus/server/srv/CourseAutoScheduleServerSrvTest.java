package vcampus.server.srv;

import vcampus.common.vo.AutoSchedulePlan;
import vcampus.common.vo.AutoScheduleRequest;
import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.server.dao.CourseDAO;
import vcampus.server.dao.CourseScheduleDAO;
import vcampus.server.dao.CourseTestData;

import java.util.List;

/** Preview, transactional apply, and rollback verification for auto scheduling. */
public class CourseAutoScheduleServerSrvTest {
    private static final String FIRST = "T_AUTO_FIRST";
    private static final String SECOND = "T_AUTO_SECOND";

    public static void main(String[] args) throws Exception {
        CourseServerSrv service = new CourseServerSrv();
        cleanup();
        try {
            CourseTestData.prepareCourse(new Course(FIRST, "自动排课甲", "自动教师甲", 2, 20, 0));
            CourseTestData.prepareCourse(new Course(SECOND, "自动排课乙", "自动教师乙", 2, 20, 0));
            AutoScheduleRequest request = new AutoScheduleRequest(
                    List.of(FIRST + "-01", SECOND + "-01"), 1, 16);
            AutoSchedulePlan preview = service.previewAutoSchedule(request);
            require(preview.getAssignments().size() == 2, "Preview 生成完整方案");
            require(countFixtureSchedules() == 0, "Preview 不修改数据库");
            int applied = service.applyAutoSchedule(preview);
            require(applied == 2 && countFixtureSchedules() == 2, "Apply 原子写入全部排课");

            cleanupSchedules();
            AutoSchedulePlan invalid = service.previewAutoSchedule(request);
            invalid.getAssignments().get(1).setScheduleId(
                    invalid.getAssignments().get(0).getScheduleId());
            boolean failed = false;
            try {
                service.applyAutoSchedule(invalid);
            } catch (Exception expected) {
                failed = true;
            }
            require(failed, "Apply 中途失败被抛出");
            require(countFixtureSchedules() == 0, "Apply 中途失败全部回滚");
            System.out.println("COURSE_AUTO_SCHEDULE_SERVER_SRV_TEST=PASS classes="
                    + preview.getTeachingClassCount() + " candidates="
                    + preview.getCandidateSlotCount() + " nodes=" + preview.getSearchNodes()
                    + " backtracks=" + preview.getBacktracks() + " elapsed="
                    + preview.getElapsedMillis() + "ms");
        } finally {
            cleanup();
            require(new CourseDAO().findById(FIRST) == null
                    && new CourseDAO().findById(SECOND) == null, "测试数据清理完成");
            System.out.println("COURSE_AUTO_SCHEDULE_SERVER_SRV_TEST_RESIDUE=0");
        }
    }

    private static int countFixtureSchedules() throws Exception {
        return (int) new CourseScheduleDAO().findAll().stream()
                .filter(value -> value.getTeachingClassId().startsWith("T_AUTO_"))
                .count();
    }

    private static void cleanupSchedules() throws Exception {
        CourseScheduleDAO dao = new CourseScheduleDAO();
        for (CourseSchedule value : dao.findAll()) {
            if (value.getTeachingClassId().startsWith("T_AUTO_")) {
                dao.deleteSchedule(value.getScheduleId());
            }
        }
    }

    private static void cleanup() throws Exception {
        cleanupSchedules();
        for (String id : List.of(FIRST, SECOND)) {
            if (new CourseDAO().findById(id) != null) CourseTestData.cleanupCourse(id);
        }
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("FAIL: " + message);
        System.out.println("PASS: " + message);
    }
}
