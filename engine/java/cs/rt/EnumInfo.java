package cs.rt;

import java.util.HashMap;

/** Names and values of a .NET enum (enum values themselves are plain Java primitives). */
public final class EnumInfo {
    public final String name;
    public final Class<?> cls;
    public final boolean flags;
    public final String[] names;
    public final long[] values;
    private HashMap<String, Long> byName;
    private final BoxedEnum[] small = new BoxedEnum[64];

    public EnumInfo(String name, Class<?> cls, boolean flags, String[] names, long[] values) {
        this.name = name;
        this.cls = cls;
        this.flags = flags;
        this.names = names;
        this.values = values;
    }

    public BoxedEnum box(long v) {
        if (v >= 0 && v < small.length) {
            BoxedEnum b = small[(int) v];
            if (b == null) small[(int) v] = b = new BoxedEnum(this, v);
            return b;
        }
        return new BoxedEnum(this, v);
    }

    public String nameOf(long v) {
        for (int i = 0; i < values.length; i++) if (values[i] == v) return names[i];
        if (flags && v != 0) {
            StringBuilder sb = new StringBuilder();
            long rest = v;
            for (int i = values.length - 1; i >= 0; i--) {
                long f = values[i];
                if (f != 0 && (rest & f) == f) {
                    if (sb.length() > 0) sb.insert(0, ", ");
                    sb.insert(0, names[i]);
                    rest &= ~f;
                }
            }
            if (rest == 0) return sb.toString();
        }
        return Long.toString(v);
    }

    public boolean isDefined(long v) {
        for (long x : values) if (x == v) return true;
        return false;
    }

    /** Parse a name (or comma separated names for flags, or a number); null when not found. */
    public Long parse(String s, boolean ignoreCase) {
        if (byName == null) {
            byName = new HashMap<>();
            for (int i = 0; i < names.length; i++) byName.put(names[i], values[i]);
        }
        s = s.trim();
        if (s.isEmpty()) return null;
        char c = s.charAt(0);
        if (Character.isDigit(c) || c == '-' || c == '+') {
            try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
        }
        long total = 0;
        for (String part : s.split(",")) {
            part = part.trim();
            Long v = byName.get(part);
            if (v == null && ignoreCase) {
                for (int i = 0; i < names.length; i++) if (names[i].equalsIgnoreCase(part)) { v = values[i]; break; }
            }
            if (v == null) return null;
            total |= v;
        }
        return total;
    }
}
