// Platypus Reclayed Android port: taps on the game's menus.
// The game's own pointer support is built for a PC mouse (hover to select, menus that switch their raycasters off
// when the mouse is idle, invisible pause-menu buttons that catch the pointer during play). On a phone this module
// handles every touch itself:
//   finger down  -> the button under the finger is highlighted and selected (what hovering does on PC)
//   finger up    -> if it is still on the same button and has not wandered off, the button is pressed
// The top-most interactive, visible button wins; the search uses the game's UI raycasters and, if they report
// nothing, the buttons' own rectangles. While the joystick and fire button are on screen (active play) touches
// belong to them and never reach the menus (no stray selection sounds).
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;

namespace PlatypusAndroid
{
    public static class TouchUI
    {
        sealed class Track
        {
            public Vector2 start;
            public float time;
            public GameObject target;
            public PointerEventData data;
        }

        static readonly Dictionary<int, Track> tracks = new Dictionary<int, Track>();
        static readonly List<RaycastResult> hits = new List<RaycastResult>();
        static readonly List<CanvasGroup> groups = new List<CanvasGroup>();

        public static bool Active => Application.isMobilePlatform;

        public static void Process(EventSystem es)
        {
            if (es == null) return;
            bool playing = TouchInput.gameplayVisible;
            int n = Input.touchCount;
            for (int i = 0; i < n; i++)
            {
                Touch t = Input.GetTouch(i);
                switch (t.phase)
                {
                    case TouchPhase.Began:
                        if (playing) break;
                        var tr = new Track { start = t.position, time = Time.unscaledTime };
                        tr.target = Find(es, t.position, t.fingerId, out tr.data, out string how);
                        tracks[t.fingerId] = tr;
                        Debug.Log($"[Tap] down {t.position.x:0},{t.position.y:0} on {Path(tr.target)} ({how}, {hits.Count} hits{(hits.Count > 0 ? ", top " + hits[0].gameObject.name : "")})");
                        if (tr.target != null)
                        {
                            ExecuteEvents.Execute(tr.target, tr.data, ExecuteEvents.pointerEnterHandler);
                            ExecuteEvents.Execute(tr.target, tr.data, ExecuteEvents.pointerDownHandler);
                        }
                        break;
                    case TouchPhase.Ended:
                    case TouchPhase.Canceled:
                        if (!tracks.TryGetValue(t.fingerId, out Track tk)) break;
                        tracks.Remove(t.fingerId);
                        if (tk.target == null) break;
                        tk.data.position = t.position;
                        ExecuteEvents.Execute(tk.target, tk.data, ExecuteEvents.pointerUpHandler);
                        float slop = Mathf.Max(40f, Screen.height * 0.08f);
                        bool tap = t.phase == TouchPhase.Ended && !playing && (t.position - tk.start).magnitude <= slop
                                   && Time.unscaledTime - tk.time < 1.5f;
                        if (tap)
                        {
                            GameObject now = Find(es, t.position, t.fingerId, out _, out _);
                            if (now == tk.target || (t.position - tk.start).magnitude < slop * 0.5f)
                            {
                                Debug.Log("[Tap] click " + Path(tk.target));
                                if (!ExecuteEvents.Execute(tk.target, tk.data, ExecuteEvents.pointerClickHandler))
                                    ExecuteEvents.Execute(tk.target, new BaseEventData(es), ExecuteEvents.submitHandler);
                            }
                        }
                        if (tk.target != null) ExecuteEvents.Execute(tk.target, tk.data, ExecuteEvents.pointerExitHandler);
                        break;
                }
            }
            if (n == 0 && tracks.Count > 0) tracks.Clear();
        }

        static GameObject Find(EventSystem es, Vector2 pos, int id, out PointerEventData data, out string how)
        {
            data = new PointerEventData(es) { position = pos, pressPosition = pos, button = PointerEventData.InputButton.Left, pointerId = id, clickCount = 1, eligibleForClick = true };
            hits.Clear();
            es.RaycastAll(data, hits);
            foreach (RaycastResult h in hits)
            {
                Selectable sel = h.gameObject.GetComponentInParent<Selectable>();
                if (sel != null && Usable(sel))
                {
                    data.pointerCurrentRaycast = h;
                    data.pointerPressRaycast = h;
                    how = "raycast";
                    return sel.gameObject;
                }
            }
            // no usable hit (a raycaster switched off, an invisible panel on top): the buttons' own rectangles
            Selectable best = null;
            float bestOverlay = -1f, bestDepth = float.MinValue, bestOrder = float.MinValue, bestGraphic = float.MinValue;
            foreach (Selectable sel in Selectable.allSelectablesArray)
            {
                if (sel == null || !Usable(sel)) continue;
                var rt = sel.transform as RectTransform;
                Canvas canvas = sel.GetComponentInParent<Canvas>();
                if (rt == null || canvas == null) continue;
                Canvas root = canvas.rootCanvas;
                Camera cam = root.renderMode == RenderMode.ScreenSpaceOverlay ? null : root.worldCamera;
                if (!RectTransformUtility.RectangleContainsScreenPoint(rt, pos, cam)) continue;
                float overlay = cam == null ? 1f : 0f, depth = cam == null ? 0f : cam.depth;
                float order = root.sortingOrder, graphic = sel.targetGraphic != null ? sel.targetGraphic.depth : 0;
                bool better = overlay > bestOverlay || (overlay == bestOverlay && (depth > bestDepth || (depth == bestDepth &&
                              (order > bestOrder || (order == bestOrder && graphic > bestGraphic)))));
                if (!better) continue;
                best = sel; bestOverlay = overlay; bestDepth = depth; bestOrder = order; bestGraphic = graphic;
            }
            how = best != null ? "rect" : "none";
            return best != null ? best.gameObject : null;
        }

        static bool Usable(Selectable sel)
        {
            if (!sel.isActiveAndEnabled || !sel.IsInteractable()) return false;
            sel.GetComponentsInParent(false, groups);
            float alpha = 1f;
            foreach (CanvasGroup g in groups)
            {
                if (!g.enabled) continue;
                alpha *= g.alpha;
                if (g.ignoreParentGroups) break;
            }
            return alpha > 0.05f;
        }

        static string Path(GameObject go)
        {
            if (go == null) return "nothing";
            string p = go.name;
            for (Transform t = go.transform.parent; t != null && p.Length < 120; t = t.parent) p = t.name + "/" + p;
            return p;
        }
    }
}
