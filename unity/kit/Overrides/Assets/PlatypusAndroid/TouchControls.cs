// Platypus Reclayed Android port: clay on-screen controls (only during active gameplay).
//  - joystick base  Resources/1000045163.png  (hollow red clay ring, fixed bottom-left)
//  - joystick knob  Resources/1000045164.png  (blue clay disc, 47.5% of the ring, follows the thumb 360°, snaps back)
//  - fire button    Resources/1000045153.png  (red ring + yellow crosshair + blue dot, bottom-right, tap / hold, 0.92 press)
//  - pause button   top-right, opens the game's own pause menu
// Multi-touch with independent finger ids. Input goes to Rewired's stand-in through PlatypusAndroid.TouchInput.
using Platypus.Core;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

namespace PlatypusAndroid
{
    [DefaultExecutionOrder(-32100)]   // before the input stand-in samples, so a touch acts in the same frame
    public class TouchControls : MonoBehaviour
    {
        const float BaseSize = 0.48f;     // ring diameter, fraction of the screen height
        const float KnobScale = 0.475f;   // knob diameter / ring diameter
        const float FireSize = 0.34f;     // fire button diameter, fraction of the screen height
        const float PauseSize = 0.12f;
        const float PressScale = 0.92f;   // a held control shrinks a little ...
        static readonly Color Idle = new Color(1f, 1f, 1f, 1f);
        static readonly Color Held = new Color(0.62f, 0.62f, 0.62f, 1f);   // ... and its clay turns darker

        Canvas canvas;
        RectTransform baseRt, knobRt, fireRt, pauseRt;
        RawImage baseImg, knobImg, fireImg, pauseImg;
        float knobPress, firePress, pausePress;             // 0 = idle .. 1 = held, eased
        int joyId = -2, fireId = -2;
        Vector2 baseCenter, fireCenter, pauseCenter;
        float baseR, knobR, maxRadius, fireR, pauseR;

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
            baseRt = MakeImage("JoystickBase", "1000045163");
            knobRt = MakeImage("JoystickKnob", "1000045164");
            fireRt = MakeImage("FireButton", "1000045153");
            pauseRt = MakePause();
            baseImg = baseRt.GetComponent<RawImage>(); knobImg = knobRt.GetComponent<RawImage>();
            fireImg = fireRt.GetComponent<RawImage>(); pauseImg = pauseRt.GetComponent<RawImage>();
            SetVisible(false);
        }

        RectTransform MakeImage(string name, string res)
        {
            var go = new GameObject(name, typeof(RectTransform));
            go.transform.SetParent(transform, false);
            var img = go.AddComponent<RawImage>();
            img.texture = Resources.Load<Texture2D>(res);
            img.raycastTarget = false;
            var rt = (RectTransform)go.transform;
            rt.anchorMin = rt.anchorMax = Vector2.zero;
            rt.pivot = new Vector2(0.5f, 0.5f);
            return rt;
        }

        RectTransform MakePause()
        {
            var rt = MakeImage("PauseButton", "1000045163");             // the same clay ring, small
            foreach (float x in new[] { -0.17f, 0.17f })
            {
                var bar = new GameObject("Bar", typeof(RectTransform));
                bar.transform.SetParent(rt, false);
                var img = bar.AddComponent<Image>();
                img.color = new Color(1f, 0.86f, 0.25f, 1f);               // the crosshair's clay yellow
                img.raycastTarget = false;
                var b = (RectTransform)bar.transform;
                b.anchorMin = new Vector2(0.5f + x - 0.07f, 0.3f);
                b.anchorMax = new Vector2(0.5f + x + 0.07f, 0.7f);
                b.offsetMin = b.offsetMax = Vector2.zero;
            }
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
            joyId = fireId = -2;
            TouchInput.x = TouchInput.y = 0;
            TouchInput.fire = false;
        }

        void Layout()
        {
            Rect safe = Screen.safeArea;
            float unit = Screen.height;
            baseR = unit * BaseSize * 0.5f;
            knobR = baseR * KnobScale;
            maxRadius = baseR - knobR;                                   // the knob stays inside the ring
            fireR = unit * FireSize * 0.5f;
            pauseR = unit * PauseSize * 0.5f;
            float m = unit * 0.05f;
            baseCenter = new Vector2(safe.xMin + m + baseR, safe.yMin + m + baseR);
            fireCenter = new Vector2(safe.xMax - m - fireR, safe.yMin + m + fireR);
            pauseCenter = new Vector2(safe.xMax - m * 0.6f - pauseR, safe.yMax - m * 0.6f - pauseR);
            float s = canvas.scaleFactor > 0 ? 1f / canvas.scaleFactor : 1f;
            Place(baseRt, baseCenter, baseR * 2, s);
            Place(pauseRt, pauseCenter, pauseR * 2 * Mathf.Lerp(1f, PressScale, pausePress), s);
        }

        static void Place(RectTransform rt, Vector2 center, float size, float s)
        {
            rt.anchoredPosition = center * s;
            rt.sizeDelta = new Vector2(size, size) * s;
        }

        void Update()
        {
            bool show = InGameplay();
            TouchInput.gameplayVisible = show;
            SetVisible(show);
            if (!show) return;
            Layout();

            bool joySeen = false, fireSeen = false;
            Vector2 knob = Vector2.zero;
            int n = Input.touchCount;
            bool mouse = Application.isEditor && n == 0 && (Input.GetMouseButton(0) || Input.GetMouseButtonUp(0));
            for (int i = 0; i < (mouse ? 1 : n); i++)
            {
                int id; Vector2 p; TouchPhase ph;
                if (mouse) { id = -1; p = Input.mousePosition; ph = Input.GetMouseButtonDown(0) ? TouchPhase.Began : Input.GetMouseButtonUp(0) ? TouchPhase.Ended : TouchPhase.Moved; }
                else { var t = Input.GetTouch(i); id = t.fingerId; p = t.position; ph = t.phase; }
                bool ended = ph == TouchPhase.Ended || ph == TouchPhase.Canceled;
                if (ph == TouchPhase.Began)
                {
                    if ((p - pauseCenter).magnitude <= pauseR * 1.3f) { TouchInput.pause = true; pausePress = 1f; continue; }
                    if (fireId == -2 && (p - fireCenter).magnitude <= fireR * 1.5f) fireId = id;
                    else if (joyId == -2 && p.x < Screen.width * 0.5f) joyId = id;
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
            TouchInput.x = maxRadius > 0 ? knob.x / maxRadius : 0;
            TouchInput.y = maxRadius > 0 ? knob.y / maxRadius : 0;
            TouchInput.fire = fireId != -2;
            // press feedback: eased in ~50 ms, out ~120 ms
            float dt = Time.unscaledDeltaTime;
            knobPress = Ease(knobPress, joyId != -2, dt);
            firePress = Ease(firePress, TouchInput.fire, dt);
            pausePress = Ease(pausePress, false, dt);
            float s = canvas.scaleFactor > 0 ? 1f / canvas.scaleFactor : 1f;
            Place(knobRt, baseCenter + knob, knobR * 2 * Mathf.Lerp(1f, PressScale, knobPress), s);
            Place(fireRt, fireCenter, fireR * 2 * Mathf.Lerp(1f, PressScale, firePress), s);
            knobImg.color = Color.Lerp(Idle, Held, knobPress);
            baseImg.color = Color.Lerp(Idle, Held, knobPress * 0.5f);
            fireImg.color = Color.Lerp(Idle, Held, firePress);
            pauseImg.color = Color.Lerp(Idle, Held, pausePress);
        }

        static float Ease(float v, bool held, float dt) => Mathf.MoveTowards(v, held ? 1f : 0f, dt * (held ? 20f : 8f));

        void OnApplicationPause(bool paused) { if (paused) Release(); }
    }
}
