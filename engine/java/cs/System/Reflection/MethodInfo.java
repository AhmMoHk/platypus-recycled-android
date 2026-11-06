package cs.System.Reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class MethodInfo extends MethodBase {
    public MethodInfo() {}

    MethodInfo(cs.System.Type declaring, Method m) {
        this.declaring = declaring;
        this.method = m;
        this.name = m.getName();
        this.attrKey = "M:" + m.getName();
        this.flags = (Modifier.isStatic(m.getModifiers()) ? 1 : 0) | 2;
    }

    /** from a $mmeta row: {javaName, csName, flags, retType, paramTypes[], paramNames[]} */
    MethodInfo(cs.System.Type declaring, Method m, Object[] row) {
        this.declaring = declaring;
        this.method = m;
        this.name = (String) row[1];
        this.flags = (Integer) row[2];
        this.retType = (String) row[3];
        this.attrKey = "M:" + row[0];
        String[] pts = (String[]) row[4], pns = (String[]) row[5];
        this.ntype = m == null ? 0 : m.getParameterTypes().length - pts.length;
        params = new ParameterInfo[pts.length];
        for (int i = 0; i < pts.length; i++) {
            String ts = pts[i];
            boolean byref = ts.endsWith("&");
            cs.System.Type pt = Assembly.resolveType(ts);
            if (pt == null) pt = cs.System.Type.OBJECT;
            params[i] = new ParameterInfo(pns[i], pt, i, byref);
        }
    }

    public static MethodInfo ofDelegate(cs.System.Delegate d) {
        MethodInfo mi = new MethodInfo();
        String k = d.$key == null ? "?" : d.$key;
        int a = k.indexOf("::"), b = k.indexOf('#');
        mi.name = a >= 0 ? k.substring(a + 2, b > a ? b : k.length()) : k;
        return mi;
    }

    public static void collect(cs.System.Type t, int bf, ArrayList<MethodInfo> out) {
        boolean declaredOnly = (bf & 2) != 0;
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (Class<?> c = t.cls; c != null && c != Object.class; c = c.getSuperclass()) {
            Object[][] meta = Meta.methods(c);
            if (meta != null) {
                for (Object[] row : meta) {
                    int fl = (Integer) row[2];
                    if ((fl & 32) != 0) continue;                         // constructors
                    boolean st = (fl & 1) != 0, pub = (fl & 2) != 0;
                    if (st && (bf & 8) == 0 || !st && (bf & 4) == 0 || pub && (bf & 16) == 0 || !pub && (bf & 32) == 0) continue;
                    if (c != t.cls && (st || (fl & 64) != 0)) continue;
                    String sig = row[1] + "/" + java.util.Arrays.toString((String[]) row[4]);
                    if ((fl & 4) != 0 && !seen.add(sig)) continue;            // overridden in a subclass
                    String[] pts = (String[]) row[4];
                    Method jm = Meta.findJava(c, (String) row[0], pts.length + (((fl & 2048) != 0) ? countGeneric(c, (String) row[0], pts.length) : 0), pts);
                    if (jm == null) continue;
                    out.add(new MethodInfo(cs.System.Type.of(c), jm, row));
                }
            } else {
                for (Method m : c.getDeclaredMethods()) {
                    String n = m.getName();
                    if (n.startsWith("$") || !Modifier.isPublic(m.getModifiers())) continue;
                    boolean st = Modifier.isStatic(m.getModifiers());
                    if (st && (bf & 8) == 0 || !st && (bf & 4) == 0 || (bf & 16) == 0) continue;
                    if (m.isBridge() || m.isSynthetic()) continue;
                    if (!seen.add(n + java.util.Arrays.toString(m.getParameterTypes()))) continue;   // full signature: overloads with equal counts
                    out.add(new MethodInfo(cs.System.Type.of(c), m));
                }
            }
            if (declaredOnly) break;
        }
    }

    private static int countGeneric(Class<?> c, String jn, int np) {
        for (Method m : c.getDeclaredMethods()) {
            if (!m.getName().equals(jn)) continue;
            Class<?>[] p = m.getParameterTypes();
            int k = 0;
            while (k < p.length && p[k] == cs.System.Type.class) k++;
            if (p.length - k == np) return k;
        }
        return 0;
    }

    public cs.System.Type get_ReturnType() {
        if (retType != null) {
            cs.System.Type t = Assembly.resolveType(retType);
            if (t != null) return t;
        }
        return method == null ? cs.System.Type.VOID : cs.System.Type.of(method.getReturnType());
    }

    public ParameterInfo get_ReturnParameter() { return new ParameterInfo("", get_ReturnType(), -1); }

    public MethodInfo MakeGenericMethod(cs.System.Type[] args) { return this; }
    public MethodInfo GetBaseDefinition() { return this; }

    public static boolean op_Equality(MethodInfo a, MethodInfo b) { return a == null ? b == null : a.equals(b); }
    public static boolean op_Inequality(MethodInfo a, MethodInfo b) { return !op_Equality(a, b); }

    @Override public boolean equals(Object o) {
        if (!(o instanceof MethodInfo)) return false;
        MethodInfo m = (MethodInfo) o;
        return m.method != null ? m.method.equals(method) : (m.name.equals(name) && m.declaring == declaring);
    }
    @Override public int hashCode() { return method != null ? method.hashCode() : name.hashCode(); }
    @Override public String toString() { return name; }
}
