package cs.UnityEngine;

public class WWWForm {
    final java.util.LinkedHashMap<String, String> fields = new java.util.LinkedHashMap<>();
    public WWWForm() {}
    public void AddField(String k, String v) { fields.put(k, v); }
    public void AddField(String k, int v) { fields.put(k, Integer.toString(v)); }
}
