package vcampus.common.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** A group of equivalent courses governed by one enrollment rule. */
public class CourseRequirementGroup implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String CHOOSE_ONE = "CHOOSE_ONE";

    private String _groupId;
    private String _groupName;
    private String _rule;
    private List<String> _courseIds = new ArrayList<>();

    public CourseRequirementGroup() {
    }

    public CourseRequirementGroup(String groupId, String groupName, String rule,
                                  List<String> courseIds) {
        _groupId = groupId;
        _groupName = groupName;
        _rule = rule;
        _courseIds = new ArrayList<>(courseIds);
    }

    public String getGroupId() { return _groupId; }
    public void setGroupId(String groupId) { _groupId = groupId; }
    public String getGroupName() { return _groupName; }
    public void setGroupName(String groupName) { _groupName = groupName; }
    public String getRule() { return _rule; }
    public void setRule(String rule) { _rule = rule; }
    public List<String> getCourseIds() { return _courseIds; }
    public void setCourseIds(List<String> courseIds) {
        _courseIds = courseIds == null ? new ArrayList<>() : new ArrayList<>(courseIds);
    }
}
