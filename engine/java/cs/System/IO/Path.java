package cs.System.IO;

public final class Path {
    private Path() {}
    public static char DirectorySeparatorChar = '/';
    public static char AltDirectorySeparatorChar = '\\';
    public static char PathSeparator = ':';

    static String norm(String p) { return p == null ? null : p.replace('\\', '/'); }

    public static boolean IsPathRooted(String p) { return p != null && (p.startsWith("/") || p.startsWith("\\") || (p.length() > 1 && p.charAt(1) == ':')); }

    public static String Combine(String a, String b) {
        if (a == null || b == null) throw new cs.System.ArgumentNullException(a == null ? "path1" : "path2");
        if (b.isEmpty()) return a;
        if (a.isEmpty() || IsPathRooted(b)) return b;
        char c = a.charAt(a.length() - 1);
        return (c == '/' || c == '\\') ? a + b : a + "/" + b;
    }
    public static String Combine(String a, String b, String c) { return Combine(Combine(a, b), c); }
    public static String Combine(String a, String b, String c, String d) { return Combine(Combine(Combine(a, b), c), d); }
    public static String Combine(String[] ps) { String r = ""; for (String p : ps) r = Combine(r, p); return r; }

    public static String GetFileName(String p) {
        if (p == null) return null;
        int i = java.lang.Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
        return p.substring(i + 1);
    }
    public static String GetFileNameWithoutExtension(String p) {
        String f = GetFileName(p);
        if (f == null) return null;
        int d = f.lastIndexOf('.');
        return d < 0 ? f : f.substring(0, d);
    }
    public static String GetExtension(String p) {
        String f = GetFileName(p);
        if (f == null) return null;
        int d = f.lastIndexOf('.');
        return d < 0 || d == f.length() - 1 ? "" : f.substring(d);
    }
    public static boolean HasExtension(String p) { return !GetExtension(p).isEmpty(); }
    public static String ChangeExtension(String p, String ext) {
        int s = java.lang.Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
        int d = p.lastIndexOf('.');
        String base = d > s ? p.substring(0, d) : p;
        if (ext == null) return base;
        return base + (ext.startsWith(".") ? ext : "." + ext);
    }
    public static String GetDirectoryName(String p) {
        if (p == null) return null;
        int i = java.lang.Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
        if (i < 0) return "";
        if (i == 0) return null;
        return p.substring(0, i);
    }
    public static String GetFullPath(String p) {
        try { return new java.io.File(norm(p)).getCanonicalPath(); } catch (java.io.IOException e) { return new java.io.File(norm(p)).getAbsolutePath(); }
    }
    public static String GetRelativePath(String rel, String p) {
        String a = norm(GetFullPath(rel)), b = norm(GetFullPath(p));
        if (!a.endsWith("/")) a += "/";
        if (b.startsWith(a)) return b.substring(a.length());
        if ((b + "/").equals(a)) return ".";
        // walk up
        String[] x = a.split("/"), y = b.split("/");
        int k = 0;
        while (k < x.length && k < y.length && x[k].equals(y[k])) k++;
        StringBuilder sb = new StringBuilder();
        for (int i = k; i < x.length; i++) if (!x[i].isEmpty()) sb.append("../");
        for (int i = k; i < y.length; i++) { sb.append(y[i]); if (i < y.length - 1) sb.append('/'); }
        return sb.toString();
    }
    public static String GetTempPath() { return System.getProperty("java.io.tmpdir") + "/"; }
    public static String GetTempFileName() {
        try { return java.io.File.createTempFile("tmp", ".tmp").getPath(); } catch (java.io.IOException e) { throw new IOException(e.toString()); }
    }
    public static char[] GetInvalidFileNameChars() { return new char[]{'"', '<', '>', '|', '\0', ':', '*', '?', '\\', '/'}; }
    public static char[] GetInvalidPathChars() { return new char[]{'"', '<', '>', '|', '\0'}; }
}
