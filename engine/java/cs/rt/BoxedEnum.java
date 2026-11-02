package cs.rt;

/** A boxed .NET enum value: keeps its enum type so ToString() gives the name. */
public final class BoxedEnum implements Comparable<BoxedEnum> {
    public final EnumInfo info;
    public final long value;

    BoxedEnum(EnumInfo info, long value) {
        this.info = info;
        this.value = value;
    }

    public static Object of(EnumInfo info, long v) {
        return info.box(v);
    }

    @Override public boolean equals(Object o) {
        if (o instanceof BoxedEnum) {
            BoxedEnum b = (BoxedEnum) o;
            return b.value == value && b.info == info;
        }
        return false;
    }

    @Override public int hashCode() { return (int) (value ^ (value >>> 32)); }

    @Override public String toString() { return info.nameOf(value); }

    @Override public int compareTo(BoxedEnum o) { return Long.compare(value, o.value); }
}
