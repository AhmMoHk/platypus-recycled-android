package cs.rt;

/** A .NET value type translated to a Java class: assignments copy, never alias. */
public interface Struct {
    Object $copy();
    void $set(Object src);
    void $clear();
}
