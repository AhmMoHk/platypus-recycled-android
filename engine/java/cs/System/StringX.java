package cs.System;

import java.util.ArrayList;

import cs.rt.Fmt;

/** System.String methods (strings are java.lang.String). StringComparison: 0/1 culture, 2/3 invariant, 4 ordinal, 5 ordinal ignore case. */
public final class StringX {
    private StringX() {}

    public static final String Empty = "";

    private static NullReferenceException npe() {
        return new NullReferenceException("Object reference not set to an instance of an object.");
    }

    // ------------------------------------------------------------------ construction
    public static String $new(char[] cs) { return new String(cs); }
    public static String $new(char[] cs, int start, int len) { return new String(cs, start, len); }
    public static String $new(char c, int count) {
        char[] a = new char[count];
        java.util.Arrays.fill(a, c);
        return new String(a);
    }

    // ------------------------------------------------------------------ properties
    public static int get_Length(String s) { if (s == null) throw npe(); return s.length(); }
    public static char get_Chars(String s, int i) {
        if (i < 0 || i >= s.length()) throw new IndexOutOfRangeException("Index was outside the bounds of the array.");
        return s.charAt(i);
    }

    // ------------------------------------------------------------------ comparison
    public static boolean IsNullOrEmpty(String s) { return s == null || s.isEmpty(); }
    public static boolean IsNullOrWhiteSpace(String s) { return s == null || s.trim().isEmpty() && allWhite(s); }
    private static boolean allWhite(String s) {
        for (int i = 0; i < s.length(); i++) if (!Character.isWhitespace(s.charAt(i)) && !Character.isSpaceChar(s.charAt(i))) return false;
        return true;
    }

    public static boolean op_Equality(String a, String b) { return a == null ? b == null : a.equals(b); }
    public static boolean op_Inequality(String a, String b) { return !op_Equality(a, b); }
    public static boolean Equals(String a, String b) { return op_Equality(a, b); }
    public static boolean Equals(String a, Object b) { return b instanceof String && a.equals(b); }
    public static boolean EqualsCmp(String a, String b, int cmp) {
        if (a == null || b == null) return a == b;
        return (cmp == 1 || cmp == 3 || cmp == 5) ? a.equalsIgnoreCase(b) : a.equals(b);
    }
    public static int GetHashCode(String s) { return s.hashCode(); }

    /** Culture-style comparison (invariant culture approximation): case-insensitive first, lower before upper. */
    public static int Compare(String a, String b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        int n = java.lang.Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            char x = a.charAt(i), y = b.charAt(i);
            if (x == y) continue;
            int wx = weight(x), wy = weight(y);
            if (wx != wy) return wx < wy ? -1 : 1;
        }
        if (a.length() != b.length()) return a.length() < b.length() ? -1 : 1;
        for (int i = 0; i < n; i++) {
            char x = a.charAt(i), y = b.charAt(i);
            if (x != y) {
                boolean lx = Character.isLowerCase(x), ly = Character.isLowerCase(y);
                if (lx != ly) return lx ? -1 : 1;
                return x < y ? -1 : 1;
            }
        }
        return 0;
    }

    /** Primary sort weight: punctuation/symbols < digits < letters (case folded). */
    private static int weight(char c) {
        if (Character.isLetter(c)) return 0x20000 + Character.toLowerCase(c);
        if (Character.isDigit(c)) return 0x10000 + c;
        return c;
    }

    public static int Compare(String a, String b, boolean ignoreCase) {
        return ignoreCase ? Compare(a == null ? null : a.toLowerCase(), b == null ? null : b.toLowerCase()) : Compare(a, b);
    }

    public static int CompareCmp(String a, String b, int cmp) {
        if (cmp == 4) return CompareOrdinal(a, b);
        if (cmp == 5) return CompareOrdinal(a == null ? null : a.toUpperCase(), b == null ? null : b.toUpperCase());
        return Compare(a, b, cmp == 1 || cmp == 3);
    }

    public static int CompareOrdinal(String a, String b) {
        if (a == b) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        int c = a.compareTo(b);
        return Integer.signum(c);
    }

    public static int CompareTo(String a, String b) { if (a == null) throw npe(); return Compare(a, b); }
    public static int CompareTo(String a, Object b) { return CompareTo(a, (String) b); }

    // ------------------------------------------------------------------ searching
    public static boolean Contains(String s, String v) { if (v == null) throw new ArgumentNullException("value"); return s.contains(v); }
    public static boolean Contains(String s, char c) { return s.indexOf(c) >= 0; }
    public static boolean StartsWith(String s, String v) { if (v == null) throw new ArgumentNullException("value"); return s.startsWith(v); }
    public static boolean StartsWithCmp(String s, String v, int cmp) {
        if (cmp == 1 || cmp == 3 || cmp == 5) return s.regionMatches(true, 0, v, 0, v.length());
        return s.startsWith(v);
    }
    public static boolean StartsWith(String s, char c) { return !s.isEmpty() && s.charAt(0) == c; }
    public static boolean EndsWith(String s, String v) { if (v == null) throw new ArgumentNullException("value"); return s.endsWith(v); }
    public static boolean EndsWithCmp(String s, String v, int cmp) {
        if (cmp == 1 || cmp == 3 || cmp == 5) return s.length() >= v.length() && s.regionMatches(true, s.length() - v.length(), v, 0, v.length());
        return s.endsWith(v);
    }
    public static boolean EndsWith(String s, char c) { return !s.isEmpty() && s.charAt(s.length() - 1) == c; }

    public static int IndexOf(String s, String v) { return s.indexOf(v); }
    public static int IndexOf(String s, String v, int start) { return s.indexOf(v, start); }
    public static int IndexOf(String s, String v, int start, int count) {
        int r = s.substring(0, java.lang.Math.min(s.length(), start + count)).indexOf(v, start);
        return r;
    }
    public static int IndexOfCmp(String s, String v, int start, int count, int cmp) { return IndexOf(s, v, start, count); }
    /** IndexOf(string, StringComparison) and IndexOf(string, startIndex) share a java signature: a
     *  StringComparison value is small (0..5) and ambiguous with a start index; treated as start index
     *  except when the comparison is a case-insensitive one and the start index would be invalid. */
    public static int IndexOfCmp(String s, String v, int cmp) {
        if (cmp == 1 || cmp == 3 || cmp == 5) return s.toLowerCase().indexOf(v.toLowerCase());
        return s.indexOf(v);
    }
    public static int IndexOf(String s, char c) { return s.indexOf(c); }
    public static int IndexOf(String s, char c, int start) {
        if (start < 0 || start > s.length()) throw new ArgumentOutOfRangeException("startIndex");
        return s.indexOf(c, start);
    }
    public static int IndexOf(String s, char c, int start, int count) {
        int end = java.lang.Math.min(s.length(), start + count);
        for (int i = start; i < end; i++) if (s.charAt(i) == c) return i;
        return -1;
    }
    public static int IndexOfAny(String s, char[] any) {
        for (int i = 0; i < s.length(); i++) for (char c : any) if (s.charAt(i) == c) return i;
        return -1;
    }
    public static int IndexOfAny(String s, char[] any, int start) {
        for (int i = start; i < s.length(); i++) for (char c : any) if (s.charAt(i) == c) return i;
        return -1;
    }
    public static int LastIndexOf(String s, char c) { return s.lastIndexOf(c); }
    public static int LastIndexOf(String s, String v) { return s.lastIndexOf(v); }
    public static int LastIndexOf(String s, char c, int start) { return s.lastIndexOf(c, start); }
    public static int LastIndexOfAny(String s, char[] any, int start) {
        for (int i = start; i >= 0; i--) for (char c : any) if (s.charAt(i) == c) return i;
        return -1;
    }
    public static int LastIndexOfAny(String s, char[] any) {
        for (int i = s.length() - 1; i >= 0; i--) for (char c : any) if (s.charAt(i) == c) return i;
        return -1;
    }

    // ------------------------------------------------------------------ transformation
    public static String Substring(String s, int start) {
        if (start < 0 || start > s.length()) throw new ArgumentOutOfRangeException("startIndex", "startIndex cannot be larger than length of string.");
        return s.substring(start);
    }
    public static String Substring(String s, int start, int len) {
        if (start < 0 || len < 0 || start + len > s.length()) throw new ArgumentOutOfRangeException("length", "Index and length must refer to a location within the string.");
        return s.substring(start, start + len);
    }
    public static String Remove(String s, int start) { return s.substring(0, start); }
    public static String Remove(String s, int start, int count) { return s.substring(0, start) + s.substring(start + count); }
    public static String Insert(String s, int i, String v) { return s.substring(0, i) + v + s.substring(i); }
    public static String Replace(String s, String a, String b) {
        if (a == null) throw new ArgumentNullException("oldValue");
        if (a.isEmpty()) throw new ArgumentException("String cannot be of zero length.", "oldValue");
        return s.replace(a, b == null ? "" : b);
    }
    public static String Replace(String s, char a, char b) { return s.replace(a, b); }
    public static String ToLower(String s) { return s.toLowerCase(java.util.Locale.ROOT); }
    public static String ToLowerInvariant(String s) { return s.toLowerCase(java.util.Locale.ROOT); }
    public static String ToUpper(String s) { return s.toUpperCase(java.util.Locale.ROOT); }
    public static String ToUpperInvariant(String s) { return s.toUpperCase(java.util.Locale.ROOT); }
    public static String ToString(String s) { return s; }
    public static String ToString(String s, IFormatProvider p) { return s; }
    public static char[] ToCharArray(String s) { return s.toCharArray(); }
    public static char[] ToCharArray(String s, int start, int len) { return s.substring(start, start + len).toCharArray(); }
    public static Object Clone(String s) { return s; }

    private static boolean isWs(char c) { return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\u0085'; }
    public static String Trim(String s) {
        int a = 0, b = s.length();
        while (a < b && isWs(s.charAt(a))) a++;
        while (b > a && isWs(s.charAt(b - 1))) b--;
        return s.substring(a, b);
    }
    public static String Trim(String s, char[] cs) {
        if (cs == null || cs.length == 0) return Trim(s);
        return TrimEnd(TrimStart(s, cs), cs);
    }
    public static String Trim(String s, char c) { return Trim(s, new char[]{c}); }
    public static String TrimStart(String s) { int a = 0; while (a < s.length() && isWs(s.charAt(a))) a++; return s.substring(a); }
    public static String TrimEnd(String s) { int b = s.length(); while (b > 0 && isWs(s.charAt(b - 1))) b--; return s.substring(0, b); }
    public static String TrimStart(String s, char[] cs) {
        if (cs == null || cs.length == 0) return TrimStart(s);
        int a = 0;
        while (a < s.length() && in(s.charAt(a), cs)) a++;
        return s.substring(a);
    }
    public static String TrimEnd(String s, char[] cs) {
        if (cs == null || cs.length == 0) return TrimEnd(s);
        int b = s.length();
        while (b > 0 && in(s.charAt(b - 1), cs)) b--;
        return s.substring(0, b);
    }
    public static String TrimStart(String s, char c) { return TrimStart(s, new char[]{c}); }
    public static String TrimEnd(String s, char c) { return TrimEnd(s, new char[]{c}); }
    private static boolean in(char c, char[] cs) { for (char x : cs) if (x == c) return true; return false; }

    public static String PadLeft(String s, int n) { return PadLeft(s, n, ' '); }
    public static String PadLeft(String s, int n, char c) {
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < n; i++) sb.append(c);
        return sb.append(s).toString();
    }
    public static String PadRight(String s, int n) { return PadRight(s, n, ' '); }
    public static String PadRight(String s, int n, char c) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(c);
        return sb.toString();
    }

    // ------------------------------------------------------------------ split / join / concat / format
    /** StringSplitOptions: 1 RemoveEmptyEntries, 2 TrimEntries */
    private static String[] finish(ArrayList<String> parts, int opt) {
        ArrayList<String> out = new ArrayList<>();
        for (String p : parts) {
            if ((opt & 2) != 0) p = Trim(p);
            if ((opt & 1) != 0 && p.isEmpty()) continue;
            out.add(p);
        }
        return out.toArray(new String[0]);
    }

    public static String[] Split(String s, char[] seps) { return SplitOpt(s, seps, Integer.MAX_VALUE, 0); }
    public static String[] SplitOpt(String s, char[] seps, int opt) { return SplitOpt(s, seps, Integer.MAX_VALUE, opt); }
    public static String[] Split(String s, char[] seps, int count) { return SplitOpt(s, seps, count, 0); }
    public static String[] SplitOpt(String s, char[] seps, int count, int opt) {
        ArrayList<String> parts = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < s.length() && parts.size() < count - 1; i++) {
            char c = s.charAt(i);
            boolean sep = (seps == null || seps.length == 0) ? isWs(c) : in(c, seps);
            if (sep) {
                parts.add(s.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(s.substring(start));
        return finish(parts, opt);
    }
    public static String[] SplitOpt(String s, char sep, int opt) { return SplitOpt(s, new char[]{sep}, Integer.MAX_VALUE, opt); }
    public static String[] Split(String s, char sep) { return SplitOpt(s, new char[]{sep}, Integer.MAX_VALUE, 0); }
    public static String[] SplitOpt(String s, char sep, int count, int opt) { return SplitOpt(s, new char[]{sep}, count, opt); }
    public static String[] SplitOpt(String s, String sep, int opt) { return SplitOpt(s, new String[]{sep}, Integer.MAX_VALUE, opt); }
    public static String[] SplitOpt(String s, String sep, int count, int opt) { return SplitOpt(s, new String[]{sep}, count, opt); }
    public static String[] SplitOpt(String s, String[] seps, int opt) { return SplitOpt(s, seps, Integer.MAX_VALUE, opt); }
    public static String[] SplitOpt(String s, String[] seps, int count, int opt) {
        ArrayList<String> parts = new ArrayList<>();
        int start = 0, i = 0;
        outer:
        while (i < s.length() && parts.size() < count - 1) {
            for (String sp : seps) {
                if (sp == null || sp.isEmpty()) continue;
                if (s.startsWith(sp, i)) {
                    parts.add(s.substring(start, i));
                    i += sp.length();
                    start = i;
                    continue outer;
                }
            }
            i++;
        }
        parts.add(s.substring(start));
        return finish(parts, opt);
    }

    public static String Join(String sep, String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(sep);
            if (parts[i] != null) sb.append(parts[i]);
        }
        return sb.toString();
    }
    public static String Join(String sep, Object[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(sep);
            if (parts[i] != null) sb.append(ObjectX.ToString(parts[i]));
        }
        return sb.toString();
    }
    public static String Join(String sep, cs.System.Collections.Generic.IEnumerable_1 parts) {
        StringBuilder sb = new StringBuilder();
        cs.System.Collections.Generic.IEnumerator_1 e = parts.GetEnumerator();
        boolean first = true;
        while (e.MoveNext()) {
            if (!first) sb.append(sep);
            first = false;
            Object o = e.get_Current();
            if (o != null) sb.append(ObjectX.ToString(o));
        }
        return sb.toString();
    }
    public static String Join(cs.System.Type t, String sep, cs.System.Collections.Generic.IEnumerable_1 parts) { return Join(sep, parts); }
    public static String Join(char sep, String[] parts) { return Join(String.valueOf(sep), parts); }

    public static String Concat(String a, String b) { return (a == null ? "" : a) + (b == null ? "" : b); }
    public static String Concat(String a, String b, String c) { return Concat(Concat(a, b), c); }
    public static String Concat(String a, String b, String c, String d) { return Concat(Concat(a, b), Concat(c, d)); }
    public static String Concat(String[] ps) {
        StringBuilder sb = new StringBuilder();
        for (String p : ps) if (p != null) sb.append(p);
        return sb.toString();
    }
    public static String Concat(Object a) { return ObjectX.str(a); }
    public static String Concat(Object a, Object b) { return ObjectX.str(a) + ObjectX.str(b); }
    public static String Concat(Object a, Object b, Object c) { return ObjectX.str(a) + ObjectX.str(b) + ObjectX.str(c); }
    public static String Concat(Object[] ps) {
        StringBuilder sb = new StringBuilder();
        for (Object p : ps) sb.append(ObjectX.str(p));
        return sb.toString();
    }
    public static String Concat(cs.System.Type t, cs.System.Collections.Generic.IEnumerable_1 ps) { return Join("", ps); }

    public static String Format(String f, Object a) { return Fmt.compose(f, new Object[]{a}); }
    public static String Format(String f, Object a, Object b) { return Fmt.compose(f, new Object[]{a, b}); }
    public static String Format(String f, Object a, Object b, Object c) { return Fmt.compose(f, new Object[]{a, b, c}); }
    public static String Format(String f, Object[] args) { return Fmt.compose(f, args); }
    public static String Format(IFormatProvider p, String f, Object a) { return Fmt.compose(f, new Object[]{a}); }
    public static String Format(IFormatProvider p, String f, Object a, Object b) { return Fmt.compose(f, new Object[]{a, b}); }
    public static String Format(IFormatProvider p, String f, Object[] args) { return Fmt.compose(f, args); }

    public static cs.System.Collections.Generic.IEnumerator_1 GetEnumerator(String s) {
        cs.System.Collections.Generic.List_1 l = new cs.System.Collections.Generic.List_1();
        for (char c : s.toCharArray()) l.Add(c);
        return l.GetEnumerator();
    }

    public static String Normalize(String s) { return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFC); }
    public static String Intern(String s) { return s.intern(); }
    public static String Copy(String s) { return new String(s); }
}
