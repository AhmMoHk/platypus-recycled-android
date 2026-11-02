package cs.System.Collections.Generic;

public interface ICollection_1 extends IEnumerable_1 {
    int get_Count();
    void Add(Object v);
    void Clear();
    boolean Contains(Object v);
    boolean Remove(Object v);
}
