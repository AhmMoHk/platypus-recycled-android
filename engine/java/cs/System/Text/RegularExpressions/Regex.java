package cs.System.Text.RegularExpressions;

/** .NET Regex over java.util.regex (RegexOptions: IgnoreCase 1, Multiline 2, Singleline 16) */
public class Regex {
    final java.util.regex.Pattern p;
    public Regex(String pattern) { this(pattern, 0); }
    public Regex(String pattern, int options) {
        int f = 0;
        if ((options & 1) != 0) f |= java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE;
        if ((options & 2) != 0) f |= java.util.regex.Pattern.MULTILINE;
        if ((options & 16) != 0) f |= java.util.regex.Pattern.DOTALL;
        p = java.util.regex.Pattern.compile(translate(pattern), f);
    }
    /** .NET named groups (?<name>...) are the same in Java; (?'name') is not supported by Java */
    static String translate(String s) { return s.replace("(?'", "(?<").replace("'>", ">"); }
    public boolean IsMatch(String s) { return p.matcher(s).find(); }
    public Match Match(String s) { java.util.regex.Matcher m = p.matcher(s); return new Match(m, m.find()); }
    public Match Match(String s, int start) { java.util.regex.Matcher m = p.matcher(s); return new Match(m, m.find(start)); }
    public String Replace(String s, String rep) { return p.matcher(s).replaceAll(rep.replace("\\", "\\\\").replaceAll("\\$\\{(\\w+)\\}", "\\${$1}")); }
    public String Replace(String s, MatchEvaluator ev) {
        java.util.regex.Matcher m = p.matcher(s);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (m.find()) {
            sb.append(s, last, m.start());
            sb.append(ev.Invoke(new Match(m, true)));
            last = m.end();
        }
        sb.append(s.substring(last));
        return sb.toString();
    }
    public String[] Split(String s) { return p.split(s, -1); }
    public static boolean IsMatch(String s, String pattern) { return new Regex(pattern).IsMatch(s); }
    public static boolean IsMatch(String s, String pattern, int o) { return new Regex(pattern, o).IsMatch(s); }
    public static Match Match(String s, String pattern) { return new Regex(pattern).Match(s); }
    public static String Replace(String s, String pattern, String rep) { return new Regex(pattern).Replace(s, rep); }
    public static String Replace(String s, String pattern, MatchEvaluator ev) { return new Regex(pattern).Replace(s, ev); }
    public static String Escape(String s) { return java.util.regex.Pattern.quote(s); }
    public static String[] Split(String s, String pattern) { return new Regex(pattern).Split(s); }
}
