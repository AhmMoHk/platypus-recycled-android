package cs.UnityEngine;

public interface ICanvasRaycastFilter {
    boolean IsRaycastLocationValid(Vector2 sp, Camera eventCamera);
}
