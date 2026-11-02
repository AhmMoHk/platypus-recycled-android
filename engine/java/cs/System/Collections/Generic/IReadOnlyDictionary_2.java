package cs.System.Collections.Generic;

public interface IReadOnlyDictionary_2 extends IReadOnlyCollection_1 {
    boolean ContainsKey(Object k);
    boolean TryGetValue(Object k, cs.rt.RefO v);
    Object get_Item(Object k);
}
