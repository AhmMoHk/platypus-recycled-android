package cs.UnityEngine;

/** TouchScreenKeyboard.Status: Visible 0, Done 1, Canceled 2, LostFocus 3 */
public class TouchScreenKeyboard {
    String text = "";
    boolean active;
    int status;
    RangeInt selection = new RangeInt();
    static boolean hideInput;
    public static boolean get_isSupported() { return Host.get().isMobile(); }
    public static boolean get_isInPlaceEditingAllowed() { return false; }
    public static boolean get_visible() { return false; }
    public static void set_hideInput(boolean v) { hideInput = v; }
    public static TouchScreenKeyboard Open(String t, int type, boolean autocorrect, boolean multiline, boolean secure, boolean alert, String placeholder, int limit) {
        TouchScreenKeyboard k = new TouchScreenKeyboard();
        k.text = t == null ? "" : t;
        k.active = true;
        return k;
    }
    public static TouchScreenKeyboard Open(String t) { return Open(t, 0, true, false, false, false, "", 0); }
    public static TouchScreenKeyboard Open(String t, int type) { return Open(t, type, true, false, false, false, "", 0); }
    public String get_text() { return text; }
    public void set_text(String t) { text = t; }
    public boolean get_active() { return active; }
    public void set_active(boolean v) { active = v; if (!v) status = 1; }
    public int get_status() { return status; }
    public boolean get_canGetSelection() { return false; }
    public boolean get_canSetSelection() { return false; }
    public RangeInt get_selection() { return selection.$copy(); }
    public void set_selection(RangeInt r) { selection.$set(r); }
    public void set_characterLimit(int n) {}
}
