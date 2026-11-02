package cs.System;

public class Uri {
    private final String s;
    public Uri(String s) { this.s = s; }
    public String get_AbsoluteUri() { return s.startsWith("/") ? "file://" + s : s; }
    public String get_LocalPath() { return s.startsWith("file://") ? s.substring(7) : s; }
    public String get_AbsolutePath() { return get_LocalPath(); }
    public static boolean IsWellFormedUriString(String s, int kind) { return s != null && s.contains("://"); }
    @Override public String toString() { return s; }
}
