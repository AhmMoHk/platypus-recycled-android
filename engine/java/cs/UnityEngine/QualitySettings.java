package cs.UnityEngine;

public final class QualitySettings extends UObject {
    static int vSyncCount = 1, level;
    public static int get_vSyncCount() { return vSyncCount; }
    public static void set_vSyncCount(int v) { vSyncCount = v; }
    public static int GetQualityLevel() { return level; }
    public static void SetQualityLevel(int l) { level = l; }
    public static void SetQualityLevel(int l, boolean x) { level = l; }
    public static String[] get_names() { return new String[]{"High"}; }
    public static int get_antiAliasing() { return 0; }
    public static void set_antiAliasing(int v) {}
    public static int get_globalTextureMipmapLimit() { return 0; }
    public static void set_globalTextureMipmapLimit(int v) {}
}
