package cs.System.Collections.Generic;

public interface IList_1 extends ICollection_1 {
    Object get_Item(int i);
    void set_Item(int i, Object v);
    int IndexOf(Object v);
    void Insert(int i, Object v);
    void RemoveAt(int i);
}
