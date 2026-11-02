package cs.UnityEngine;

public class MeshRenderer extends Renderer {
    public Mesh $mesh() {
        MeshFilter f = gameObject == null ? null : (MeshFilter) gameObject.GetComponent(cs.System.Type.of(MeshFilter.class));
        return f == null ? null : f.mesh;
    }
    public Mesh get_additionalVertexStreams() { return null; }
    public void set_additionalVertexStreams(Mesh m) {}
}
