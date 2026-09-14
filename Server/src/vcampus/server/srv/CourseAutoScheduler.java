package vcampus.server.srv;

import vcampus.common.vo.AutoSchedulePlan;
import vcampus.common.vo.CourseSchedule;
import vcampus.common.vo.TeachingClass;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Backtracking CSP scheduler using MRV over real timetable patterns and classrooms. */
public class CourseAutoScheduler {
    private long _nodes;
    private long _backtracks;

    public AutoSchedulePlan generate(List<TeachingClass> targets,
                                     List<TeachingClass> allClasses,
                                     List<CourseSchedule> existing,
                                     int weekStart, int weekEnd) {
        long started = System.nanoTime();
        _nodes = 0;
        _backtracks = 0;
        Map<String, TeachingClass> classes = new HashMap<>();
        allClasses.forEach(value -> classes.put(value.getTeachingClassId(), value));
        List<Slot> domain = candidateDomain(existing);
        List<CourseSchedule> assigned = new ArrayList<>();
        List<TeachingClass> remaining = new ArrayList<>(targets);
        boolean solved = search(remaining, assigned, existing, classes, domain,
                weekStart, weekEnd);

        AutoSchedulePlan plan = new AutoSchedulePlan();
        plan.setTeachingClassCount(targets.size());
        plan.setCandidateSlotCount(domain.size());
        plan.setSearchNodes(_nodes);
        plan.setBacktracks(_backtracks);
        plan.setElapsedMillis((System.nanoTime() - started) / 1_000_000);
        if (solved) {
            plan.setAssignments(assigned);
            plan.setUnassignedTeachingClassIds(List.of());
        } else {
            plan.setAssignments(List.of());
            plan.setUnassignedTeachingClassIds(targets.stream()
                    .map(TeachingClass::getTeachingClassId).toList());
        }
        return plan;
    }

    private boolean search(List<TeachingClass> remaining,
                           List<CourseSchedule> assigned,
                           List<CourseSchedule> existing,
                           Map<String, TeachingClass> classes,
                           List<Slot> domain, int weekStart, int weekEnd) {
        if (remaining.isEmpty()) return true;
        _nodes++;
        TeachingClass next = remaining.stream().min(Comparator.comparingLong(value ->
                feasible(value, assigned, existing, classes, domain, weekStart, weekEnd)
                        .size())).orElseThrow();
        List<CourseSchedule> candidates = feasible(next, assigned, existing, classes,
                domain, weekStart, weekEnd);
        if (candidates.isEmpty()) {
            _backtracks++;
            return false;
        }
        remaining.remove(next);
        for (CourseSchedule candidate : candidates) {
            assigned.add(candidate);
            if (search(remaining, assigned, existing, classes, domain, weekStart, weekEnd)) {
                return true;
            }
            assigned.remove(assigned.size() - 1);
        }
        remaining.add(next);
        _backtracks++;
        return false;
    }

    private List<CourseSchedule> feasible(TeachingClass target,
                                          List<CourseSchedule> assigned,
                                          List<CourseSchedule> existing,
                                          Map<String, TeachingClass> classes,
                                          List<Slot> domain, int weekStart, int weekEnd) {
        List<CourseSchedule> result = new ArrayList<>();
        for (Slot slot : domain) {
            CourseSchedule candidate = schedule(target, slot, weekStart, weekEnd);
            if (compatible(candidate, target, existing, classes)
                    && compatible(candidate, target, assigned, classes)) {
                result.add(candidate);
            }
        }
        return result;
    }

    private boolean compatible(CourseSchedule candidate, TeachingClass target,
                               List<CourseSchedule> occupied,
                               Map<String, TeachingClass> classes) {
        for (CourseSchedule value : occupied) {
            if (!overlaps(candidate, value)) continue;
            TeachingClass owner = classes.get(value.getTeachingClassId());
            if (candidate.getTeachingClassId().equals(value.getTeachingClassId())
                    || candidate.getClassroom().equals(value.getClassroom())
                    || owner != null && target.getTeacher().equals(owner.getTeacher())) {
                return false;
            }
        }
        return true;
    }

    static boolean overlaps(CourseSchedule left, CourseSchedule right) {
        return left.getDayOfWeek() == right.getDayOfWeek()
                && Math.max(left.getWeekStart(), right.getWeekStart())
                <= Math.min(left.getWeekEnd(), right.getWeekEnd())
                && Math.max(left.getStartPeriod(), right.getStartPeriod())
                <= Math.min(left.getEndPeriod(), right.getEndPeriod());
    }

    private List<Slot> candidateDomain(List<CourseSchedule> schedules) {
        Set<TimePattern> times = new LinkedHashSet<>();
        Set<String> rooms = new LinkedHashSet<>();
        for (CourseSchedule value : schedules) {
            if (value.getClassroom() != null && !value.getClassroom().isBlank()) {
                rooms.add(value.getClassroom());
            }
            times.add(new TimePattern(value.getDayOfWeek(), value.getStartPeriod(),
                    value.getEndPeriod(), value.getStartTime(), value.getEndTime()));
        }
        if (rooms.isEmpty()) rooms.add("教一-101");
        if (times.isEmpty()) {
            times.add(new TimePattern(1, 1, 2, LocalTime.of(8, 0), LocalTime.of(9, 35)));
            times.add(new TimePattern(2, 3, 5, LocalTime.of(9, 50), LocalTime.of(12, 15)));
        }
        List<Slot> result = new ArrayList<>();
        for (TimePattern time : times) for (String room : rooms) result.add(new Slot(time, room));
        return result;
    }

    private CourseSchedule schedule(TeachingClass target, Slot slot,
                                    int weekStart, int weekEnd) {
        CourseSchedule value = new CourseSchedule();
        value.setScheduleId("AUTO" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        value.setTeachingClassId(target.getTeachingClassId());
        value.setCourseId(target.getCourseId());
        value.setWeekStart(weekStart);
        value.setWeekEnd(weekEnd);
        value.setDayOfWeek(slot.time().day());
        value.setStartPeriod(slot.time().startPeriod());
        value.setEndPeriod(slot.time().endPeriod());
        value.setStartTime(slot.time().startTime());
        value.setEndTime(slot.time().endTime());
        value.setClassroom(slot.room());
        return value;
    }

    private record TimePattern(int day, int startPeriod, int endPeriod,
                               LocalTime startTime, LocalTime endTime) { }
    private record Slot(TimePattern time, String room) { }
}
