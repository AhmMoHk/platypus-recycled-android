package cs.UnityEngine;

/** IMGUI events (keyboard input for input fields); fed by the host's typed characters. */
public class Event {
    int type = 12, keyCode, modifiers;
    char character;
    String commandName = "";
    private static final java.util.ArrayDeque<Object[]> queue = new java.util.ArrayDeque<>();
    public Event() {}
    public static void $push(int type, int keyCode, char c, int mods) { synchronized (queue) { queue.add(new Object[]{type, keyCode, c, mods}); } }
    public static boolean PopEvent(Event e) {
        Object[] o;
        synchronized (queue) { o = queue.poll(); }
        if (o == null) return false;
        e.type = (Integer) o[0]; e.keyCode = (Integer) o[1]; e.character = (Character) o[2]; e.modifiers = (Integer) o[3];
        return true;
    }
    public static int GetEventCount() { synchronized (queue) { return queue.size(); } }
    public int get_type() { return type; }
    public int get_rawType() { return type; }
    public int get_keyCode() { return keyCode; }
    public char get_character() { return character; }
    public int get_modifiers() { return modifiers; }
    public boolean get_shift() { return (modifiers & 1) != 0; }
    public boolean get_control() { return (modifiers & 2) != 0; }
    public boolean get_alt() { return (modifiers & 4) != 0; }
    public boolean get_command() { return (modifiers & 8) != 0; }
    public String get_commandName() { return commandName; }
    public static Event get_current() { return null; }
    public void Use() {}
}
