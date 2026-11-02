package cs.UnityEngine;

public final class ColorUtility {
    private ColorUtility() {}
    private static final String[][] NAMED = {{"red", "FF0000FF"}, {"cyan", "00FFFFFF"}, {"blue", "0000FFFF"}, {"darkblue", "0000A0FF"},
        {"lightblue", "ADD8E6FF"}, {"purple", "800080FF"}, {"yellow", "FFFF00FF"}, {"lime", "00FF00FF"}, {"fuchsia", "FF00FFFF"},
        {"white", "FFFFFFFF"}, {"silver", "C0C0C0FF"}, {"grey", "808080FF"}, {"black", "000000FF"}, {"orange", "FFA500FF"},
        {"brown", "A52A2AFF"}, {"maroon", "800000FF"}, {"green", "008000FF"}, {"olive", "808000FF"}, {"navy", "000080FF"},
        {"teal", "008080FF"}, {"aqua", "00FFFFFF"}, {"magenta", "FF00FFFF"}};

    public static boolean TryParseHtmlString(String s, Color out) {
        if (s == null || s.isEmpty()) return false;
        String h;
        if (s.charAt(0) == '#') h = s.substring(1);
        else {
            h = null;
            for (String[] n : NAMED) if (n[0].equalsIgnoreCase(s)) h = n[1];
            if (h == null) return false;
        }
        for (int i = 0; i < h.length(); i++) if (Character.digit(h.charAt(i), 16) < 0) return false;
        int r, g, b, a = 255;
        switch (h.length()) {
            case 3: case 4:
                r = Character.digit(h.charAt(0), 16) * 17; g = Character.digit(h.charAt(1), 16) * 17; b = Character.digit(h.charAt(2), 16) * 17;
                if (h.length() == 4) a = Character.digit(h.charAt(3), 16) * 17;
                break;
            case 6: case 8:
                r = Integer.parseInt(h.substring(0, 2), 16); g = Integer.parseInt(h.substring(2, 4), 16); b = Integer.parseInt(h.substring(4, 6), 16);
                if (h.length() == 8) a = Integer.parseInt(h.substring(6, 8), 16);
                break;
            default: return false;
        }
        out.r = r / 255f; out.g = g / 255f; out.b = b / 255f; out.a = a / 255f;
        return true;
    }
    private static int b(float v) { return Math.round(Mathf.Clamp01(v) * 255f); }
    public static String ToHtmlStringRGB(Color c) { return String.format("%02X%02X%02X", b(c.r), b(c.g), b(c.b)); }
    public static String ToHtmlStringRGBA(Color c) { return String.format("%02X%02X%02X%02X", b(c.r), b(c.g), b(c.b), b(c.a)); }
}
