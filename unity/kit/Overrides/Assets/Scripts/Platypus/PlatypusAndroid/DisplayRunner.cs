// Platypus Reclayed Android port: per-frame display housekeeping.
//  - A black backdrop camera drawn first every frame. On a phone the screen buffer holds leftover garbage unless
//    something clears it, and the loading screen / intro film cameras never clear, which showed as TV-static
//    noise and smeared "Loading" text around the Claymatic film. Everything outside the game's 16:9 picture is
//    therefore pure black.
//  - Every scene camera is drawn without HDR: the game has no post effects, so HDR only cost a full-screen float
//    buffer and a copy per frame (a lot of memory traffic at 90-120 FPS on a phone).
//  - Asks Android for a display refresh rate matching the FPS limit (phones keep apps at 60 Hz otherwise).
using System;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace PlatypusAndroid
{
    public class DisplayRunner : MonoBehaviour
    {
        static DisplayRunner instance;
        static int wantedHz, appliedHz = -1;
        static float refreshAt = -1f;
        Camera backdrop;
        int cameraCount = -1;
        Camera[] cameras = new Camera[16];

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
        static void Boot()
        {
            Quality.CaptureNative();
            var go = new GameObject("[Display]");
            DontDestroyOnLoad(go);
            go.hideFlags = HideFlags.HideInHierarchy;
            instance = go.AddComponent<DisplayRunner>();
        }

        void Awake()
        {
            backdrop = gameObject.AddComponent<Camera>();
            backdrop.depth = -1000f;                       // before every game camera
            backdrop.clearFlags = CameraClearFlags.SolidColor;
            backdrop.backgroundColor = Color.black;
            backdrop.cullingMask = 0;                      // draws nothing, only clears
            backdrop.orthographic = true;
            backdrop.useOcclusionCulling = false;
            backdrop.allowHDR = false;
            backdrop.allowMSAA = false;
            backdrop.rect = new Rect(0f, 0f, 1f, 1f);
            SceneManager.sceneLoaded += (s, m) => cameraCount = -1;
        }

        void LateUpdate()
        {
            int n = Camera.allCamerasCount;
            if (n != cameraCount)
            {
                cameraCount = n;
                if (cameras.Length < n) cameras = new Camera[n + 8];
                int got = Camera.GetAllCameras(cameras);
                for (int i = 0; i < got; i++)
                {
                    Camera c = cameras[i];
                    if (c == null || c == backdrop) continue;
                    c.allowHDR = false;
                    c.allowMSAA = false;
                    cameras[i] = null;
                }
            }
            if (refreshAt >= 0f && Time.unscaledTime >= refreshAt)
            {
                refreshAt = -1f;
                ApplyRefreshRate(wantedHz);
            }
        }

        /// <summary>Asks for the display mode whose refresh rate fits the FPS limit (applied a moment later, after
        /// any resolution change has gone through).</summary>
        public static void RequestRefreshRate(int fps, bool again = false)
        {
            if (Application.platform != RuntimePlatform.Android || fps <= 0) return;
            if (fps == wantedHz && appliedHz == fps && !again) return;
            wantedHz = fps;
            appliedHz = -1;
            refreshAt = Time.unscaledTime + 0.3f;
        }

        static void ApplyRefreshRate(int fps)
        {
            appliedHz = fps;
            try
            {
                using (var player = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
                {
                    AndroidJavaObject activity = player.GetStatic<AndroidJavaObject>("currentActivity");
                    activity.Call("runOnUiThread", new AndroidJavaRunnable(() => SetDisplayMode(activity, fps)));
                }
            }
            catch (Exception e) { Debug.LogWarning("[Display] refresh rate: " + e.Message); }
        }

        // Runs on Android's UI thread.
        static void SetDisplayMode(AndroidJavaObject activity, int fps)
        {
            try
            {
                using (var window = activity.Call<AndroidJavaObject>("getWindow"))
                using (var wm = activity.Call<AndroidJavaObject>("getWindowManager"))
                using (var display = wm.Call<AndroidJavaObject>("getDefaultDisplay"))
                using (var current = display.Call<AndroidJavaObject>("getMode"))
                {
                    int cw = current.Call<int>("getPhysicalWidth"), ch = current.Call<int>("getPhysicalHeight");
                    AndroidJavaObject[] modes = display.Call<AndroidJavaObject[]>("getSupportedModes");
                    int bestId = -1, topId = -1;
                    float bestHz = float.MaxValue, topHz = 0f;
                    string list = "";
                    foreach (AndroidJavaObject m in modes)
                    {
                        int id = m.Call<int>("getModeId");
                        float hz = m.Call<float>("getRefreshRate");
                        bool same = m.Call<int>("getPhysicalWidth") == cw && m.Call<int>("getPhysicalHeight") == ch;
                        m.Dispose();
                        list += $" {hz:0}";
                        if (!same) continue;
                        if (hz > topHz) { topHz = hz; topId = id; }
                        if (hz >= fps - 1f && hz < bestHz) { bestHz = hz; bestId = id; }   // the slowest mode that still fits
                    }
                    if (bestId < 0) { bestId = topId; bestHz = topHz; }
                    if (bestId < 0) return;
                    using (var lp = window.Call<AndroidJavaObject>("getAttributes"))
                    {
                        lp.Set("preferredDisplayModeId", bestId);
                        lp.Set("preferredRefreshRate", bestHz);
                        window.Call("setAttributes", lp);
                    }
                    Debug.Log($"[Display] {fps} FPS -> display mode {bestId} at {bestHz:0} Hz (modes:{list})");
                }
            }
            catch (Exception e) { Debug.LogWarning("[Display] display mode: " + e.Message); }
        }
    }
}
