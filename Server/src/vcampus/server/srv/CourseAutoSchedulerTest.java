package vcampus.server.srv;

import vcampus.common.vo.AutoSchedulePlan;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.TeachingClass;

import java.time.LocalTime;
import java.util.List;

/** Deterministic checks for the pure CSP scheduling component. */
public class CourseAutoSchedulerTest {
    public static void main(String[] args) {
        TeachingClass occupiedClass = teaching("EXIST", "C0", "教师甲");
        TeachingClass target = teaching("TARGET", "C1", "教师甲");
        CourseSchedule occupied = schedule("S0", "EXIST", "C0", 1, 1, 8, 1, 2, "教一-101");
        CourseSchedule secondPattern = schedule("S1", "OTHER", "C2", 2, 1, 16, 3, 5, "教二-202");
        TeachingClass other = teaching("OTHER", "C2", "教师乙");

        AutoSchedulePlan feasible = new CourseAutoScheduler().generate(List.of(target),
                List.of(occupiedClass, target, other), List.of(occupied, secondPattern), 1, 16);
        require(feasible.getAssignments().size() == 1, "简单可行场景成功");
        CourseSchedule chosen = feasible.getAssignments().get(0);
        require(!CourseAutoScheduler.overlaps(chosen, occupied)
                        || !chosen.getClassroom().equals(occupied.getClassroom()),
                "避免教室冲突");
        require(!CourseAutoScheduler.overlaps(chosen, occupied), "避免教师冲突");

        AutoSchedulePlan offset = new CourseAutoScheduler().generate(List.of(target),
                List.of(occupiedClass, target), List.of(occupied), 9, 16);
        require(offset.getAssignments().size() == 1, "周次错开允许相同时间");

        CourseSchedule fullBlock = schedule("S2", "EXIST", "C0", 1, 1, 16, 1, 2, "唯一教室");
        AutoSchedulePlan impossible = new CourseAutoScheduler().generate(List.of(target),
                List.of(occupiedClass, target), List.of(fullBlock), 1, 16);
        require(impossible.getAssignments().isEmpty()
                        && impossible.getUnassignedTeachingClassIds().size() == 1,
                "无可行解明确返回失败");
        System.out.println("COURSE_AUTO_SCHEDULER_TEST=PASS nodes="
                + feasible.getSearchNodes() + " backtracks=" + feasible.getBacktracks());
    }

    private static TeachingClass teaching(String id, String course, String teacher) {
        return new TeachingClass(id, course, "01", teacher, 30, 0, null, null);
    }

    private static CourseSchedule schedule(String id, String classId, String courseId,
                                           int day, int weekStart, int weekEnd,
                                           int start, int end, String room) {
        CourseSchedule value = new CourseSchedule();
        value.setScheduleId(id);
        value.setTeachingClassId(classId);
        value.setCourseId(courseId);
        value.setDayOfWeek(day);
        value.setWeekStart(weekStart);
        value.setWeekEnd(weekEnd);
        value.setStartPeriod(start);
        value.setEndPeriod(end);
        value.setStartTime(start == 1 ? LocalTime.of(8, 0) : LocalTime.of(9, 55));
        value.setEndTime(start == 1 ? LocalTime.of(9, 35) : LocalTime.of(12, 20));
        value.setClassroom(room);
        return value;
    }

    private static void require(boolean value, String message) {
        if (!value) throw new IllegalStateException("FAIL: " + message);
        System.out.println("PASS: " + message);
    }
}
