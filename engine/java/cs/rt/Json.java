package cs.rt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/** Minimal fast JSON parser: objects -> LinkedHashMap, arrays -> ArrayList, numbers -> Double/Long, true/false/null. */
public final class Json {
    private final String s;
    private int p;

    private Json(String s) { this.s = s; }

    public static Object parse(String s) {
        Json j = new Json(s);
        j.ws();
        Object v = j.value();
        return v;
    }

    private void ws() {
        while (p < s.length()) {
            char c = s.charAt(p);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') p++;
            else break;
        }
    }

    private Object value() {
        char c = s.charAt(p);
        switch (c) {
            case '{': return obj();
            case '[': return arr();
            case '"': return str();
            case 't': p += 4; return Boolean.TRUE;
            case 'f': p += 5; return Boolean.FALSE;
            case 'n': p += 4; return null;
            default: return num();
        }
    }

    private HashMap<String, Object> obj() {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        p++;
        ws();
        if (s.charAt(p) == '}') { p++; return m; }
        while (true) {
            ws();
            String k = str();
            ws();
            p++;            // :
            ws();
            m.put(k, value());
            ws();
            char c = s.charAt(p++);
            if (c == '}') return m;
        }
    }

    private ArrayList<Object> arr() {
        ArrayList<Object> a = new ArrayList<>();
        p++;
        ws();
        if (s.charAt(p) == ']') { p++; return a; }
        while (true) {
            ws();
            a.add(value());
            ws();
            char c = s.charAt(p++);
            if (c == ']') return a;
        }
    }

    private String str() {
        p++;
        StringBuilder sb = null;
        int start = p;
        while (true) {
            char c = s.charAt(p);
            if (c == '"') {
                String r = sb == null ? s.substring(start, p) : sb.append(s, start, p).toString();
                p++;
                return r;
            }
            if (c == '\\') {
                if (sb == null) sb = new StringBuilder();
                sb.append(s, start, p);
                char e = s.charAt(p + 1);
                p += 2;
                switch (e) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'u': sb.append((char) Integer.parseInt(s.substring(p, p + 4), 16)); p += 4; break;
                    default: sb.append(e);
                }
                start = p;
                continue;
            }
            p++;
        }
    }

    private Object num() {
        int start = p;
        boolean frac = false;
        while (p < s.length()) {
            char c = s.charAt(p);
            if (c >= '0' && c <= '9' || c == '-' || c == '+') p++;
            else if (c == '.' || c == 'e' || c == 'E') { frac = true; p++; }
            else break;
        }
        String t = s.substring(start, p);
        if (!frac) {
            try { return Long.parseLong(t); } catch (NumberFormatException e) { return Double.parseDouble(t); }
        }
        return Double.parseDouble(t);
    }

    // ------------------------------------------------------------------ helpers
    public static double num(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o instanceof Boolean) return ((Boolean) o) ? 1 : 0;
        if (o instanceof String) {
            String s = (String) o;
            if (s.equals("NaN")) return Double.NaN;
            if (s.equals("Inf")) return Double.POSITIVE_INFINITY;
            if (s.equals("-Inf")) return Double.NEGATIVE_INFINITY;
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    public static long lng(Object o) { return o instanceof Number ? ((Number) o).longValue() : (long) num(o); }
    public static int i(Object o) { return (int) lng(o); }
    public static float f(Object o) { return (float) num(o); }
    public static boolean b(Object o) { return o instanceof Boolean ? (Boolean) o : num(o) != 0; }

    @SuppressWarnings("unchecked")
    public static HashMap<String, Object> map(Object o) { return (HashMap<String, Object>) o; }

    @SuppressWarnings("unchecked")
    public static ArrayList<Object> list(Object o) { return (ArrayList<Object>) o; }

    // ------------------------------------------------------------------ writing (for saves)
    public static String write(Object v) {
        StringBuilder sb = new StringBuilder();
        write(sb, v);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(StringBuilder sb, Object v) {
        if (v == null) sb.append("null");
        else if (v instanceof String) quote(sb, (String) v);
        else if (v instanceof Boolean || v instanceof Number) sb.append(v);
        else if (v instanceof java.util.Map) {
            sb.append('{');
            boolean first = true;
            for (java.util.Map.Entry<String, Object> e : ((java.util.Map<String, Object>) v).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                quote(sb, e.getKey());
                sb.append(':');
                write(sb, e.getValue());
            }
            sb.append('}');
        } else if (v instanceof java.util.List) {
            sb.append('[');
            boolean first = true;
            for (Object x : (java.util.List<Object>) v) {
                if (!first) sb.append(',');
                first = false;
                write(sb, x);
            }
            sb.append(']');
        } else quote(sb, v.toString());
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') sb.append('\\').append(c);
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
            else sb.append(c);
        }
        sb.append('"');
    }
}
