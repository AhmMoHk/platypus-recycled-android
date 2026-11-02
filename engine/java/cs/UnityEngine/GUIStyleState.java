package cs.UnityEngine;

public class GUIStyleState {
    final Color textColor = new Color(1, 1, 1, 1);
    public void set_textColor(Color c) { textColor.$set(c); }
    public Color get_textColor() { return textColor.$copy(); }
}
