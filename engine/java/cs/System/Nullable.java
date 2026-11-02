package cs.System;

public final class Nullable {
    private Nullable() {}
    public static Type GetUnderlyingType(Type t) { return t.gargs != null && t.cls == Nullable_1.class ? t.gargs[0] : null; }
}
