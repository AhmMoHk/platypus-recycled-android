package cs.UnityEngine;

public class SpriteMask extends Renderer {
    Sprite sprite;
    float alphaCutoff = .5f;
    public Sprite get_sprite() { return sprite; }
    public void set_sprite(Sprite s) { sprite = s; }
    public float get_alphaCutoff() { return alphaCutoff; }
    public void set_alphaCutoff(float a) { alphaCutoff = a; }
    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) {
        super.$load(d, ctx);
        UObject s = ctx.ref(d.get("m_Sprite"));
        sprite = s instanceof Sprite ? (Sprite) s : null;
        alphaCutoff = Loader.f(d, "m_MaskAlphaCutoff", .5f);
    }
}
