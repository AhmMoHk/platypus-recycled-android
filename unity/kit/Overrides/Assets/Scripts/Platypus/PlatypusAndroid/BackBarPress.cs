// Platypus Reclayed Android port: the red of the pressed BACK in the bar at the bottom (see BackBar). The text is red
// exactly while a finger is on the button (a tap or a long press) and white again as soon as no finger is on it.
using UnityEngine;

namespace PlatypusAndroid
{
    /// <summary>The red of a pressed bar button.</summary>
    public class BackBarPress : MonoBehaviour
    {
        PText text;
        Material normal;
        bool red;
        Canvas canvas;

        void Update() => Set(FingerOn());

        void OnDisable() { if (red) Set(false); }

        bool FingerOn()
        {
            if (!(transform is RectTransform rt)) return false;
            if (canvas == null) canvas = GetComponentInParent<Canvas>();
            Camera cam = null;
            if (canvas != null && canvas.rootCanvas.renderMode != RenderMode.ScreenSpaceOverlay) cam = canvas.rootCanvas.worldCamera;
            if (Input.touchCount > 0)
            {
                for (int i = 0; i < Input.touchCount; i++)
                {
                    Touch t = Input.GetTouch(i);
                    if (t.phase == TouchPhase.Ended || t.phase == TouchPhase.Canceled) continue;
                    if (RectTransformUtility.RectangleContainsScreenPoint(rt, t.position, cam)) return true;
                }
                return false;
            }
            return Input.GetMouseButton(0) && RectTransformUtility.RectangleContainsScreenPoint(rt, Input.mousePosition, cam);
        }

        void Set(bool on)
        {
            if (on == red) return;
            if (text == null) text = GetComponentInChildren<PText>(true);
            if (text == null || GlobalAssets.Instance == null) return;
            if (on) normal = text.material;
            Material m = on ? GlobalAssets.Instance.letterMaterialRed : (normal != null ? normal : GlobalAssets.Instance.letterMaterialWhite);
            red = on;
            if (m == null || text.material == m) return;
            text.material = m;
            text.UpdateMaterial();
        }
    }
}
