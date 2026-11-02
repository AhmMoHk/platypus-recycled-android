package cs.System.Reflection;

import java.util.ArrayList;
import java.util.HashMap;

/** The translated game is one "assembly"; types are listed in cs.rt.GameTypes. */
public class Assembly {
    private static Assembly game;
    private static cs.System.Type[] types;
    private static final HashMap<String, cs.System.Type> byName = new HashMap<>();

    public Assembly() {}

    public static synchronized Assembly game() {
        if (game == null) game = new Assembly();
        return game;
    }

    public synchronized cs.System.Type[] GetTypes() {
        if (types == null) {
            ArrayList<cs.System.Type> out = new ArrayList<>();
            for (String n : cs.rt.GameTypes.NAMES) {
                try {
                    out.add(cs.System.Type.of(Class.forName(n)));
                } catch (ClassNotFoundException | LinkageError ignored) {
                }
            }
            types = out.toArray(new cs.System.Type[0]);
        }
        return types.clone();
    }

    public String get_FullName() { return "Platypus"; }
    public static Assembly GetCallingAssembly() { return game(); }
    public static Object[] typeAttrs(cs.System.Type t) { return Meta.attrsOf(t.cls, ""); }
    public static Assembly GetExecutingAssembly() { return game(); }
    public static Assembly GetEntryAssembly() { return game(); }
    public static boolean op_Equality(Assembly a, Assembly b) { return a == b; }
    public static boolean op_Inequality(Assembly a, Assembly b) { return a != b; }
    public String[] GetManifestResourceNames() { return new String[0]; }
    public cs.System.IO.Stream GetManifestResourceStream(String n) { return null; }
    public String GetName() { return "Platypus"; }

    private static final String[][] PRIMS = {
        {"bool", "System.Boolean"}, {"char", "System.Char"}, {"i1", "System.SByte"}, {"u1", "System.Byte"},
        {"i2", "System.Int16"}, {"u2", "System.UInt16"}, {"i4", "System.Int32"}, {"u4", "System.UInt32"},
        {"i8", "System.Int64"}, {"u8", "System.UInt64"}, {"r4", "System.Single"}, {"r8", "System.Double"},
        {"i", "System.IntPtr"}, {"u", "System.UIntPtr"}, {"string", "System.String"}, {"object", "System.Object"},
    };

    private static cs.System.Type prim(String n) {
        switch (n) {
            case "bool": case "System.Boolean": return cs.System.Type.BOOLEAN;
            case "char": case "System.Char": return cs.System.Type.CHAR;
            case "i1": case "System.SByte": return cs.System.Type.SBYTE;
            case "u1": case "System.Byte": return cs.System.Type.BYTE;
            case "i2": case "System.Int16": return cs.System.Type.INT16;
            case "u2": case "System.UInt16": return cs.System.Type.UINT16;
            case "i4": case "System.Int32": return cs.System.Type.INT32;
            case "u4": case "System.UInt32": return cs.System.Type.UINT32;
            case "i8": case "System.Int64": return cs.System.Type.INT64;
            case "u8": case "System.UInt64": return cs.System.Type.UINT64;
            case "r4": case "System.Single": return cs.System.Type.SINGLE;
            case "r8": case "System.Double": return cs.System.Type.DOUBLE;
            case "i": case "System.IntPtr": return cs.System.Type.INTPTR;
            case "u": case "System.UIntPtr": return cs.System.Type.UINTPTR;
            case "string": case "System.String": return cs.System.Type.STRING;
            case "object": case "System.Object": return cs.System.Type.OBJECT;
            case "void": case "System.Void": return cs.System.Type.VOID;
        }
        return null;
    }

    /** Java class name of a .NET type name (same rules as the translator). */
    public static String javaName(String n) {
        if (n.equals("UnityEngine.Object")) return "cs.UnityEngine.UObject";
        String[] parts = n.split("/");
        String top = parts[0];
        int k = top.lastIndexOf('.');
        String ns = k < 0 ? "" : top.substring(0, k);
        StringBuilder sb = new StringBuilder(ns.isEmpty() ? "cs.global." : "cs." + ns + ".");
        sb.append(san(k < 0 ? top : top.substring(k + 1)));
        for (int i = 1; i < parts.length; i++) sb.append('$').append(san(parts[i]));
        return sb.toString();
    }

    private static String san(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isLetterOrDigit(c) && c < 128 || c == '_' || c == '$') sb.append(c);
            else if (c == '`') sb.append('_');
            else if (c == '<' || c == '>') sb.append('$');
            else sb.append('_');
        }
        return sb.toString();
    }

    /** Resolve a type from the translator's metadata notation: i4, string, Foo, A/B, List`1<i4>, T[], !0 */
    public static synchronized cs.System.Type resolveType(String s) {
        cs.System.Type t = byName.get(s);
        if (t != null) return t;
        t = parse(s);
        if (t != null) byName.put(s, t);
        return t;
    }

    private static cs.System.Type parse(String s) {
        s = s.trim();
        if (s.endsWith("[]")) {
            cs.System.Type e = parse(s.substring(0, s.length() - 2));
            return e == null ? null : cs.System.Type.arrayOf(e);
        }
        if (s.endsWith("&")) return parse(s.substring(0, s.length() - 1));
        if (s.startsWith("!")) return cs.System.Type.OBJECT;
        cs.System.Type p = prim(s);
        if (p != null) return p;
        int lt = s.indexOf('<');
        if (lt > 0 && s.endsWith(">")) {
            String base = s.substring(0, lt);
            ArrayList<String> args = new ArrayList<>();
            int depth = 0, start = lt + 1;
            for (int i = lt + 1; i < s.length() - 1; i++) {
                char c = s.charAt(i);
                if (c == '<') depth++;
                else if (c == '>') depth--;
                else if (c == ',' && depth == 0) { args.add(s.substring(start, i)); start = i + 1; }
            }
            args.add(s.substring(start, s.length() - 1));
            Class<?> bc = load(base);
            if (bc == null) return null;
            cs.System.Type[] ta = new cs.System.Type[args.size()];
            for (int i = 0; i < ta.length; i++) {
                ta[i] = parse(args.get(i));
                if (ta[i] == null) ta[i] = cs.System.Type.OBJECT;
            }
            return cs.System.Type.generic(bc, ta);
        }
        Class<?> c = load(s);
        return c == null ? null : cs.System.Type.of(c);
    }

    private static Class<?> load(String n) {
        try {
            return Class.forName(javaName(n));
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /** Type.GetType(string) */
    public cs.System.Type GetType(String name) {
        return resolveType(name.replace('+', '/'));
    }
}
