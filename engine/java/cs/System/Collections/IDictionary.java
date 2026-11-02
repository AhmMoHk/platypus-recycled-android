package cs.System.Collections;

public interface IDictionary extends ICollection {
    void Add(Object k, Object v);
    IDictionaryEnumerator IDictionary_GetEnumerator();
    Object IDictionary_get_Item(Object k);
    void IDictionary_set_Item(Object k, Object v);
}
