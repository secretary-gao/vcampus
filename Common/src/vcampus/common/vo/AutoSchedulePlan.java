package vcampus.common.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Serializable preview result produced by the automatic scheduler. */
public class AutoSchedulePlan implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<CourseSchedule> _assignments = new ArrayList<>();
    private List<String> _unassignedTeachingClassIds = new ArrayList<>();
    private int _teachingClassCount;
    private int _candidateSlotCount;
    private long _searchNodes;
    private long _backtracks;
    private long _elapsedMillis;

    public List<CourseSchedule> getAssignments() { return _assignments; }
    public void setAssignments(List<CourseSchedule> value) { _assignments = new ArrayList<>(value); }
    public List<String> getUnassignedTeachingClassIds() { return _unassignedTeachingClassIds; }
    public void setUnassignedTeachingClassIds(List<String> value) {
        _unassignedTeachingClassIds = new ArrayList<>(value);
    }
    public int getTeachingClassCount() { return _teachingClassCount; }
    public void setTeachingClassCount(int value) { _teachingClassCount = value; }
    public int getCandidateSlotCount() { return _candidateSlotCount; }
    public void setCandidateSlotCount(int value) { _candidateSlotCount = value; }
    public long getSearchNodes() { return _searchNodes; }
    public void setSearchNodes(long value) { _searchNodes = value; }
    public long getBacktracks() { return _backtracks; }
    public void setBacktracks(long value) { _backtracks = value; }
    public long getElapsedMillis() { return _elapsedMillis; }
    public void setElapsedMillis(long value) { _elapsedMillis = value; }
}
