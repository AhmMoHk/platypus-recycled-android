package cs.System.Text.RegularExpressions;

public class GroupCollection {
    final Group[] groups;
    GroupCollection(Group[] g) { groups = g; }
    public int get_Count() { return groups.length; }
    public Group get_Item(int i) { return i >= 0 && i < groups.length ? groups[i] : new Group(0, 0, "", false); }
}
