package cs.System.Collections.Generic;

public interface IDictionary_2 extends ICollection_1 {
    void Add(Object k, Object v);
    boolean ContainsKey(Object k);
    boolean TryGetValue(Object k, cs.rt.RefO v);
    Object get_Item(Object k);
    void set_Item(Object k, Object v);
    ICollection_1 get_Keys();
    ICollection_1 get_Values();
}
