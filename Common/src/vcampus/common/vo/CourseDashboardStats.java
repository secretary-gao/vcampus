package vcampus.common.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Aggregate snapshot for the administrator Course dashboard. */
public class CourseDashboardStats implements Serializable {
    private static final long serialVersionUID = 1L;
    private int _courseCount;
    private int _teachingClassCount;
    private int _studentCount;
    private int _selectionCount;
    private double _averageCapacity;
    private double _averageSelected;
    private int _fullClassCount;
    private double _capacityUtilization;
    private List<TeachingClassStatistic> _popularClasses = new ArrayList<>();
    private List<TeachingClassStatistic> _availableClasses = new ArrayList<>();
    private List<TeachingClassStatistic> _allClasses = new ArrayList<>();

    public int getCourseCount() { return _courseCount; }
    public void setCourseCount(int v) { _courseCount = v; }
    public int getTeachingClassCount() { return _teachingClassCount; }
    public void setTeachingClassCount(int v) { _teachingClassCount = v; }
    public int getStudentCount() { return _studentCount; }
    public void setStudentCount(int v) { _studentCount = v; }
    public int getSelectionCount() { return _selectionCount; }
    public void setSelectionCount(int v) { _selectionCount = v; }
    public double getAverageCapacity() { return _averageCapacity; }
    public void setAverageCapacity(double v) { _averageCapacity = v; }
    public double getAverageSelected() { return _averageSelected; }
    public void setAverageSelected(double v) { _averageSelected = v; }
    public int getFullClassCount() { return _fullClassCount; }
    public void setFullClassCount(int v) { _fullClassCount = v; }
    public double getCapacityUtilization() { return _capacityUtilization; }
    public void setCapacityUtilization(double v) { _capacityUtilization = v; }
    public List<TeachingClassStatistic> getPopularClasses() { return _popularClasses; }
    public void setPopularClasses(List<TeachingClassStatistic> v) { _popularClasses = new ArrayList<>(v); }
    public List<TeachingClassStatistic> getAvailableClasses() { return _availableClasses; }
    public void setAvailableClasses(List<TeachingClassStatistic> v) { _availableClasses = new ArrayList<>(v); }
    public List<TeachingClassStatistic> getAllClasses() { return _allClasses; }
    public void setAllClasses(List<TeachingClassStatistic> v) { _allClasses = new ArrayList<>(v); }
}
