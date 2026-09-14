package vcampus.common.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Request for previewing schedules for currently unscheduled teaching classes. */
public class AutoScheduleRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> _teachingClassIds = new ArrayList<>();
    private int _weekStart = 1;
    private int _weekEnd = 16;

    public AutoScheduleRequest() { }
    public AutoScheduleRequest(List<String> teachingClassIds, int weekStart, int weekEnd) {
        _teachingClassIds = new ArrayList<>(teachingClassIds);
        _weekStart = weekStart;
        _weekEnd = weekEnd;
    }
    public List<String> getTeachingClassIds() { return _teachingClassIds; }
    public void setTeachingClassIds(List<String> value) {
        _teachingClassIds = value == null ? new ArrayList<>() : new ArrayList<>(value);
    }
    public int getWeekStart() { return _weekStart; }
    public void setWeekStart(int value) { _weekStart = value; }
    public int getWeekEnd() { return _weekEnd; }
    public void setWeekEnd(int value) { _weekEnd = value; }
}
