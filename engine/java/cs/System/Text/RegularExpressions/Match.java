package cs.System.Text.RegularExpressions;

public class Match extends Group {
    final GroupCollection groups;
    final java.util.regex.Matcher m;
    Match(java.util.regex.Matcher m, boolean ok) {
        super(ok ? m.start() : 0, ok ? m.end() - m.start() : 0, ok ? m.group() : "", ok);
        this.m = m;
        Group[] g = new Group[ok ? m.groupCount() + 1 : 1];
        if (ok) for (int i = 0; i < g.length; i++) {
            boolean s = m.start(i) >= 0;
            g[i] = new Group(s ? m.start(i) : 0, s ? m.end(i) - m.start(i) : 0, s ? m.group(i) : "", s);
        } else g[0] = this;
        groups = new GroupCollection(g);
    }
    public GroupCollection get_Groups() { return groups; }
    public Match NextMatch() {
        if (!success) return this;
        boolean ok = m.find();
        return new Match(m, ok);
    }
    public static Match get_Empty() { return new Match(java.util.regex.Pattern.compile("$^").matcher(""), false); }
}
