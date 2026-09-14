package vcampus.server.srv;

import vcampus.common.vo.Course;
import vcampus.common.vo.CourseSchedule;
import vcampus.server.dao.CourseDAO;
import vcampus.server.dao.CourseScheduleDAO;
import vcampus.server.dao.CourseTestData;
import vcampus.server.dao.SelectCourseDAO;
import vcampus.server.dao.TeachingClassDAO;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Verifies authoritative student schedule-conflict validation and rollback. */
public class StudentScheduleConflictServerSrvTest {

    private final String _runId = UUID.randomUUID().toString().replace("-", "")
            .substring(0, 10);
    private final String _studentId = "CF" + _runId.substring(0, 8);
    private final String _userId = ("CF" + _runId).substring(0, 8);
    private final String _source = "CFS" + _runId;
    private final String _overlap = "CFO" + _runId;
    private final String _otherWeeks = "CFW" + _runId;
    private final String _adjacent = "CFA" + _runId;
    private final String _otherDay = "CFD" + _runId;
    private final String _multi = "CFM" + _runId;
    private final List<String> _courses = new ArrayList<>();
    private final List<String> _schedules = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        new StudentScheduleConflictServerSrvTest().run();
    }

    private void run() throws Exception {
        CourseDAO courseDAO = new CourseDAO();
        SelectCourseDAO selectDAO = new SelectCourseDAO();
        CourseScheduleDAO scheduleDAO = new CourseScheduleDAO();
        CourseServerSrv service = new CourseServerSrv(courseDAO, selectDAO, scheduleDAO);
        TeachingClassDAO classDAO = new TeachingClassDAO();

        try {
            CourseTestData.prepareStudent(_userId, _studentId);
            prepareCourse(_source, "冲突来源课程", "教师甲");
            prepareCourse(_overlap, "周次节次重叠课程", "教师乙");
            prepareCourse(_otherWeeks, "错开周次课程", "教师丙");
            prepareCourse(_adjacent, "相邻节次课程", "教师丁");
            prepareCourse(_otherDay, "不同星期课程", "教师戊");
            prepareCourse(_multi, "多段排课课程", "教师己");

            addSchedule(service, _source, 1, 8, 1, 3, 4, "冲突测试-101");
            addSchedule(service, _overlap, 5, 12, 1, 4, 5, "冲突测试-102");
            addSchedule(service, _otherWeeks, 9, 16, 1, 3, 4, "冲突测试-103");
            addSchedule(service, _adjacent, 1, 8, 1, 5, 6, "冲突测试-104");
            addSchedule(service, _otherDay, 1, 8, 2, 3, 4, "冲突测试-105");
            addSchedule(service, _multi, 1, 8, 2, 8, 9, "冲突测试-106");
            addSchedule(service, _multi, 6, 10, 1, 4, 5, "冲突测试-107");

            require(service.selectCourse(_studentId, classId(_source)),
                    "准备已有课程");

            expectConflict(() -> service.selectCourse(_studentId, classId(_overlap)),
                    "同星期、周次重叠、节次重叠时拒绝");
            require(selectDAO.findByStudentAndCourse(_studentId, _overlap) == null,
                    "冲突拒绝后 SelectCourse 不新增");
            require(classDAO.findById(classId(_overlap)).getSelectedCount() == 0,
                    "冲突拒绝后候选教学班 selectedCount 不变化");
            require(classDAO.findById(classId(_source)).getSelectedCount() == 1,
                    "冲突拒绝后已有教学班 selectedCount 不变化");

            require(service.selectCourse(_studentId, classId(_otherWeeks)),
                    "同星期、同节次但周次不重叠时允许");
            require(service.dropCourse(_studentId, classId(_otherWeeks)),
                    "清理错开周次课程");

            require(service.selectCourse(_studentId, classId(_adjacent)),
                    "同星期、同周次但相邻节次不重叠时允许");
            require(service.dropCourse(_studentId, classId(_adjacent)),
                    "清理相邻节次课程");

            require(service.selectCourse(_studentId, classId(_otherDay)),
                    "不同星期、相同节次时允许");
            require(service.dropCourse(_studentId, classId(_otherDay)),
                    "清理不同星期课程");

            expectConflict(() -> service.selectCourse(_studentId, classId(_multi)),
                    "候选教学班多条 Schedule 中任意一条冲突时拒绝");
            require(selectDAO.findByStudentAndCourse(_studentId, _multi) == null
                            && classDAO.findById(classId(_multi)).getSelectedCount() == 0,
                    "多段排课冲突拒绝后事务无残留");

            require(service.dropCourse(_studentId, classId(_source)),
                    "退掉冲突来源课程");
            require(service.selectCourse(_studentId, classId(_multi)),
                    "退掉冲突来源后原候选教学班可以选择");
            require(service.dropCourse(_studentId, classId(_multi)),
                    "清理最终选课");

            verifyConcurrentConflict(service, selectDAO, classDAO);

            System.out.println("STUDENT_SCHEDULE_CONFLICT_SERVER_SRV_TEST=PASS");
        } finally {
            cleanup(selectDAO, scheduleDAO);
            int residue = CourseTestData.countResidue(_userId, _studentId);
            for (String courseId : _courses) {
                residue += courseDAO.findById(courseId) == null ? 0 : 1;
            }
            System.out.println("STUDENT_SCHEDULE_CONFLICT_SERVER_SRV_TEST_RESIDUE=" + residue);
            require(residue == 0, "冲突测试数据清理完成");
        }
    }

    private void prepareCourse(String courseId, String name, String teacher) throws Exception {
        require(CourseTestData.prepareCourse(
                new Course(courseId, name, teacher + _runId, 2, 20, 0)),
                "准备测试课程 " + name);
        _courses.add(courseId);
    }

    private void addSchedule(CourseServerSrv service, String courseId,
                             int weekStart, int weekEnd, int dayOfWeek,
                             int startPeriod, int endPeriod, String room) throws Exception {
        CourseSchedule schedule = new CourseSchedule();
        schedule.setCourseId(courseId);
        schedule.setTeachingClassId(classId(courseId));
        schedule.setWeekStart(weekStart);
        schedule.setWeekEnd(weekEnd);
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setStartPeriod(startPeriod);
        schedule.setEndPeriod(endPeriod);
        schedule.setClassroom(room + _runId);
        schedule.setStartTime(LocalTime.of(8, 0).plusMinutes((startPeriod - 1L) * 50));
        schedule.setEndTime(LocalTime.of(8, 0).plusMinutes(endPeriod * 50L - 5));
        CourseSchedule added = service.addSchedule(schedule);
        _schedules.add(added.getScheduleId());
    }

    private void cleanup(SelectCourseDAO selectDAO, CourseScheduleDAO scheduleDAO)
            throws Exception {
        for (String courseId : _courses) {
            selectDAO.deleteSelectCourse(_studentId, courseId);
        }
        for (String scheduleId : _schedules) {
            if (scheduleDAO.findById(scheduleId) != null) {
                scheduleDAO.deleteSchedule(scheduleId);
            }
        }
        for (String courseId : _courses) {
            CourseTestData.cleanupCourse(courseId);
        }
        CourseTestData.cleanupStudent(_userId, _studentId);
    }

    private String classId(String courseId) {
        return courseId + "-01";
    }

    /** Proves that the per-student row lock closes the concurrent check/insert gap. */
    private void verifyConcurrentConflict(CourseServerSrv service,
                                          SelectCourseDAO selectDAO,
                                          TeachingClassDAO classDAO) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Boolean> sourceResult = executor.submit(
                    () -> concurrentSelect(service, classId(_source), ready, start));
            Future<Boolean> overlapResult = executor.submit(
                    () -> concurrentSelect(service, classId(_overlap), ready, start));
            ready.await();
            start.countDown();
            boolean sourceSelected = sourceResult.get();
            boolean overlapSelected = overlapResult.get();
            require(sourceSelected ^ overlapSelected,
                    "并发选择相互冲突教学班时只有一个事务成功");
            int records = (selectDAO.findByStudentAndCourse(_studentId, _source) == null ? 0 : 1)
                    + (selectDAO.findByStudentAndCourse(_studentId, _overlap) == null ? 0 : 1);
            int counts = classDAO.findById(classId(_source)).getSelectedCount()
                    + classDAO.findById(classId(_overlap)).getSelectedCount();
            require(records == 1 && counts == 1,
                    "并发冲突后记录与 selectedCount 均只增加一次");
            String selectedClass = sourceSelected ? classId(_source) : classId(_overlap);
            require(service.dropCourse(_studentId, selectedClass), "清理并发选课记录");
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private boolean concurrentSelect(CourseServerSrv service, String teachingClassId,
                                     CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            return service.selectCourse(_studentId, teachingClassId);
        } catch (CourseServiceException expected) {
            if (!expected.getMessage().contains("课程时间冲突")) {
                throw expected;
            }
            return false;
        }
    }

    private void expectConflict(CheckedAction action, String description) throws Exception {
        try {
            action.run();
            throw new IllegalStateException("FAIL: " + description);
        } catch (CourseServiceException expected) {
            require(expected.getMessage().contains("课程时间冲突")
                            && expected.getMessage().contains("冲突来源课程"),
                    description + "，并返回明确冲突课程");
        }
    }

    private void require(boolean condition, String description) {
        if (!condition) {
            throw new IllegalStateException("FAIL: " + description);
        }
        System.out.println("PASS: " + description);
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
