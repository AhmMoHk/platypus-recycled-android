package cs.Rewired;

public class InputActionEventData implements cs.rt.Struct {
    Player player;
    String action = "";
    public InputActionEventData() {}
    public Player get_player() { return player; }
    public int get_playerId() { return player == null ? 0 : player.id; }
    public String get_actionName() { return action; }
    public String get_actionDescriptiveName() { return action; }
    public boolean GetButtonDown() { return player != null && player.GetButtonDown(action); }
    public boolean GetButton() { return player != null && player.GetButton(action); }
    public float GetAxis() { return player == null ? 0 : player.GetAxis(action); }
    public InputActionEventData $copy() { InputActionEventData d = new InputActionEventData(); d.player = player; d.action = action; return d; }
    public void $set(Object o) { InputActionEventData d = (InputActionEventData) o; player = d.player; action = d.action; }
    public void $clear() { player = null; action = ""; }
    public static InputActionEventData[] $newArray(int n) { InputActionEventData[] a = new InputActionEventData[n]; for (int i = 0; i < n; i++) a[i] = new InputActionEventData(); return a; }
}
