package cs.UnityEngine;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Legacy input. The host queues touch / key / mouse events from any thread; they are applied at frame start.
 * Touches also drive the mouse (simulateMouseWithTouches) for the UI event system.
 */
public final class Input {
    private Input() {}
    private static final Object lock = new Object();
    private static final ArrayList<int[]> touchEvents = new ArrayList<>();       // id, phase(0 down,1 move,2 up,3 cancel), x*16, y*16
    private static final ArrayList<int[]> keyEvents = new ArrayList<>();         // keycode, down
    private static final ArrayList<float[]> mouseEvents = new ArrayList<>();     // x, y, buttons, scroll
    static final ArrayList<Touch> touches = new ArrayList<>();
    private static final HashSet<Integer> keys = new HashSet<>(), keysDown = new HashSet<>(), keysUp = new HashSet<>();
    static final Vector3 mousePos = new Vector3();
    static final Vector2 scroll = new Vector2();
    static int mouseButtons, mouseDown, mouseUp;
    static boolean simulateMouse = true, multiTouch = true;
    static String inputString = "", compositionString = "";
    static int imeMode;
    static final Vector2 compositionCursor = new Vector2();
    private static final StringBuilder typed = new StringBuilder();

    // ------------------------------------------------------------------ host API (thread-safe)
    /** phase: 0 down, 1 move, 2 up, 3 cancel; coordinates in screen pixels, origin bottom-left */
    public static void $touch(int id, int phase, float x, float y) {
        synchronized (lock) { touchEvents.add(new int[]{id, phase, Math.round(x * 16), Math.round(y * 16)}); }
    }
    public static void $key(int keyCode, boolean down) { synchronized (lock) { keyEvents.add(new int[]{keyCode, down ? 1 : 0}); } }
    public static void $char(char c) { synchronized (lock) { typed.append(c); } }
    public static void $mouse(float x, float y, int buttons, float scrollY) { synchronized (lock) { mouseEvents.add(new float[]{x, y, buttons, scrollY}); } }

    static void beginFrame() {
        ArrayList<int[]> te, ke;
        ArrayList<float[]> me;
        String chars;
        synchronized (lock) {
            te = new ArrayList<>(touchEvents); touchEvents.clear();
            ke = new ArrayList<>(keyEvents); keyEvents.clear();
            me = new ArrayList<>(mouseEvents); mouseEvents.clear();
            chars = typed.toString(); typed.setLength(0);
        }
        inputString = chars;
        // touches: drop ended, mark stationary
        for (int i = touches.size() - 1; i >= 0; i--) {
            Touch t = touches.get(i);
            if (t.m_Phase == 3 || t.m_Phase == 4) touches.remove(i);
            else { t.m_Phase = 2; t.m_PositionDelta.Set(0, 0); }
        }
        for (int[] e : te) {
            Touch t = find(e[0]);
            float x = e[2] / 16f, y = e[3] / 16f;
            if (e[1] == 0) {
                if (t != null && t.m_Phase != 3 && t.m_Phase != 4) continue;
                t = new Touch();
                t.m_FingerId = e[0];
                t.m_Phase = 0;
                t.m_Position.Set(x, y);
                t.m_RawPosition.Set(x, y);
                touches.add(t);
            } else if (t != null) {
                t.m_PositionDelta.Set(t.m_PositionDelta.x + x - t.m_Position.x, t.m_PositionDelta.y + y - t.m_Position.y);
                t.m_Position.Set(x, y);
                if (e[1] == 1) { if (t.m_Phase != 0) t.m_Phase = 1; }
                else t.m_Phase = e[1] == 2 ? 3 : 4;
            }
            t.m_TimeDelta = Time.unscaledDeltaTime;
        }
        keysDown.clear();
        keysUp.clear();
        for (int[] k : ke) {
            if (k[1] == 1) { if (keys.add(k[0])) keysDown.add(k[0]); }
            else if (keys.remove(k[0])) keysUp.add(k[0]);
        }
        // mouse: from real mouse events, else simulated from the primary touch
        int prev = mouseButtons;
        scroll.Set(0, 0);
        if (!me.isEmpty()) {
            for (float[] m : me) { mousePos.Set(m[0], m[1], 0); mouseButtons = (int) m[2]; scroll.y += m[3]; }
        } else if (simulateMouse && !touches.isEmpty()) {
            Touch t = touches.get(0);
            mousePos.Set(t.m_Position.x, t.m_Position.y, 0);
            mouseButtons = t.m_Phase == 3 || t.m_Phase == 4 ? 0 : 1;
        } else if (simulateMouse && te.size() > 0) {
            mouseButtons = 0;
        }
        mouseDown = mouseButtons & ~prev;
        mouseUp = prev & ~mouseButtons;
    }

    private static Touch find(int id) {
        for (Touch t : touches) if (t.m_FingerId == id) return t;
        return null;
    }

    // ------------------------------------------------------------------ Unity API
    public static int get_touchCount() { return touches.size(); }
    public static Touch GetTouch(int i) {
        if (i < 0 || i >= touches.size()) throw new cs.UnityEngine.UnityException("Index out of bounds.");
        return touches.get(i).$copy();
    }
    public static Touch[] get_touches() { Touch[] r = new Touch[touches.size()]; for (int i = 0; i < r.length; i++) r[i] = touches.get(i).$copy(); return r; }
    public static boolean get_touchSupported() { return Host.get().isMobile(); }
    public static boolean get_touchPressureSupported() { return false; }
    public static boolean get_multiTouchEnabled() { return multiTouch; }
    public static void set_multiTouchEnabled(boolean v) { multiTouch = v; }
    public static boolean get_simulateMouseWithTouches() { return simulateMouse; }
    public static void set_simulateMouseWithTouches(boolean v) { simulateMouse = v; }
    public static boolean get_mousePresent() { return !Host.get().isMobile(); }
    public static Vector3 get_mousePosition() { return mousePos.$copy(); }
    public static Vector2 get_mouseScrollDelta() { return scroll.$copy(); }
    public static boolean GetMouseButton(int b) { return (mouseButtons & (1 << b)) != 0; }
    public static boolean GetMouseButtonDown(int b) { return (mouseDown & (1 << b)) != 0; }
    public static boolean GetMouseButtonUp(int b) { return (mouseUp & (1 << b)) != 0; }
    public static boolean GetKey(int k) { return keys.contains(k); }
    public static boolean GetKeyDown(int k) { return keysDown.contains(k); }
    public static boolean GetKeyUp(int k) { return keysUp.contains(k); }
    public static boolean GetKey(String n) { return GetKey(KeyNames.code(n)); }
    public static boolean GetKeyDown(String n) { return GetKeyDown(KeyNames.code(n)); }
    public static boolean GetKeyUp(String n) { return GetKeyUp(KeyNames.code(n)); }
    public static boolean get_anyKey() { return !keys.isEmpty() || mouseButtons != 0; }
    public static boolean get_anyKeyDown() { return !keysDown.isEmpty() || mouseDown != 0; }
    public static String get_inputString() { return inputString; }

    /** legacy virtual axes/buttons (InputManager defaults) */
    public static float GetAxisRaw(String n) {
        switch (n) {
            case "Horizontal": return (keys.contains(275) || keys.contains(100) ? 1 : 0) - (keys.contains(276) || keys.contains(97) ? 1 : 0);
            case "Vertical": return (keys.contains(273) || keys.contains(119) ? 1 : 0) - (keys.contains(274) || keys.contains(115) ? 1 : 0);
            case "Mouse ScrollWheel": return scroll.y;
            default: return cs.Rewired.ReInput.$action(n);
        }
    }
    public static float GetAxis(String n) { return GetAxisRaw(n); }
    public static boolean GetButton(String n) { return button(n, keys); }
    public static boolean GetButtonDown(String n) { return button(n, keysDown); }
    public static boolean GetButtonUp(String n) { return button(n, keysUp); }
    private static boolean button(String n, HashSet<Integer> s) {
        switch (n) {
            case "Submit": return s.contains(13) || s.contains(271) || s.contains(32);
            case "Cancel": return s.contains(27);
            case "Fire1": return s.contains(306);
            case "Jump": return s.contains(32);
            default: return s == keys ? cs.Rewired.ReInput.$action(n) != 0 : s == keysDown ? cs.Rewired.ReInput.$actionDown(n) : cs.Rewired.ReInput.$actionUp(n);
        }
    }
    public static void ResetInputAxes() { keys.clear(); }

    public static int get_imeCompositionMode() { return imeMode; }
    public static void set_imeCompositionMode(int m) { imeMode = m; }
    public static String get_compositionString() { return compositionString; }
    public static Vector2 get_compositionCursorPos() { return compositionCursor.$copy(); }
    public static void set_compositionCursorPos(Vector2 v) { compositionCursor.$set(v); }
    public static boolean get_imeIsSelected() { return false; }
    public static Vector3 get_acceleration() { return new Vector3(); }
    public static int get_deviceOrientation() { return 3; }
}
