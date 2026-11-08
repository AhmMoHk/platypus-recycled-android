// Platypus Reclayed Android port: per-frame display housekeeping.
//  - A black backdrop camera drawn first every frame. On a phone the screen buffer holds leftover garbage unless
//    something clears it, and the loading screen / intro film cameras never clear, which showed as TV-static
//    noise and smeared "Loading" text around the Claymatic film. Everything outside the game's 16:9 picture is
//    therefore pure black.
//  - Quality Low draws the scene cameras without HDR (one full-screen float buffer and copy less per frame);
//    Medium and High keep the game's own camera setup.
//  - Asks Android for a display refresh rate matching the FPS limit (phones keep apps at 60 Hz otherwise): the
//    window's preferred display mode and the game surface's frame rate, asked again whenever the display drops
//    below it (another app, the system or a game booster may change it). [Perf] lines show the display's rate.
//  - While a loading screen is up, Unity may spend more of each frame finishing loaded objects and uploading
//    textures (the loading screen is still, so nothing visible suffers), which shortens loading.
//  - Keeps a film picture (the intro's first) on screen behind the loading bar, as the PC game does (there the
//    camera simply never clears, which on a phone shows garbage instead).
//  - The loading screen camera draws without HDR / MSAA, straight onto the screen (see LateUpdate).
using System;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.Video;

namespace PlatypusAndroid
{
    public class DisplayRunner : MonoBehaviour
    {
        static DisplayRunner instance;
        static int wantedHz, appliedHz = -1, reasks;
        static float refreshAt = -1f, nextCheck = 5f;
        /// <summary>The display's current refresh rate (0 = not known yet).</summary>
        public static float CurrentHz { get; private set; }
        Camera backdrop;
        int cameraCount = -1, cameraLevel = -1;
        Camera[] cameras = new Camera[16];
        readonly System.Collections.Generic.Dictionary<Camera, bool> sceneHdr = new System.Collections.Generic.Dictionary<Camera, bool>();

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
        static void Boot()
        {
            Quality.CaptureNative();
            var go = new GameObject("[Display]");
            DontDestroyOnLoad(go);
            go.hideFlags = HideFlags.HideInHierarchy;
            instance = go.AddComponent<DisplayRunner>();
        }

        const int LoadingSceneIndex = 7;                   // LoadModScreen, loaded on top of the others by LoadingScreen

        static RenderTexture held;
        static Material heldMat;
        static float heldAspect = 16f / 9f;

        void Awake()
        {
            QualitySettings.asyncUploadBufferSize = 32;          // MB: big textures upload without resizing the buffer
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
            SceneManager.sceneLoaded += (s, m) =>
            {
                cameraCount = -1;
                var gone = new System.Collections.Generic.List<Camera>();
                foreach (Camera c in sceneHdr.Keys) if (c == null) gone.Add(c);
                foreach (Camera c in gone) sceneHdr.Remove(c);
            };
        }

        void LateUpdate()
        {
            int n = Camera.allCamerasCount, level = Quality.LevelCached;
            if (n != cameraCount || level != cameraLevel)
            {
                cameraCount = n;
                cameraLevel = level;
                if (cameras.Length < n) cameras = new Camera[n + 8];
                int got = Camera.GetAllCameras(cameras);
                for (int i = 0; i < got; i++)
                {
                    Camera c = cameras[i];
                    cameras[i] = null;
                    if (c == null || c == backdrop) continue;
                    if (c.gameObject.scene.buildIndex == LoadingSceneIndex)
                    {
                        // the loading screen camera draws over the screen without clearing it; with HDR / MSAA it
                        // drew into a buffer of its own that nothing cleared, whose leftovers cut the loading bar
                        // (split in two, bits missing) and let old pictures through. Drawn straight to the screen,
                        // with depth and stencil cleared, it is the same picture as on PC.
                        c.allowHDR = false;
                        c.allowMSAA = false;
                        if (c.clearFlags == CameraClearFlags.Nothing) c.clearFlags = CameraClearFlags.Depth;
                        continue;
                    }
                    if (!sceneHdr.ContainsKey(c)) sceneHdr[c] = c.allowHDR;
                    c.allowHDR = level > 0 && sceneHdr[c];
                }
            }
            bool loading = LoadingScreen.IsLoading;
            ThreadPriority prio = loading ? ThreadPriority.High : ThreadPriority.BelowNormal;
            if (Application.backgroundLoadingPriority != prio)
            {
                Application.backgroundLoadingPriority = prio;
                QualitySettings.asyncUploadTimeSlice = loading ? 8 : 2;   // ms per frame
            }
            if (!loading && held != null) ReleaseFrame();
            if (refreshAt >= 0f && Time.unscaledTime >= refreshAt)
            {
                refreshAt = -1f;
                ApplyRefreshRate(wantedHz);
            }
            if (Application.platform == RuntimePlatform.Android && Time.unscaledTime >= nextCheck)
            {
                nextCheck = Time.unscaledTime + 5f;
                CheckRefreshRate();
            }
        }

        void OnApplicationPause(bool paused)
        {
            if (paused) SaveGuard.Flush("the game went to the background");
        }

        void OnApplicationFocus(bool focus)
        {
            if (focus && wantedHz > 0) RequestRefreshRate(wantedHz, true);
        }

        /// <summary>Reads the display's refresh rate and asks again for the wanted one when it is lower.</summary>
        static void CheckRefreshRate()
        {
            try
            {
                using (var player = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
                using (var activity = player.GetStatic<AndroidJavaObject>("currentActivity"))
                using (var wm = activity.Call<AndroidJavaObject>("getWindowManager"))
                using (var display = wm.Call<AndroidJavaObject>("getDefaultDisplay"))
                    CurrentHz = display.Call<float>("getRefreshRate");
            }
            catch (Exception) { return; }
            if (wantedHz > 60 && CurrentHz > 0f && CurrentHz < wantedHz - 1f && refreshAt < 0f)
            {
                reasks++;
                if (reasks <= 5 || reasks % 12 == 0) Debug.Log($"[Display] display at {CurrentHz:0} Hz, {wantedHz} wanted: asking again ({reasks})");
                RequestRefreshRate(wantedHz, true);
            }
        }

        /// <summary>Keeps the film's current picture on screen (drawn by the backdrop, under everything else) until
        /// <see cref="ReleaseFrame"/> or the end of loading.</summary>
        public static void HoldFrame(VideoPlayer v)
        {
            try
            {
                Texture t = v != null ? v.texture : null;
                if (t == null || t.width <= 0 || t.height <= 0) return;
                if (held == null || held.width != t.width || held.height != t.height)
                {
                    ReleaseFrame();
                    held = new RenderTexture(t.width, t.height, 0) { name = "Held film frame" };
                }
                Graphics.Blit(t, held);
                heldAspect = (float)t.width / t.height;
                if (heldMat == null) heldMat = new Material(Canvas.GetDefaultCanvasMaterial());
            }
            catch (Exception e) { Debug.LogWarning("[Display] hold frame: " + e.Message); }
        }

        public static void ReleaseFrame()
        {
            if (held == null) return;
            held.Release();
            Destroy(held);
            held = null;
        }

        // the backdrop camera: after clearing, the held picture, fitted inside the screen like the film was
        void OnPostRender()
        {
            if (held == null || heldMat == null) return;
            float sa = Screen.height > 0 ? (float)Screen.width / Screen.height : heldAspect;
            float w = 1f, h = 1f;
            if (sa > heldAspect) w = heldAspect / sa; else h = sa / heldAspect;
            float x0 = (1f - w) * 0.5f, y0 = (1f - h) * 0.5f;
            heldMat.mainTexture = held;
            heldMat.SetPass(0);
            GL.PushMatrix();
            GL.LoadOrtho();
            GL.Begin(GL.QUADS);
            GL.Color(Color.white);
            GL.TexCoord2(0f, 0f); GL.Vertex3(x0, y0, 0f);
            GL.TexCoord2(0f, 1f); GL.Vertex3(x0, y0 + h, 0f);
            GL.TexCoord2(1f, 1f); GL.Vertex3(x0 + w, y0 + h, 0f);
            GL.TexCoord2(1f, 0f); GL.Vertex3(x0 + w, y0, 0f);
            GL.End();
            GL.PopMatrix();
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

        static AndroidJavaObject surfaceViewClass, viewGroupClass;
        static int sdk = -1;

        /// <summary>Surface.setFrameRate on every SurfaceView in the window (Unity draws into one), Android 11+.</summary>
        static int SetSurfaceRate(AndroidJavaObject view, float hz, int depth)
        {
            if (view == null || depth > 12) return 0;
            if (sdk < 0) using (var v = new AndroidJavaClass("android.os.Build$VERSION")) sdk = v.GetStatic<int>("SDK_INT");
            if (sdk < 30) return 0;
            if (surfaceViewClass == null)
                using (var cls = new AndroidJavaClass("java.lang.Class"))
                {
                    surfaceViewClass = cls.CallStatic<AndroidJavaObject>("forName", "android.view.SurfaceView");
                    viewGroupClass = cls.CallStatic<AndroidJavaObject>("forName", "android.view.ViewGroup");
                }
            int count = 0;
            if (surfaceViewClass.Call<bool>("isInstance", view))
            {
                using (var holder = view.Call<AndroidJavaObject>("getHolder"))
                using (var surface = holder.Call<AndroidJavaObject>("getSurface"))
                {
                    if (surface != null && surface.Call<bool>("isValid"))
                    {
                        if (sdk >= 31) surface.Call("setFrameRate", hz, 0, 1);   // FRAME_RATE_COMPATIBILITY_DEFAULT, CHANGE_FRAME_RATE_ALWAYS
                        else surface.Call("setFrameRate", hz, 0);
                        count++;
                    }
                }
            }
            if (viewGroupClass.Call<bool>("isInstance", view))
            {
                int n = view.Call<int>("getChildCount");
                for (int i = 0; i < n; i++)
                    using (var child = view.Call<AndroidJavaObject>("getChildAt", i)) count += SetSurfaceRate(child, hz, depth + 1);
            }
            return count;
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
                    int surfaces = 0;
                    using (var decor = window.Call<AndroidJavaObject>("getDecorView")) surfaces = SetSurfaceRate(decor, bestHz, 0);
                    if (reasks <= 5)
                        Debug.Log($"[Display] {fps} FPS -> display mode {bestId} at {bestHz:0} Hz, {surfaces} game surface(s) asked (modes:{list})");
                }
            }
            catch (Exception e) { Debug.LogWarning("[Display] display mode: " + e.Message); }
        }
    }
}
