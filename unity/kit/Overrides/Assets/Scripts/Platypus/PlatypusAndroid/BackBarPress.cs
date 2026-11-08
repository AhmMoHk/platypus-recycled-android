// Platypus Reclayed Android port: the red of the pressed BACK in the bar at the bottom (see BackBar).
using UnityEngine;
using UnityEngine.EventSystems;

namespace PlatypusAndroid
{
    /// <summary>The red of a pressed bar button.</summary>
    public class BackBarPress : MonoBehaviour, IPointerDownHandler, IPointerUpHandler
    {
        PText text;
        Material normal;
        bool held, red;
        float until;

        public void OnPointerDown(PointerEventData e) { held = true; Set(true); }

        public void OnPointerUp(PointerEventData e) { held = false; until = Time.unscaledTime + 0.15f; }

        public void Flash() { until = Time.unscaledTime + 0.3f; Set(true); }

        void Update()
        {
            if (red && !held && Time.unscaledTime >= until) Set(false);
        }

        void OnDisable() { held = false; if (red) Set(false); }

        void Set(bool on)
        {
            if (text == null) text = GetComponentInChildren<PText>(true);
            if (text == null || GlobalAssets.Instance == null) return;
            if (on && !red) normal = text.material;
            Material m = on ? GlobalAssets.Instance.letterMaterialRed : (normal != null ? normal : GlobalAssets.Instance.letterMaterialWhite);
            red = on;
            if (m == null || text.material == m) return;
            text.material = m;
            text.UpdateMaterial();
        }
    }
}
