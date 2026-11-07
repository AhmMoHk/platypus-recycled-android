// Platypus Reclayed Android port: clay on-screen controls (only during active gameplay).
//  - joystick base  Resources/1000045163.png  (hollow red clay ring, fixed bottom-left)
//  - joystick knob  Resources/1000045164.png  (blue clay disc, 47.5% of the ring, follows the thumb 360°, snaps back)
//  - fire button    Resources/1000045153.png  (red ring + yellow crosshair + blue dot, bottom-right, tap / hold)
//  - pause button   Resources/pause_button.png (red clay ring + two yellow bars), just above player 1's lives
// Size and opacity come from Options > Gameplay > Controls (TouchSettings). A held control turns darker and
// shrinks a little. Multi-touch with independent finger ids; input goes to the Rewired stand-in through
// PlatypusAndroid.TouchInput. Positions are worked out in screen-glass pixels and converted to drawn pixels, so the
// controls stay round when the Fullscreen screen option stretches the picture sideways.
using System.Reflection;
using Platypus.Core;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

namespace PlatypusAndroid
{
    [DefaultExecutionOrder(-32100)]   // before the input stand-in samples, so a touch acts in the same frame
    public class TouchControls : MonoBehaviour
    {
        const float BaseSize = 0.36f;     // ring diameter at 100%, fraction of the screen height
        const float KnobScale = 0.475f;   // knob diameter / ring diameter
        const float FireSize = 0.26f;     // fire button diameter at 100%
        const float PauseSize = 0.11f;    // pause button diameter at 100%
        const float PressScale = 0.92f;   // a held control shrinks a little ...
        const float HeldShade = 0.62f;    // ... and its clay turns darker

        Canvas canvas;
        RectTransform baseRt, knobRt, fireRt, pauseRt;
        RawImage baseImg, knobImg, fireImg, pauseImg;
        float knobPress, firePress, pausePress;             // 0 = idle .. 1 = held, eased
        int joyId = -2, fireId = -2, pauseId = -2;
        Vector2 pauseStart; float pauseStartTime;
        Vector2 baseCenter, fireCenter, pauseCenter;        // glass pixels
        float baseR, knobR, maxRadius, fireR, pauseR, sx = 1f;

        // player 1's lives counter in the HUD (the pause button sits just above it)
        RectTransform livesGroup; Canvas livesCanvas; int livesScene = -1; float livesRetry;
        static readonly FieldInfo playerIndexField = typeof(DisplayPlayerLives).GetField("m_playerIndex", BindingFlags.NonPublic | BindingFlags.Instance);
        static readonly Vector3[] corners = new Vector3[4];

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
        static void Boot()
        {
            var go = new GameObject("[TouchControls]");
            DontDestroyOnLoad(go);
            go.AddComponent<TouchControls>();
            Input.multiTouchEnabled = true;
        }

        void Awake()
        {
            canvas = gameObject.AddComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            canvas.sortingOrder = 32000;
            baseRt = MakeImage("JoystickBase", "1000045163", out baseImg);
            knobRt = MakeImage("JoystickKnob", "1000045164", out knobImg);
            fireRt = MakeImage("FireButton", "1000045153", out fireImg);
            pauseRt = MakeImage("PauseButton", "pause_button", out pauseImg);
            SetVisible(false);
        }

        RectTransform MakeImage(string name, string res, out RawImage img)
        {
            var go = new GameObject(name, typeof(RectTransform));
            go.transform.SetParent(transform, false);
            img = go.AddComponent<RawImage>();
            img.texture = Resources.Load<Texture2D>(res);
            img.raycastTarget = false;
            var rt = (RectTransform)go.transform;
            rt.anchorMin = rt.anchorMax = Vector2.zero;
            rt.pivot = new Vector2(0.5f, 0.5f);
            return rt;
        }

        static bool InGameplay()
        {
            // scenes load additively next to the loading screen, so ask whether the gameplay scene is loaded
            if (!SceneManager.GetSceneByBuildIndex((int)BuildScenes.Gameplay).isLoaded) return false;
            if (Globals.paused || LoadingScreen.IsLoading || LoadingScreen.IsBlockingView) return false;
            return true;
        }

        void SetVisible(bool v)
        {
            if (baseRt.gameObject.activeSelf == v) return;
            baseRt.gameObject.SetActive(v);
            knobRt.gameObject.SetActive(v);
            fireRt.gameObject.SetActive(v);
            pauseRt.gameObject.SetActive(v);
            if (!v) Release();
        }

        void Release()
        {
            joyId = fireId = pauseId = -2;
            TouchInput.x = TouchInput.y = 0;
            TouchInput.fire = false;
        }

        // ---- layout ------------------------------------------------------------------------------------------------

        void FindLives()
        {
            Scene s = SceneManager.GetSceneByBuildIndex((int)BuildScenes.Gameplay);
            int handle = s.handle;
            if (livesScene == handle && (livesGroup != null || Time.unscaledTime < livesRetry)) return;
            livesScene = handle;
            livesRetry = Time.unscaledTime + 1f;
            livesGroup = null;
            foreach (DisplayPlayerLives d in FindObjectsOfType<DisplayPlayerLives>())
            {
                if (playerIndexField != null && (int)playerIndexField.GetValue(d) != 0) continue;
                Transform g = d.transform.Find("Lives Counter");
                livesGroup = (RectTransform)(g != null ? g : d.transform);
                Canvas c = d.GetComponentInParent<Canvas>();
                livesCanvas = c != null ? c.rootCanvas : null;
                break;
            }
        }

        /// <summary>The on-screen box (drawn pixels) of the visible parts of player 1's lives counter.</summary>
        bool LivesRect(out Rect r)
        {
            r = default;
            if (livesGroup == null || livesCanvas == null || !livesGroup.gameObject.activeInHierarchy) return false;
            Camera cam = livesCanvas.renderMode == RenderMode.ScreenSpaceOverlay ? null : livesCanvas.worldCamera;
            float x0 = float.MaxValue, y0 = float.MaxValue, x1 = float.MinValue, y1 = float.MinValue;
            bool any = false;
            for (int i = 0; i < livesGroup.childCount; i++)
            {
                var child = livesGroup.GetChild(i) as RectTransform;
                if (child == null || !child.gameObject.activeInHierarchy) continue;
                child.GetWorldCorners(corners);
                for (int k = 0; k < 4; k++)
                {
                    Vector2 p = RectTransformUtility.WorldToScreenPoint(cam, corners[k]);
                    x0 = Mathf.Min(x0, p.x); y0 = Mathf.Min(y0, p.y); x1 = Mathf.Max(x1, p.x); y1 = Mathf.Max(y1, p.y);
                }
                any = true;
            }
            if (!any) return false;
            r = Rect.MinMaxRect(x0, y0, x1, y1);
            return true;
        }

        void Layout()
        {
            sx = Quality.Stretch;                                        // drawn pixel -> glass pixel, sideways
            float size = TouchSettings.Size;
            Rect safe = Screen.safeArea;
            float left = safe.xMin * sx, right = safe.xMax * sx, bottom = safe.yMin, top = safe.yMax;
            float unit = Screen.height;
            baseR = unit * BaseSize * size * 0.5f;
            knobR = baseR * KnobScale;
            maxRadius = baseR - knobR;                                   // the knob stays inside the ring
            fireR = unit * FireSize * size * 0.5f;
            pauseR = unit * PauseSize * size * 0.5f;
            float m = unit * 0.05f;
            baseCenter = new Vector2(left + m + baseR, bottom + m + baseR);
            fireCenter = new Vector2(right - m - fireR, bottom + m + fireR);

            FindLives();
            if (LivesRect(out Rect lives))
                pauseCenter = new Vector2((lives.xMin + lives.xMax) * 0.5f * sx, lives.yMax + unit * 0.015f + pauseR);
            else
                pauseCenter = new Vector2(left + m * 0.6f + pauseR, top - m * 0.6f - pauseR);   // no HUD yet: top-left
            pauseCenter.x = Mathf.Clamp(pauseCenter.x, left + pauseR, right - pauseR);
            pauseCenter.y = Mathf.Clamp(pauseCenter.y, bottom + pauseR, top - pauseR);
        }

        void Place(RectTransform rt, Vector2 center, float diameter)
        {
            float s = canvas.scaleFactor > 0 ? 1f / canvas.scaleFactor : 1f;
            rt.anchoredPosition = new Vector2(center.x / sx, center.y) * s;
            rt.sizeDelta = new Vector2(diameter / sx, diameter) * s;
        }

        static Color Shade(float press, float alpha)
        {
            float v = Mathf.Lerp(1f, HeldShade, press);
            return new Color(v, v, v, alpha);
        }

        // ---- input -------------------------------------------------------------------------------------------------

        void Update()
        {
            bool show = InGameplay();
            TouchInput.gameplayVisible = show;
            SetVisible(show);
            if (!show) return;
            Layout();

            bool joySeen = false, fireSeen = false, pauseSeen = false;
            Vector2 knob = Vector2.zero;
            int n = Input.touchCount;
            bool mouse = Application.isEditor && n == 0 && (Input.GetMouseButton(0) || Input.GetMouseButtonUp(0));
            for (int i = 0; i < (mouse ? 1 : n); i++)
            {
                int id; Vector2 p; TouchPhase ph;
                if (mouse) { id = -1; p = Input.mousePosition; ph = Input.GetMouseButtonDown(0) ? TouchPhase.Began : Input.GetMouseButtonUp(0) ? TouchPhase.Ended : TouchPhase.Moved; }
                else { var t = Input.GetTouch(i); id = t.fingerId; p = t.position; ph = t.phase; }
                p.x *= sx;                                               // glass pixels
                bool ended = ph == TouchPhase.Ended || ph == TouchPhase.Canceled;
                if (ph == TouchPhase.Began)
                {
                    if (pauseId == -2 && (p - pauseCenter).magnitude <= pauseR * 1.1f) { pauseId = id; pauseStart = p; pauseStartTime = Time.unscaledTime; }
                    else if (fireId == -2 && (p - fireCenter).magnitude <= fireR * 1.5f) fireId = id;
                    else if (joyId == -2 && p.x < Screen.width * sx * 0.5f) joyId = id;
                }
                if (id == pauseId)
                {
                    // The pause button sits inside the joystick's half of the screen: a tap on it pauses, a finger
                    // that lands on it and slides is the joystick.
                    if (ended)
                    {
                        pauseId = -2;
                        if (ph == TouchPhase.Ended && Time.unscaledTime - pauseStartTime < 0.8f) { TouchInput.pause = true; pausePress = 1f; }
                        continue;
                    }
                    if ((p - pauseStart).magnitude > Screen.height * 0.04f && joyId == -2) { pauseId = -2; joyId = id; }
                    else { pauseSeen = true; continue; }
                }
                if (id == joyId)
                {
                    if (ended) { joyId = -2; continue; }
                    joySeen = true;
                    Vector2 d = p - baseCenter;
                    if (d.magnitude > maxRadius) d = d.normalized * maxRadius;
                    knob = d;
                }
                else if (id == fireId)
                {
                    if (ended) { fireId = -2; continue; }
                    fireSeen = true;
                }
            }
            if (!joySeen) joyId = -2;
            if (!fireSeen) fireId = -2;
            if (!pauseSeen) pauseId = -2;
            TouchInput.x = maxRadius > 0 ? knob.x / maxRadius : 0;
            TouchInput.y = maxRadius > 0 ? knob.y / maxRadius : 0;
            TouchInput.fire = fireId != -2;

            // press feedback: eased in ~50 ms, out ~120 ms
            float dt = Time.unscaledDeltaTime;
            knobPress = Ease(knobPress, joyId != -2, dt);
            firePress = Ease(firePress, TouchInput.fire, dt);
            pausePress = Ease(pausePress, pauseId != -2, dt);
            Place(baseRt, baseCenter, baseR * 2);
            Place(knobRt, baseCenter + knob, knobR * 2 * Mathf.Lerp(1f, PressScale, knobPress));
            Place(fireRt, fireCenter, fireR * 2 * Mathf.Lerp(1f, PressScale, firePress));
            Place(pauseRt, pauseCenter, pauseR * 2 * Mathf.Lerp(1f, PressScale, pausePress));
            float a = TouchSettings.Opacity;
            knobImg.color = Shade(knobPress, a);
            baseImg.color = Shade(knobPress * 0.5f, a);
            fireImg.color = Shade(firePress, a);
            pauseImg.color = Shade(pausePress, TouchSettings.PauseOpacity);
        }

        static float Ease(float v, bool held, float dt) => Mathf.MoveTowards(v, held ? 1f : 0f, dt * (held ? 20f : 8f));

        void OnApplicationPause(bool paused) { if (paused) Release(); }
    }
}
