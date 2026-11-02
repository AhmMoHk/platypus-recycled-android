package cs.UnityEngine;

import java.util.HashMap;

/** Script classes that are replaced by native facades (Rewired input, platform services). */
final class Facades {
    private Facades() {}
    private static final HashMap<String, String> MAP = new HashMap<>();
    static {
        MAP.put("Rewired.InputManager", "cs.Rewired.InputManager");
        MAP.put("Rewired.Integration.UnityUI.RewiredEventSystem", "cs.Rewired.Integration.UnityUI.RewiredEventSystem");
        MAP.put("Rewired.Integration.UnityUI.RewiredStandaloneInputModule", "cs.Rewired.Integration.UnityUI.RewiredStandaloneInputModule");
        MAP.put("Rewired.Platforms.Switch.NintendoSwitchInputManager", "-");
    }
    static String scriptClass(String fullname) {
        String m = MAP.get(fullname);
        if (m != null) return m;
        return cs.System.Reflection.Assembly.javaName(fullname);
    }
}
