package cs.UnityEngine;

public class Display {
    private static final Display main = new Display();
    public static Display[] displays = {main};
    public static Display get_main() { return main; }
    public int get_renderingWidth() { return Screen.width; }
    public int get_renderingHeight() { return Screen.height; }
    public int get_systemWidth() { return Screen.width; }
    public int get_systemHeight() { return Screen.height; }
    public boolean get_active() { return true; }
    public void Activate() {}
    public static Vector3 RelativeMouseAt(Vector3 p) { return new Vector3(p.x, p.y, 0); }
}
