package cs.System.Collections;

public interface IList extends ICollection {
    int IList_Add(Object v);
    Object get_Item(int i);
    void set_Item(int i, Object v);
    void Clear();
    boolean Contains(Object v);
    int IndexOf(Object v);
    void RemoveAt(int i);
}
