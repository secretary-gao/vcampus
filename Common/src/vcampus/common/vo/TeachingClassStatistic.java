package vcampus.common.vo;

import java.io.Serializable;

/** One teaching class row used by academic analytics and CSV export. */
public record TeachingClassStatistic(String teachingClassId, String courseName,
                                     String teacher, int capacity, int selectedCount)
        implements Serializable {
    private static final long serialVersionUID = 1L;
    public int remaining() { return capacity - selectedCount; }
    public double utilization() {
        return capacity == 0 ? 0.0 : (double) selectedCount / capacity;
    }
}
