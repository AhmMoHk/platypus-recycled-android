package cs.System.IO;

import java.nio.charset.Charset;

public final class File {
    private File() {}
    static final Charset UTF8 = Charset.forName("UTF-8");

    static java.io.File f(String p) { return new java.io.File(Path.norm(p)); }

    public static boolean Exists(String p) { return p != null && f(p).isFile(); }

    public static byte[] ReadAllBytes(String p) {
        java.io.File file = f(p);
        if (!file.isFile()) throw new FileNotFoundException("Could not find file '" + p + "'");
        try {
            java.io.FileInputStream in = new java.io.FileInputStream(file);
            try {
                byte[] d = new byte[(int) file.length()];
                int n = 0;
                while (n < d.length) { int r = in.read(d, n, d.length - n); if (r < 0) break; n += r; }
                return n == d.length ? d : java.util.Arrays.copyOf(d, n);
            } finally { in.close(); }
        } catch (java.io.IOException e) {
            throw new IOException(e.toString());
        }
    }

    /** text with BOM handling like .NET's StreamReader */
    public static String ReadAllText(String p) { return decode(ReadAllBytes(p)); }
    public static String ReadAllText(String p, cs.System.Text.Encoding e) { return decode(ReadAllBytes(p)); }

    static String decode(byte[] d) {
        if (d.length >= 3 && (d[0] & 255) == 0xef && (d[1] & 255) == 0xbb && (d[2] & 255) == 0xbf) return new String(d, 3, d.length - 3, UTF8);
        if (d.length >= 2 && (d[0] & 255) == 0xff && (d[1] & 255) == 0xfe) return new String(d, 2, d.length - 2, Charset.forName("UTF-16LE"));
        if (d.length >= 2 && (d[0] & 255) == 0xfe && (d[1] & 255) == 0xff) return new String(d, 2, d.length - 2, Charset.forName("UTF-16BE"));
        return new String(d, UTF8);
    }

    public static String[] ReadAllLines(String p) {
        String t = ReadAllText(p);
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        int start = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '\n' || c == '\r') {
                lines.add(t.substring(start, i));
                if (c == '\r' && i + 1 < t.length() && t.charAt(i + 1) == '\n') i++;
                start = i + 1;
            }
        }
        if (start < t.length()) lines.add(t.substring(start));
        return lines.toArray(new String[0]);
    }

    public static void WriteAllBytes(String p, byte[] d) {
        try {
            java.io.File file = f(p);
            java.io.FileOutputStream out = new java.io.FileOutputStream(file);
            try { out.write(d); } finally { out.close(); }
        } catch (java.io.IOException e) {
            throw new IOException(e.toString());
        }
    }
    public static void WriteAllText(String p, String s) { WriteAllBytes(p, s.getBytes(UTF8)); }
    public static void WriteAllText(String p, String s, cs.System.Text.Encoding e) { WriteAllBytes(p, s.getBytes(UTF8)); }
    public static void WriteAllLines(String p, String[] lines) {
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(l).append('\n');
        WriteAllText(p, sb.toString());
    }
    public static void AppendAllText(String p, String s) {
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(f(p), true);
            try { out.write(s.getBytes(UTF8)); } finally { out.close(); }
        } catch (java.io.IOException e) { throw new IOException(e.toString()); }
    }
    public static void Delete(String p) { f(p).delete(); }
    public static void Move(String a, String b) {
        if (!f(a).renameTo(f(b))) { Copy(a, b, true); Delete(a); }
    }
    public static void Copy(String a, String b) { Copy(a, b, false); }
    public static void Copy(String a, String b, boolean overwrite) {
        if (!overwrite && Exists(b)) throw new IOException("The file '" + b + "' already exists.");
        WriteAllBytes(b, ReadAllBytes(a));
    }
    public static StreamReader OpenText(String p) { return new StreamReader(ReadAllText(p)); }
    public static FileStream OpenRead(String p) { return new FileStream(p, 3, 1, 1); }
    public static FileStream Create(String p) { return new FileStream(p, 2, 2, 0); }
    public static StreamWriter CreateText(String p) { return new StreamWriter(new FileStream(p, 2, 2, 0)); }
    public static cs.System.DateTime GetLastWriteTime(String p) { return cs.System.DateTime.fromMillis(f(p).lastModified()); }
    public static cs.System.DateTime GetLastWriteTimeUtc(String p) { return cs.System.DateTime.fromMillis(f(p).lastModified()); }
}
