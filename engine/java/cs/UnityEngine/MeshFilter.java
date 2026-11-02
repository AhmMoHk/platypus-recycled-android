package cs.UnityEngine;

public class MeshFilter extends Component {
    Mesh mesh;
    public Mesh get_sharedMesh() { return mesh; }
    public void set_sharedMesh(Mesh m) { mesh = m; }
    public Mesh get_mesh() { if (mesh == null) mesh = new Mesh(); return mesh; }
    public void set_mesh(Mesh m) { mesh = m; }
    @Override public void $load(java.util.HashMap<String, Object> d, Loader.Ctx ctx) { UObject m = ctx.ref(d.get("m_Mesh")); mesh = m instanceof Mesh ? (Mesh) m : null; }
    @Override public void $cloneFrom(Component s, Eng.CloneCtx ctx) { mesh = ((MeshFilter) s).mesh; }
}
