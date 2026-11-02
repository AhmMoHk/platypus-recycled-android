package cs.UnityEngine;

/** RuntimePlatform: WindowsPlayer 2, Android 11, LinuxPlayer 13 */
public final class Application {
    private Application() {}
    static cs.System.Delegate logReceived, logReceivedThreaded, focusChanged, quitting, lowMemory;
    static int targetFrameRate = -1;
    static boolean focused = true, quitRequested;
    private static boolean inLog;

    static void dispatchLog(String msg, String st, int type) {
        if (inLog) return;
        inLog = true;
        try {
            if (logReceived != null) logReceived.DynamicInvoke(new Object[]{msg, st, type});
            if (logReceivedThreaded != null) logReceivedThreaded.DynamicInvoke(new Object[]{msg, st, type});
        } catch (RuntimeException e) {
            Host.get().log(0, "log handler failed: " + e);
        } finally {
            inLog = false;
        }
    }

    public static void add_logMessageReceived(Application$LogCallback d) { logReceived = cs.System.Delegate.Combine(logReceived, d); }
    public static void remove_logMessageReceived(Application$LogCallback d) { logReceived = cs.System.Delegate.Remove(logReceived, d); }
    public static void add_logMessageReceivedThreaded(Application$LogCallback d) { logReceivedThreaded = cs.System.Delegate.Combine(logReceivedThreaded, d); }
    public static void remove_logMessageReceivedThreaded(Application$LogCallback d) { logReceivedThreaded = cs.System.Delegate.Remove(logReceivedThreaded, d); }
    public static void add_focusChanged(cs.System.Action_1 d) { focusChanged = cs.System.Delegate.Combine(focusChanged, d); }
    public static void remove_focusChanged(cs.System.Action_1 d) { focusChanged = cs.System.Delegate.Remove(focusChanged, d); }
    public static void add_quitting(cs.System.Action d) { quitting = cs.System.Delegate.Combine(quitting, d); }
    public static void remove_quitting(cs.System.Action d) { quitting = cs.System.Delegate.Remove(quitting, d); }
    public static void add_lowMemory(Application$LowMemoryCallback d) { lowMemory = cs.System.Delegate.Combine(lowMemory, d); }
    public static void remove_lowMemory(Application$LowMemoryCallback d) { lowMemory = cs.System.Delegate.Remove(lowMemory, d); }

    public static int get_platform() { return Host.get().isMobile() ? 11 : 2; }
    public static boolean get_isMobilePlatform() { return Host.get().isMobile(); }
    public static boolean get_isConsolePlatform() { return false; }
    public static boolean get_isEditor() { return false; }
    public static boolean get_isPlaying() { return true; }
    public static boolean get_isBatchMode() { return false; }
    public static boolean get_isFocused() { return focused; }
    public static boolean get_genuine() { return true; }
    public static boolean get_genuineCheckAvailable() { return false; }
    public static boolean get_runInBackground() { return false; }
    public static void set_runInBackground(boolean v) {}
    public static String get_version() { return "1.4.0"; }
    public static String get_unityVersion() { return "2022.3.62f2"; }
    public static String get_productName() { return "Platypus Reclayed"; }
    public static String get_companyName() { return "Squashy Software"; }
    public static String get_identifier() { return "com.prr.platypusreclayed"; }
    public static String get_systemLanguage$str() { return "English"; }
    public static int get_systemLanguage() { return 10; }
    public static String get_dataPath() { return Host.get().dataPath(); }
    public static String get_streamingAssetsPath() { return Host.get().dataPath() + "/StreamingAssets"; }
    public static String get_persistentDataPath() { return Host.get().persistentDataPath(); }
    public static String get_temporaryCachePath() { return Host.get().persistentDataPath() + "/cache"; }
    public static int get_targetFrameRate() { return targetFrameRate; }
    public static void set_targetFrameRate(int f) { targetFrameRate = f; Host.get().setTargetFrameRate(f); }
    public static void OpenURL(String u) { Host.get().openURL(u); }
    public static void Quit() { Quit(0); }
    public static void Quit(int code) {
        quitRequested = true;
        if (quitting != null) quitting.DynamicInvoke(new Object[0]);
        Host.get().quit();
    }
    public static int get_internetReachability() { return 0; }
    public static boolean CanStreamedLevelBeLoaded(int i) { return i >= 0 && i < Loader.buildSceneCount(); }

    /** host: app focus changed */
    public static void $setFocus(boolean f) {
        focused = f;
        if (focusChanged != null) focusChanged.DynamicInvoke(new Object[]{f});
        for (MonoBehaviour mb : new java.util.ArrayList<>(Eng.running)) Eng.invokeByName(mb, "OnApplicationFocus", f);
    }
    public static void $setPaused(boolean p) {
        for (MonoBehaviour mb : new java.util.ArrayList<>(Eng.running)) Eng.invokeByName(mb, "OnApplicationPause", p);
    }
}
