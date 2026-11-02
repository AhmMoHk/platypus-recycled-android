package cs.System.Text.RegularExpressions;

public class Group extends Capture {
    final boolean success;
    Group(int i, int l, String v, boolean ok) { super(i, l, v); success = ok; }
    public boolean get_Success() { return success; }
}
