package cs.rt;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** .NET (invariant culture) number formatting and parsing. */
public final class Fmt {
    private Fmt() {}

    // ------------------------------------------------------------------ default ToString()
    public static String str(int v) { return Integer.toString(v); }
    public static String str(long v) { return Long.toString(v); }

    /** float.ToString(): shortest round-trip form (.NET Core 3.0+ / Unity), "E" notation for large/small. */
    public static String str(float f) {
        if (Float.isNaN(f)) return "NaN";
        if (Float.isInfinite(f)) return f > 0 ? "Infinity" : "-Infinity";
        if (f == 0) return (Float.floatToRawIntBits(f) < 0) ? "-0" : "0";
        return shortest(new BigDecimal(Float.toString(f)), 7);
    }

    public static String str(double d) {
        if (Double.isNaN(d)) return "NaN";
        if (Double.isInfinite(d)) return d > 0 ? "Infinity" : "-Infinity";
        if (d == 0) return (Double.doubleToRawLongBits(d) < 0) ? "-0" : "0";
        return shortest(new BigDecimal(Double.toString(d)), 15);
    }

    /** "R"/shortest digits rendered like .NET: plain for exponent in [-5, digits), else d.dddE+XX */
    private static String shortest(BigDecimal bd, int precision) {
        bd = bd.stripTrailingZeros();
        String digits = bd.unscaledValue().abs().toString();
        int exp = digits.length() - 1 - bd.scale();          // decimal exponent of first digit
        boolean neg = bd.signum() < 0;
        String r;
        if (exp >= -5 && exp < Math.max(precision, digits.length())) {
            r = bd.abs().toPlainString();
        } else {
            String mant = digits.length() > 1 ? digits.charAt(0) + "." + digits.substring(1) : digits;
            r = mant + "E" + (exp < 0 ? "-" : "+") + pad2(Math.abs(exp));
        }
        return neg ? "-" + r : r;
    }

    private static String pad2(int e) { return e < 10 ? "0" + e : Integer.toString(e); }

    // ------------------------------------------------------------------ ToString(format)
    public static String format(Object v, String fmt) {
        if (v instanceof Integer || v instanceof Short || v instanceof Byte) return formatLong(((Number) v).longValue(), fmt, false);
        if (v instanceof Long) return formatLong((Long) v, fmt, false);
        if (v instanceof Float) return formatDouble((Float) v, fmt, true);
        if (v instanceof Double) return formatDouble((Double) v, fmt, false);
        if (v instanceof BoxedEnum) {
            if (fmt != null && (fmt.equals("D") || fmt.equals("d"))) return Long.toString(((BoxedEnum) v).value);
            return v.toString();
        }
        if (v instanceof cs.System.IFormattable) return ((cs.System.IFormattable) v).ToString(fmt, null);
        return cs.System.ObjectX.ToString(v);
    }

    public static String formatLong(long v, String fmt, boolean unsigned) {
        if (fmt == null || fmt.isEmpty()) return Long.toString(v);
        char c = Character.toUpperCase(fmt.charAt(0));
        String ps = fmt.substring(1);
        boolean standard = fmt.length() <= 3 && (ps.isEmpty() || Rt.allDigits(ps)) && "DXNFGEPC".indexOf(c) >= 0;
        if (standard) {
            int prec = ps.isEmpty() ? -1 : Integer.parseInt(ps);
            switch (c) {
                case 'D': {
                    String s = Long.toString(Math.abs(v));
                    while (prec > s.length()) s = "0" + s;
                    return v < 0 ? "-" + s : s;
                }
                case 'X': {
                    String s = Long.toHexString(v);
                    if (v < 0 && v >= Integer.MIN_VALUE) s = Integer.toHexString((int) v);
                    if (fmt.charAt(0) == 'X') s = s.toUpperCase();
                    while (prec > s.length()) s = "0" + s;
                    return s;
                }
                case 'G':
                    return Long.toString(v);
                default:
                    return formatDouble((double) v, fmt, false);
            }
        }
        return custom(new BigDecimal(v), fmt);
    }

    public static String formatDouble(double d, String fmt, boolean single) {
        if (fmt == null || fmt.isEmpty() || fmt.equals("G") || fmt.equals("g") || fmt.equals("R") || fmt.equals("r"))
            return single ? str((float) d) : str(d);
        if (Double.isNaN(d)) return "NaN";
        if (Double.isInfinite(d)) return d > 0 ? "Infinity" : "-Infinity";
        char c = Character.toUpperCase(fmt.charAt(0));
        String ps = fmt.substring(1);
        boolean standard = (ps.isEmpty() || (ps.length() <= 2 && Rt.allDigits(ps))) && "FNGEPCD".indexOf(c) >= 0;
        BigDecimal bd = single ? new BigDecimal(Float.toString((float) d)) : new BigDecimal(Double.toString(d));
        if (standard) {
            int prec = ps.isEmpty() ? -1 : Integer.parseInt(ps);
            switch (c) {
                case 'F': return fixed(bd, prec < 0 ? 2 : prec, false);
                case 'N': return fixed(bd, prec < 0 ? 2 : prec, true);
                case 'P': return fixed(bd.multiply(BigDecimal.valueOf(100)), prec < 0 ? 2 : prec, true) + " %";
                case 'C': return "¤" + fixed(bd, prec < 0 ? 2 : prec, true);
                case 'D': return Long.toString((long) d);
                case 'E': {
                    int p = prec < 0 ? 6 : prec;
                    if (bd.signum() == 0) return "0." + Rt.repeat("0", p) + "E+000";
                    int exp = bd.precision() - bd.scale() - 1;
                    BigDecimal m = bd.movePointLeft(exp).setScale(p, RoundingMode.HALF_EVEN);
                    if (m.abs().compareTo(BigDecimal.TEN) >= 0) { exp++; m = bd.movePointLeft(exp).setScale(p, RoundingMode.HALF_EVEN); }
                    String es = Integer.toString(Math.abs(exp));
                    while (es.length() < 3) es = "0" + es;
                    return m.toPlainString() + (fmt.charAt(0) == 'e' ? "e" : "E") + (exp < 0 ? "-" : "+") + es;
                }
                case 'G': {
                    if (prec <= 0) return single ? str((float) d) : str(d);
                    BigDecimal r = bd.round(new java.math.MathContext(prec, RoundingMode.HALF_EVEN));
                    return shortest(r, prec);
                }
            }
        }
        return custom(bd, fmt);
    }

    private static String fixed(BigDecimal bd, int prec, boolean group) {
        BigDecimal r = bd.setScale(prec, RoundingMode.HALF_UP);
        String s = r.abs().toPlainString();
        if (group) {
            int dot = s.indexOf('.');
            String ip = dot < 0 ? s : s.substring(0, dot), fp = dot < 0 ? "" : s.substring(dot);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ip.length(); i++) {
                if (i > 0 && (ip.length() - i) % 3 == 0) sb.append(',');
                sb.append(ip.charAt(i));
            }
            s = sb + fp;
        }
        boolean neg = r.signum() < 0;
        return neg ? "-" + s : s;
    }

    /** Custom numeric format: 0 # . , % and literals ('..', "..", \x); sections separated by ; */
    private static String custom(BigDecimal v, String fmt) {
        String[] sections = splitSections(fmt);
        String f = sections[0];
        boolean neg = v.signum() < 0;
        if (neg && sections.length > 1 && !sections[1].isEmpty()) { f = sections[1]; v = v.negate(); neg = false; }
        else if (v.signum() == 0 && sections.length > 2) { f = sections[2]; }
        int pct = 0;
        int intZeros = 0, fracMin = 0, fracMax = 0;
        boolean dot = false, group = false;
        int scaleCommas = 0;
        for (int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            if (c == '\'' || c == '"') { int j = f.indexOf(c, i + 1); i = j < 0 ? f.length() : j; continue; }
            if (c == '\\') { i++; continue; }
            if (c == '.') { if (!dot) dot = true; continue; }
            if (c == '%') pct++;
            if (c == ',') {
                // thousands separator if followed by a digit placeholder before the decimal point, else scaling
                int j = i + 1;
                if (!dot && j < f.length() && (f.charAt(j) == '0' || f.charAt(j) == '#')) group = true;
                else if (!dot) scaleCommas++;
            }
            if (c == '0' || c == '#') {
                if (dot) { fracMax++; if (c == '0') fracMin = fracMax; }
                else if (c == '0' || intZeros > 0) { if (c == '0') intZeros++; }
            }
        }
        // count integer zeros properly (zeros after the first 0 in the integer part)
        intZeros = 0;
        boolean seenZero = false;
        for (int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            if (c == '\'' || c == '"') { int j = f.indexOf(c, i + 1); i = j < 0 ? f.length() : j; continue; }
            if (c == '\\') { i++; continue; }
            if (c == '.') break;
            if (c == '0') seenZero = true;
            if (seenZero && (c == '0' || c == '#')) intZeros++;
        }
        for (int i = 0; i < pct; i++) v = v.multiply(BigDecimal.valueOf(100));
        for (int i = 0; i < scaleCommas; i++) v = v.divide(BigDecimal.valueOf(1000));
        BigDecimal r = v.abs().setScale(fracMax, RoundingMode.HALF_UP);
        String s = r.toPlainString();
        int dp = s.indexOf('.');
        String ip = dp < 0 ? s : s.substring(0, dp);
        String fp = dp < 0 ? "" : s.substring(dp + 1);
        while (fp.length() > fracMin && fp.endsWith("0")) fp = fp.substring(0, fp.length() - 1);
        if (ip.equals("0") && intZeros == 0) ip = "";
        while (ip.length() < intZeros) ip = "0" + ip;
        if (group && ip.length() > 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ip.length(); i++) {
                if (i > 0 && (ip.length() - i) % 3 == 0) sb.append(',');
                sb.append(ip.charAt(i));
            }
            ip = sb.toString();
        }
        String number = ip + (fp.isEmpty() ? "" : "." + fp);
        if (r.signum() == 0) neg = false;
        // rebuild with literals: emit literals before/after the number block
        StringBuilder out = new StringBuilder();
        boolean emitted = false;
        for (int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            if (c == '\'' || c == '"') {
                int j = f.indexOf(c, i + 1);
                if (j < 0) j = f.length();
                out.append(f, i + 1, j);
                i = j;
                continue;
            }
            if (c == '\\' && i + 1 < f.length()) { out.append(f.charAt(++i)); continue; }
            if (c == '0' || c == '#' || c == '.' || c == ',') {
                if (!emitted) { out.append(number); emitted = true; }
                continue;
            }
            out.append(c);
        }
        if (!emitted) out.append(number);
        return (neg ? "-" : "") + out;
    }

    private static String[] splitSections(String f) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            if (c == '\'' || c == '"') {
                int j = f.indexOf(c, i + 1);
                if (j < 0) j = f.length() - 1;
                cur.append(f, i, j + 1);
                i = j;
            } else if (c == '\\' && i + 1 < f.length()) {
                cur.append(c).append(f.charAt(++i));
            } else if (c == ';') {
                out.add(cur.toString());
                cur.setLength(0);
            } else cur.append(c);
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    // ------------------------------------------------------------------ composite formatting
    public static String compose(String fmt, Object[] args) {
        StringBuilder sb = new StringBuilder();
        int n = fmt.length();
        for (int i = 0; i < n; i++) {
            char c = fmt.charAt(i);
            if (c == '{') {
                if (i + 1 < n && fmt.charAt(i + 1) == '{') { sb.append('{'); i++; continue; }
                int j = fmt.indexOf('}', i);
                if (j < 0) throw new cs.System.FormatException("Input string was not in a correct format.");
                String spec = fmt.substring(i + 1, j);
                i = j;
                String f = null;
                int colon = spec.indexOf(':');
                if (colon >= 0) { f = spec.substring(colon + 1); spec = spec.substring(0, colon); }
                int align = 0;
                int comma = spec.indexOf(',');
                if (comma >= 0) { align = Integer.parseInt(spec.substring(comma + 1).trim()); spec = spec.substring(0, comma); }
                int idx = Integer.parseInt(spec.trim());
                if (idx >= args.length) throw new cs.System.FormatException("Index (zero based) must be greater than or equal to zero and less than the size of the argument list.");
                Object a = args[idx];
                String s = a == null ? "" : (f != null ? format(a, f) : cs.System.ObjectX.ToString(a));
                if (align > 0) while (s.length() < align) s = " " + s;
                else if (align < 0) while (s.length() < -align) s = s + " ";
                sb.append(s);
            } else if (c == '}') {
                if (i + 1 < n && fmt.charAt(i + 1) == '}') i++;
                sb.append('}');
            } else sb.append(c);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ parsing
    public static int parseInt(String s) {
        if (s == null) throw new cs.System.ArgumentNullException("s");
        String t = s.trim();
        try {
            if (t.startsWith("+")) t = t.substring(1);
            return Integer.parseInt(t);
        } catch (NumberFormatException e) {
            throw new cs.System.FormatException("Input string was not in a correct format.");
        }
    }

    public static long parseLong(String s) {
        if (s == null) throw new cs.System.ArgumentNullException("s");
        String t = s.trim();
        try {
            if (t.startsWith("+")) t = t.substring(1);
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            throw new cs.System.FormatException("Input string was not in a correct format.");
        }
    }

    public static double parseDouble(String s) {
        if (s == null) throw new cs.System.ArgumentNullException("s");
        String t = s.trim();
        if (t.isEmpty() || t.endsWith("d") || t.endsWith("D") || t.endsWith("f") || t.endsWith("F") || t.startsWith("0x") || t.contains("_"))
            throw new cs.System.FormatException("Input string was not in a correct format.");
        if (t.equals("Infinity") || t.equals("∞")) return Double.POSITIVE_INFINITY;
        if (t.equals("-Infinity") || t.equals("-∞")) return Double.NEGATIVE_INFINITY;
        if (t.equals("NaN")) return Double.NaN;
        t = t.replace(",", "");
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            throw new cs.System.FormatException("Input string was not in a correct format.");
        }
    }

    public static boolean tryInt(String s, RefI out) {
        try { out.v = parseInt(s); return true; } catch (RuntimeException e) { out.v = 0; return false; }
    }

    public static boolean tryDouble(String s, RefD out) {
        try { out.v = parseDouble(s); return true; } catch (RuntimeException e) { out.v = 0; return false; }
    }

    public static boolean tryFloat(String s, RefF out) {
        try { out.v = (float) parseDouble(s); return true; } catch (RuntimeException e) { out.v = 0; return false; }
    }
}
