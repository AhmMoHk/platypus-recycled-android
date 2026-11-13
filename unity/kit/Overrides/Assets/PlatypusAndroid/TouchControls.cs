// Platypus Reclayed Android port: clay on-screen controls (only during active gameplay).
//  - joystick base  Resources/1000045163.png  (hollow red clay ring, bottom-left, right above the lives display)
//  - joystick knob  Resources/1000045164.png  (blue clay disc, 47.5% of the ring, follows the thumb 360°, snaps back)
//  - fire button    Resources/1000045153.png  (red ring + yellow crosshair + blue dot, bottom-right on the same
//                   line as the joystick, tap / hold)
//  - pause button   Resources/pause_button.png (red clay ring + two yellow bars), top-right corner
// Size and opacity come from Options > Gameplay > Controls (TouchSettings). A held control turns darker and
// shrinks a little. Multi-touch with independent finger ids; input goes to the Rewired stand-in through
// PlatypusAndroid.TouchInput. Positions are worked out in screen-glass pixels and converted to drawn pixels, so the
// controls stay round when the Fullscreen screen option stretches the picture sideways. The joystick and the fire
// button stand on a line just above player 1's lives display (ship icon, number of lives, shields), whatever their
// size, so they never cover it; the joystick is centred over it as far as the screen edge allows.
using System.Collections.Generic;
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
            float line = bottom + m;                                     // the controls' bottom edge
            TrackLives();
            float hud = Mathf.Max(livesKnown ? livesTop : 0f, livesArea);   // the lives strip, or what is drawn above it
            if (hud > 0f) line = Mathf.Max(line, hud + unit * 0.025f);
            baseCenter = new Vector2(left + m + baseR, line + baseR);
            if (livesKnown || livesArea > 0f) baseCenter.x = Mathf.Max(livesKnown ? livesX : 0f, left + m * 0.4f + baseR);
            fireCenter = new Vector2(right - m - fireR, line + fireR);

            pauseCenter = new Vector2(right - m * 0.6f - pauseR, top - m * 0.6f - pauseR);   // top-right corner
        }

        // ---- player 1's lives display --------------------------------------------------------------------------------

        DisplayPlayerLives lives;
        int livesScene = int.MinValue;
        readonly List<RectTransform> livesParts = new List<RectTransform>();
        Camera livesCam;
        bool livesKnown;
        float livesTop, livesX;                                          // drawn-pixel top, glass-pixel centre
        float livesArea;                                                 // top of the whole lives strip (drawn pixels)
        static readonly Vector3[] corners = new Vector3[4];

        /// <summary>Where player 1's ship icon, lives and shields are drawn; the last place seen stays known while
        /// they are hidden (between lives), so the controls do not jump.</summary>
        void TrackLives()
        {
            Scene g = SceneManager.GetSceneByBuildIndex((int)BuildScenes.Gameplay);
            if (g.handle != livesScene || lives == null)
            {
                livesScene = g.handle;
                lives = null;
                livesKnown = false;
                livesArea = 0f;
                livesParts.Clear();
                foreach (DisplayPlayerLives d in FindObjectsByType<DisplayPlayerLives>(FindObjectsInactive.Include, FindObjectsSortMode.None))
                    if (d.gameObject.scene == g && d.AndroidPlayerIndex == 0) { lives = d; break; }
                if (lives == null) return;
                Component[] parts = { lives.AndroidIcon, lives.AndroidPracticeIcon, lives.AndroidLivesText,
                                      lives.GetComponentInChildren<DisplayPlayerShieldCount>(true) };
                foreach (Component c in parts) if (c != null && c.transform is RectTransform rt) livesParts.Add(rt);
                Canvas cv = lives.GetComponentInParent<Canvas>(true);
                livesCam = cv != null && cv.rootCanvas.renderMode != RenderMode.ScreenSpaceOverlay ? cv.rootCanvas.worldCamera : null;
            }
            if (lives.transform is RectTransform strip && strip.gameObject.activeInHierarchy)
            {
                strip.GetWorldCorners(corners);
                float t = RectTransformUtility.WorldToScreenPoint(livesCam, corners[1]).y;   // top-left corner
                if (t > 0f && t < Screen.height * 0.5f) livesArea = t;
            }
            float top = float.MinValue, minX = float.MaxValue, maxX = float.MinValue;
            foreach (RectTransform rt in livesParts)
            {
                if (rt == null || !rt.gameObject.activeInHierarchy) continue;
                rt.GetWorldCorners(corners);
                for (int i = 0; i < 4; i++)
                {
                    Vector2 sp = RectTransformUtility.WorldToScreenPoint(livesCam, corners[i]);
                    top = Mathf.Max(top, sp.y);
                    minX = Mathf.Min(minX, sp.x);
                    maxX = Mathf.Max(maxX, sp.x);
                }
            }
            if (top == float.MinValue || top > Screen.height * 0.5f) return;   // nothing shown (or not laid out yet)
            livesKnown = true;
            livesTop = top;
            livesX = (minX + maxX) * 0.5f * sx;
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
                    if (pauseId == -2 && (p - pauseCenter).magnitude <= pauseR * 1.4f) { pauseId = id; pauseStart = p; pauseStartTime = Time.unscaledTime; }
                    else if (fireId == -2 && (p - fireCenter).magnitude <= fireR * 1.5f) fireId = id;
                    else if (joyId == -2 && p.x < Screen.width * sx * 0.5f) joyId = id;
                }
                if (id == pauseId)
                {
                    // a tap on it pauses (lift the finger on the button); sliding off cancels
                    if (ended)
                    {
                        pauseId = -2;
                        if (ph == TouchPhase.Ended && (p - pauseCenter).magnitude <= pauseR * 1.6f && Time.unscaledTime - pauseStartTime < 1.2f)
                        { TouchInput.pause = true; pausePress = 1f; }
                        continue;
                    }
                    pauseSeen = true;
                    continue;
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
