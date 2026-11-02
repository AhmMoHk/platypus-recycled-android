package cs.UnityEngine;

import java.util.ArrayList;

/** Sorting layers: id (uniqueID) -> value (order in the TagManager list, Default = its index) */
public class SortingLayer implements cs.rt.Struct {
    static String[] names = {"Default"};
    static long[] ids = {0};
    public int m_Id;
    public SortingLayer() {}

    static void init(ArrayList<Object> l) {
        names = new String[l.size()];
        ids = new long[l.size()];
        for (int i = 0; i < l.size(); i++) {
            java.util.HashMap<String, Object> m = cs.rt.Json.map(l.get(i));
            names[i] = (String) m.get("name");
            ids[i] = cs.rt.Json.lng(m.get("uniqueID"));
        }
    }
    static int defaultIndex() {
        for (int i = 0; i < ids.length; i++) if (ids[i] == 0) return i;
        return 0;
    }
    /** layer value relative to Default (Unity: negative for layers before Default) */
    public static int GetLayerValueFromID(int id) {
        for (int i = 0; i < ids.length; i++) if ((int) ids[i] == id) return i - defaultIndex();
        return 0;
    }
    public static int NameToID(String n) {
        for (int i = 0; i < names.length; i++) if (names[i].equals(n)) return (int) ids[i];
        return 0;
    }
    public static String IDToName(int id) {
        for (int i = 0; i < ids.length; i++) if ((int) ids[i] == id) return names[i];
        return null;
    }
    public static int GetLayerValueFromName(String n) { return GetLayerValueFromID(NameToID(n)); }
    public static boolean IsValid(int id) { return IDToName(id) != null; }
    public int get_id() { return m_Id; }
    public String get_name() { return IDToName(m_Id); }
    public int get_value() { return GetLayerValueFromID(m_Id); }
    public static SortingLayer[] get_layers() {
        SortingLayer[] r = new SortingLayer[ids.length];
        for (int i = 0; i < r.length; i++) { r[i] = new SortingLayer(); r[i].m_Id = (int) ids[i]; }
        return r;
    }
    public SortingLayer $copy() { SortingLayer s = new SortingLayer(); s.m_Id = m_Id; return s; }
    public void $set(Object o) { m_Id = ((SortingLayer) o).m_Id; }
    public void $clear() { m_Id = 0; }
    public static SortingLayer[] $newArray(int n) { SortingLayer[] a = new SortingLayer[n]; for (int i = 0; i < n; i++) a[i] = new SortingLayer(); return a; }
}
