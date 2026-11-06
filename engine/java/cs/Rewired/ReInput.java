package cs.Rewired;

import cs.System.Collections.Generic.List_1;
import java.util.ArrayList;
import java.util.HashMap;

/** Rewired replacement: one player, a touch controller (reported as a mouse) and the keyboard. */
public final class ReInput {
    private ReInput() {}
    static final String[] ACTIONS = {"MoveHorizontal", "MoveVertical", "Fire", "Pause", "UIHorizontal", "UIVertical", "UISubmit", "UICancel", "UISkinChange",
        "DebugMenuOpen", "DebugMenuCombo1", "DebugMenuCombo2"};
    static final Mouse touch = new Mouse("Touch", "Touch");
    static final Controller keyboard = new Controller("Keyboard", 0, "Keyboard");
    static final Player p0 = new Player(0, "Player0"), system = new Player(9999999, "SYSTEM");
    static final List_1 players = new List_1(), allPlayersList = new List_1(), controllers = new List_1();
    static final ArrayList<Player> allPlayers = new ArrayList<>();
    static final HashMap<Integer, Player> ghosts = new HashMap<>();
    static Controller lastActive = touch;
    static cs.System.Delegate connected, disconnected;
    private static final ReInput$PlayerHelper playerHelper = new ReInput$PlayerHelper();
    private static final ReInput$ControllerHelper controllerHelper = new ReInput$ControllerHelper();

    static {
        players.Add(p0);
        allPlayersList.Add(system);
        allPlayersList.Add(p0);
        allPlayers.add(system);
        allPlayers.add(p0);
        controllers.Add(touch);
        if (!cs.UnityEngine.Host.get().isMobile()) controllers.Add(keyboard);
        system.controllers.AddController(touch, false);
        system.controllers.AddController(keyboard, false);
        system.controllers.set_hasMouse(true);
    }

    static Player ghost(int id) {
        Player p = ghosts.get(id);
        if (p == null) { p = new Player(id, "Player" + id); ghosts.put(id, p); }
        return p;
    }

    public static boolean get_isReady() { return true; }
    public static ReInput$PlayerHelper get_players() { return playerHelper; }
    public static ReInput$ControllerHelper get_controllers() { return controllerHelper; }
    public static void add_ControllerConnectedEvent(cs.System.Action_1 a) { connected = cs.System.Delegate.Combine(connected, a); }
    public static void remove_ControllerConnectedEvent(cs.System.Action_1 a) { connected = cs.System.Delegate.Remove(connected, a); }
    public static void add_ControllerDisconnectedEvent(cs.System.Action_1 a) { disconnected = cs.System.Delegate.Combine(disconnected, a); }
    public static void remove_ControllerDisconnectedEvent(cs.System.Action_1 a) { disconnected = cs.System.Delegate.Remove(disconnected, a); }
    public static void add_ControllerPreDisconnectEvent(cs.System.Action_1 a) {}
    public static void remove_ControllerPreDisconnectEvent(cs.System.Action_1 a) {}
    public static float get_unscaledTime() { return cs.UnityEngine.Time.get_unscaledTime(); }

    private static boolean prevTap, prevKeyAny;

    /** called by the engine at frame start (after Input.beginFrame) */
    public static void $beginFrame() {
        HashMap<String, Float> t = new HashMap<>(), k = new HashMap<>();
        float mx = TouchInput.moveX, my = TouchInput.moveY;
        t.put("MoveHorizontal", mx);
        t.put("MoveVertical", my);
        t.put("Fire", TouchInput.fire ? 1f : 0f);
        t.put("Pause", TouchInput.pause ? 1f : 0f);
        t.put("UICancel", TouchInput.back ? 1f : 0f);
        boolean tapAny = TouchInput.fire || TouchInput.pause || TouchInput.back || cs.UnityEngine.Input.get_touchCount() > 0 || cs.UnityEngine.Input.GetMouseButton(0);
        touch.sample(tapAny, tapAny && !prevTap);
        prevTap = tapAny;
        // keyboard (desktop): arrows/WASD move, Z/Space fire, Escape pause/cancel, Enter submit, Q/E skin
        float kx = key(275, 100) - key(276, 97), ky = key(273, 119) - key(274, 115);
        k.put("MoveHorizontal", kx);
        k.put("MoveVertical", ky);
        k.put("UIHorizontal", kx);
        k.put("UIVertical", ky);
        k.put("Fire", key(122, 32) + key(306, 0) > 0 ? 1f : 0f);
        k.put("Pause", key(27, 112));
        k.put("UISubmit", key(13, 32) + key(122, 0) > 0 ? 1f : 0f);
        k.put("UICancel", key(27, 8) + key(120, 0) > 0 ? 1f : 0f);
        k.put("UISkinChange", key(101, 0) - key(113, 0));
        boolean keyAny = cs.UnityEngine.Input.get_anyKey() && !cs.UnityEngine.Input.GetMouseButton(0);
        keyboard.sample(keyAny, keyAny && !prevKeyAny);
        prevKeyAny = keyAny;
        if (touch.anyDown) lastActive = touch;
        else if (keyboard.anyDown) lastActive = keyboard;
        system.update(t, k);
        p0.update(t, k);
    }

    private static float key(int a, int b) { return cs.UnityEngine.Input.GetKey(a) || b != 0 && cs.UnityEngine.Input.GetKey(b) ? 1f : 0f; }

    /** Rewired UI actions for the UGUI input module: the system player (menus, which assign no game player)
     *  and player 0 (unknown action names read 0) */
    public static float $action(String n) { float a = p0.GetAxisRaw(n), b = system.GetAxisRaw(n); return Math.abs(a) >= Math.abs(b) ? a : b; }
    public static boolean $actionDown(String n) { return p0.GetButtonDown(n) || system.GetButtonDown(n); }
    public static boolean $actionUp(String n) { return p0.GetButtonUp(n) || system.GetButtonUp(n); }
}
