package cs.UnityEngine.Experimental.Rendering;

public final class GraphicsFormatUtility {
    private GraphicsFormatUtility() {}
    public static boolean IsCrunchFormat(int textureFormat) { return textureFormat == 28 || textureFormat == 29 || textureFormat == 64 || textureFormat == 65; }
}
