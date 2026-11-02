package cs.UnityEngine;

/** Font asset; TTF data lives in data/unity/fonts/<file>_<pid>.ttf (used by the TMP FontEngine). */
public class Font extends UObject {
    int fontSize = 16;
    float lineHeight, ascent;
    boolean dynamic = true;
    Material material;
    Texture texture;
    public String $ttf;
    public String[] $names = new String[0];
    static cs.System.Delegate textureRebuilt;

    public Font() {}
    public Font(String name) { m_Name = name; }
    public static void add_textureRebuilt(cs.System.Action_1 a) { textureRebuilt = cs.System.Delegate.Combine(textureRebuilt, a); }
    public static void remove_textureRebuilt(cs.System.Action_1 a) { textureRebuilt = cs.System.Delegate.Remove(textureRebuilt, a); }
    public boolean get_dynamic() { return dynamic; }
    public int get_fontSize() { return fontSize; }
    public int get_lineHeight() { return Math.round(lineHeight); }
    public int get_ascent() { return Math.round(ascent); }
    public Material get_material() { return material; }
    public void set_material(Material m) { material = m; }
    public String[] get_fontNames() { return $names.clone(); }
    public boolean HasCharacter(char c) { return true; }
    public boolean HasCharacter(int c) { return true; }
    public void RequestCharactersInTexture(String s) {}
    public void RequestCharactersInTexture(String s, int size) {}
    public void RequestCharactersInTexture(String s, int size, int style) {}
    public static Font CreateDynamicFontFromOSFont(String name, int size) { Font f = new Font(name); f.fontSize = size; return f; }
    public static String[] GetOSInstalledFontNames() { return new String[0]; }
    @Override public void $loadAsset(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        fontSize = Math.round(Loader.f(d, "m_FontSize", 16));
        lineHeight = Loader.f(d, "m_LineSpacing", 0);
        ascent = Loader.f(d, "m_Ascent", 0);
        UObject m = ctx.ref(d.get("m_DefaultMaterial"));
        material = m instanceof Material ? (Material) m : null;
        UObject t = ctx.ref(d.get("m_Texture"));
        texture = t instanceof Texture ? (Texture) t : null;
        java.util.ArrayList<Object> n = Loader.l(d, "m_FontNames");
        $names = new String[n.size()];
        for (int i = 0; i < $names.length; i++) $names[i] = (String) n.get(i);
        if ($ref != null) $ttf = "unity/fonts/" + $ref.replace('#', '_') + ".ttf";
    }
}
