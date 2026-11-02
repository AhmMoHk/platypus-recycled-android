package cs.UnityEngine;

/** OperatingSystemFamily: Other 0, MacOSX 1, Windows 2, Linux 3 */
public final class SystemInfo {
    private SystemInfo() {}
    public static int get_operatingSystemFamily() { return Host.get().isMobile() ? 3 : 2; }
    public static String get_operatingSystem() { return Host.get().isMobile() ? "Android" : "Linux"; }
    public static String get_deviceModel() { return "Android"; }
    public static String get_deviceUniqueIdentifier() { return "prr-device"; }
    public static int get_systemMemorySize() { return 4096; }
    public static int get_graphicsMemorySize() { return 1024; }
    public static int get_processorCount() { return Runtime.getRuntime().availableProcessors(); }
    public static int get_deviceType() { return Host.get().isMobile() ? 1 : 3; }
    public static String get_graphicsDeviceName() { return "GLES3"; }
    public static int get_maxTextureSize() { return 8192; }
    public static boolean get_supportsInstancing() { return false; }
    public static boolean SupportsTextureFormat(int f) { return true; }
}
