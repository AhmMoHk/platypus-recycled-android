package cs.System.IO;

import java.util.ArrayList;

public final class Directory {
    private Directory() {}

    public static boolean Exists(String p) { return p != null && File.f(p).isDirectory(); }
    public static DirectoryInfo CreateDirectory(String p) { File.f(p).mkdirs(); return new DirectoryInfo(p); }
    public static void Delete(String p) { File.f(p).delete(); }
    public static void Delete(String p, boolean rec) { del(File.f(p)); }
    private static void del(java.io.File f) {
        java.io.File[] k = f.listFiles();
        if (k != null) for (java.io.File c : k) del(c);
        f.delete();
    }

    private static String join(String dir, String name) {
        return dir.endsWith("/") || dir.endsWith("\\") ? dir + name : dir + "/" + name;
    }

    private static java.io.File[] sorted(java.io.File d) {
        java.io.File[] k = d.listFiles();
        if (k == null) throw new DirectoryNotFoundException("Could not find a part of the path '" + d + "'.");
        java.util.Arrays.sort(k, new java.util.Comparator<java.io.File>() {
            public int compare(java.io.File a, java.io.File b) { return a.getName().compareTo(b.getName()); }
        });
        return k;
    }

    public static String[] GetDirectories(String p) {
        ArrayList<String> out = new ArrayList<>();
        for (java.io.File c : sorted(File.f(p))) if (c.isDirectory()) out.add(join(p, c.getName()));
        return out.toArray(new String[0]);
    }
    public static String[] GetDirectories(String p, String pattern) {
        ArrayList<String> out = new ArrayList<>();
        for (java.io.File c : sorted(File.f(p))) if (c.isDirectory() && match(c.getName(), pattern)) out.add(join(p, c.getName()));
        return out.toArray(new String[0]);
    }
    public static String[] GetFiles(String p) { return GetFiles(p, "*", 0); }
    public static String[] GetFiles(String p, String pattern) { return GetFiles(p, pattern, 0); }
    /** SearchOption: 0 top only, 1 all directories */
    public static String[] GetFiles(String p, String pattern, int opt) {
        ArrayList<String> out = new ArrayList<>();
        collect(p, pattern, opt == 1, out);
        return out.toArray(new String[0]);
    }
    private static void collect(String p, String pattern, boolean rec, ArrayList<String> out) {
        java.io.File[] k = sorted(File.f(p));
        for (java.io.File c : k) if (c.isFile() && match(c.getName(), pattern)) out.add(join(p, c.getName()));
        if (rec) for (java.io.File c : k) if (c.isDirectory()) collect(join(p, c.getName()), pattern, true, out);
    }
    public static String[] GetFileSystemEntries(String p) {
        ArrayList<String> out = new ArrayList<>();
        for (java.io.File c : sorted(File.f(p))) out.add(join(p, c.getName()));
        return out.toArray(new String[0]);
    }

    /** .NET wildcard: * and ?, "*.ext" also matches longer extensions starting with ext (3-char rule) */
    static boolean match(String name, String pattern) {
        if (pattern == null || pattern.equals("*") || pattern.equals("*.*")) return true;
        StringBuilder re = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            if (c == '*') re.append(".*");
            else if (c == '?') re.append('.');
            else re.append(java.util.regex.Pattern.quote(String.valueOf(c)));
        }
        return java.util.regex.Pattern.compile(re.toString(), java.util.regex.Pattern.CASE_INSENSITIVE).matcher(name).matches();
    }
}
