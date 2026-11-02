package cs.UnityEngine;

final class KeyNames {
    private KeyNames() {}
    static int code(String n) {
        if (n == null) return 0;
        n = n.toLowerCase();
        if (n.length() == 1) return n.charAt(0);
        switch (n) {
            case "space": return 32; case "return": case "enter": return 13; case "escape": return 27; case "backspace": return 8;
            case "tab": return 9; case "up": return 273; case "down": return 274; case "right": return 275; case "left": return 276;
            case "left shift": return 304; case "right shift": return 303; case "left ctrl": return 306; case "right ctrl": return 305;
            case "left alt": return 308; case "right alt": return 307;
            default:
                if (n.startsWith("f") && n.length() <= 3) try { return 281 + Integer.parseInt(n.substring(1)); } catch (NumberFormatException ignored) {}
                return 0;
        }
    }
}
