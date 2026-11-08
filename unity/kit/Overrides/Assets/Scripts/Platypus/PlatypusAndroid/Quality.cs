// Platypus Reclayed Android port: Video options for a phone (Resolution / Window Mode / VSync of the PC menu are gone).
//  - Quality, the PC Resolution row: Low / Medium / High. The levels set the size the game is drawn at
//    (Low 70% and at most 720 lines, Medium 85% and at most 1080 lines, High the screen's own size) and texture
//    detail (Low: half-size textures where they have mipmaps).
//  - Screen, the PC Window Mode row: Original = the game's 16:9 picture, the rest of the screen black.
//    Fullscreen = the same 16:9 picture stretched over the whole screen (as a PC emulator does): the game is
//    drawn at a 16:9 size and Android scales that surface to the screen, so everything stretches together
//    (films, menus, play field, score) and taps still land where they are drawn.
//  - FPS 30 / 60 / 120; the display is asked for a matching refresh rate (DisplayRunner). No 90: Xiaomi's HyperOS gives
//    apps 60 or 120 Hz only, so 90 ran at 60 (an old 90 setting becomes 120).
using System;
using Blackdrop.Platforms;
using UnityEngine;

namespace PlatypusAndroid
{
    public static class Quality
    {
        public static readonly string[] FpsOptions = { "30", "60", "120" };
        static readonly string[] Ids = { "Options.QualityLow", "Options.QualityMedium", "Options.QualityHigh" };
        static readonly float[] Scale = { 0.7f, 0.85f, 1f };
        static readonly int[] MaxLines = { 720, 1080, 100000 };
        static readonly int[] MipLimit = { 1, 0, 0 };
        const string Key = "AndroidQuality", DisplayKey = "AndroidDisplay";

        static PlatformStore Store => Platform.Instance.LocalStore;

        public static int Level
        {
            get => Mathf.Clamp(Store.GetInt(Key, 2), 0, 2);        // an old "Ultra" (3) reads as High
            set { levelCache = Mathf.Clamp(value, 0, 2); Store.SetInt(Key, levelCache); Apply(); }
        }

        static int levelCache = -1;

        /// <summary>The quality level without a store lookup (High until the save store is ready).</summary>
        public static int LevelCached
        {
            get
            {
                if (levelCache >= 0) return levelCache;
                // never touch Platform.Instance before the game has set up its platform (it would create a default one)
                if (BasePlatformSystem.Instance == null || BasePlatformSystem.Instance.LocalStore == null) return 2;
                try { levelCache = Level; } catch (Exception) { return 2; }
                return levelCache;
            }
        }

        /// <summary>false = Original (16:9 with black bars), true = Fullscreen (16:9 stretched over the screen).</summary>
        public static bool Fullscreen
        {
            get => Store.GetInt(DisplayKey, 0) == 1;
            set { Store.SetInt(DisplayKey, value ? 1 : 0); Apply(); }
        }

        /// <summary>FPS limit kept to the offered values (the PC default was 240; 144 and 90 become 120).</summary>
        public static int NormalizeFps(int fps)
        {
            if (fps <= 0 || fps >= 200) return 60;
            int best = 60, d = int.MaxValue;
            foreach (string s in FpsOptions) { int v = int.Parse(s); if (Math.Abs(v - fps) <= d) { d = Math.Abs(v - fps); best = v; } }
            return best;
        }

        // ---- the screen ------------------------------------------------------------------------------------------

        static int nativeW, nativeH;

        /// <summary>The phone's own screen size (the app window, landscape). Asked from Android each time, so it is
        /// right whatever size the game currently draws at; before the first resolution change Unity's own screen size
        /// is the fallback.</summary>
        internal static void CaptureNative()
        {
            if (WindowSize(out int w, out int h) || nativeW == 0)
            {
                if (w <= 0 || h <= 0) { w = Screen.width; h = Screen.height; }
                if (w <= 0 || h <= 0) { w = Display.main.systemWidth; h = Display.main.systemHeight; }
                int nw = Mathf.Max(w, h), nh = Mathf.Min(w, h);
                if (nw != nativeW || nh != nativeH)
                {
                    nativeW = nw; nativeH = nh;
                    Debug.Log($"[Display] screen {nativeW}x{nativeH} (display {Display.main.systemWidth}x{Display.main.systemHeight})");
                }
            }
        }

        static bool WindowSize(out int w, out int h)
        {
            w = h = 0;
            if (Application.platform != RuntimePlatform.Android) return false;
            try
            {
                using (var player = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
                using (var activity = player.GetStatic<AndroidJavaObject>("currentActivity"))
                using (var window = activity.Call<AndroidJavaObject>("getWindow"))
                using (var decor = window.Call<AndroidJavaObject>("getDecorView"))
                {
                    w = decor.Call<int>("getWidth");
                    h = decor.Call<int>("getHeight");
                }
            }
            catch (Exception) { w = h = 0; }
            return w > 0 && h > 0;
        }

        public static int NativeWidth { get { if (nativeW == 0) CaptureNative(); return nativeW; } }
        public static int NativeHeight { get { if (nativeW == 0) CaptureNative(); return nativeH; } }

        /// <summary>How much wider a drawn pixel is on the glass than tall (1 in Original, 20/16 for a 20:9 phone in
        /// Fullscreen). The touch controls divide their widths by it so they stay round.</summary>
        public static float Stretch
        {
            get
            {
                if (nativeW == 0) CaptureNative();
                if (!Application.isMobilePlatform || Screen.width <= 0 || Screen.height <= 0 || nativeH <= 0) return 1f;
                float s = ((float)nativeW / nativeH) / ((float)Screen.width / Screen.height);
                return Mathf.Abs(s - 1f) < 0.01f ? 1f : s;
            }
        }

        static void TargetSize(int level, bool fullscreen, out int w, out int h)
        {
            float rw = nativeW, rh = nativeH;
            if (fullscreen)
            {
                if (rw / rh > 16f / 9f) rw = rh * 16f / 9f;       // wide phone: narrower picture, stretched sideways
                else rh = rw * 9f / 16f;                           // 4:3 / 16:10 tablet: stretched up and down
            }
            float s = Mathf.Min(1f, Scale[level], MaxLines[level] / rh);
            w = Mathf.Max(2, Mathf.RoundToInt(rw * s / 2f) * 2);
            h = Mathf.Max(2, Mathf.RoundToInt(rh * s / 2f) * 2);
        }

        static int appliedW, appliedH, appliedFps;

        public static void Apply()
        {
            int level = Level;
            QualitySettings.vSyncCount = 0;
            QualitySettings.globalTextureMipmapLimit = MipLimit[level];
            QualitySettings.anisotropicFiltering = AnisotropicFiltering.Disable;   // flat sprites gain nothing from it
            QualitySettings.softParticles = false;
            if (QualitySettings.antiAliasing != 0) QualitySettings.antiAliasing = 0;  // clay sprites have soft edges already
            if (!Application.isMobilePlatform) return;

            CaptureNative();
            TargetSize(level, Fullscreen, out int w, out int h);
            int fps = Application.targetFrameRate > 0 ? Application.targetFrameRate : 60;
            if (w != appliedW || h != appliedH || fps != appliedFps)
            {
                // the size to draw at and the refresh rate wanted, both through Unity (it also picks the display mode);
                // the display mode is asked for again directly a moment later (DisplayRunner)
                appliedW = w; appliedH = h; appliedFps = fps;
                Screen.SetResolution(w, h, FullScreenMode.FullScreenWindow, new RefreshRate { numerator = (uint)fps, denominator = 1 });
                DisplayRunner.RequestRefreshRate(fps, true);
                Debug.Log($"[Display] drawing at {w}x{h} ({(Fullscreen ? "Fullscreen" : "Original")}, {Ids[level].Substring(15)}) on {nativeW}x{nativeH}, {fps} FPS");
            }
            else DisplayRunner.RequestRefreshRate(fps);
        }

        static readonly string[] OptionKeys = { Key, DisplayKey, "AndroidTouchSize", "AndroidTouchOpacity", "AndroidPauseOpacity" };

        /// <summary>Runs <paramref name="wipe"/> (the game's Reset Progress) and puts the Android options back.</summary>
        public static void KeepOptions(Action wipe)
        {
            var kept = new System.Collections.Generic.Dictionary<string, int>();
            foreach (string k in OptionKeys) if (Store.HasKey(k)) kept[k] = Store.GetInt(k, 0);
            wipe();
            foreach (var kv in kept) Store.SetInt(kv.Key, kv.Value);
        }

        // ---- menu rows -------------------------------------------------------------------------------------------

        static void Relabel(Component row, string id)
        {
            var label = row.GetComponent<ButtonScript>();
            if (label == null) return;
            label.localizedText = id;
            if (label.text != null) { label.text.useLocalizedText = true; label.text.localizedTextID = id; label.text.UpdateText(); }
        }

        /// <summary>Turns the original Resolution row into the Quality row.</summary>
        public static void SetupSelector(OptionSelectorString selector, Action changed)
        {
            Relabel(selector, "Options.QualityLabel");
            selector.SetOptions(new[] { "0", "1", "2" }, Ids, Level);
            selector.OnOptionChanged += (string option, int index) => { Level = index; changed?.Invoke(); };
        }

        /// <summary>Turns the original Window Mode row into the Screen row (Original / Fullscreen).</summary>
        public static void SetupDisplaySelector(OptionSelector<FullScreenMode> selector, Action changed)
        {
            Relabel(selector, "Options.DisplayLabel");
            selector.SetOptions(new[] { FullScreenMode.Windowed, FullScreenMode.FullScreenWindow },
                                new[] { "Options.DisplayOriginal", "Options.WindowModeFullscreen" }, Fullscreen ? 1 : 0);
            selector.OnOptionChanged += (FullScreenMode option, int index) => { Fullscreen = index == 1; changed?.Invoke(); };
        }

        /// <summary>Menu strings for the new rows, in the game's languages (English where its fonts lack the glyphs).</summary>
        public static void ApplyStrings() => LocalisationManager.ApplyString(Csv);

        const string Csv =
"\"Meta.Key\",\"en_gb\",\"de_de\",\"fr_fr\",\"it_it\",\"pt_pt\",\"pt_br\",\"es_es\",\"es_mx\",\"pl_pl\",\"ko_kr\",\"ru_ru\",\"ja_jp\",\"zh_cn\",\"zh_tw\",\"th_th\",\"id_id\"\n" +
"\"Options.QualityLabel\",\"Quality\",\"Qualität\",\"Qualité\",\"Qualità\",\"Qualidade\",\"Qualidade\",\"Calidad\",\"Calidad\",\"Jakość\",\"Quality\",\"Качество\",\"Quality\",\"Quality\",\"Quality\",\"คุณภาพ\",\"Kualitas\"\n" +
"\"Options.QualityLow\",\"Low\",\"Niedrig\",\"Basse\",\"Bassa\",\"Baixa\",\"Baixa\",\"Baja\",\"Baja\",\"Niska\",\"Low\",\"Низкое\",\"Low\",\"Low\",\"Low\",\"ต่ำ\",\"Rendah\"\n" +
"\"Options.QualityMedium\",\"Medium\",\"Mittel\",\"Moyenne\",\"Media\",\"Média\",\"Média\",\"Media\",\"Media\",\"Średnia\",\"Medium\",\"Среднее\",\"Medium\",\"Medium\",\"Medium\",\"ปานกลาง\",\"Sedang\"\n" +
"\"Options.QualityHigh\",\"High\",\"Hoch\",\"Haute\",\"Alta\",\"Alta\",\"Alta\",\"Alta\",\"Alta\",\"Wysoka\",\"High\",\"Высокое\",\"High\",\"High\",\"High\",\"สูง\",\"Tinggi\"\n" +
"\"Options.DisplayLabel\",\"Screen\",\"Bildschirm\",\"Écran\",\"Schermo\",\"Ecrã\",\"Tela\",\"Pantalla\",\"Pantalla\",\"Ekran\",\"화면\",\"Экран\",\"Screen\",\"屏幕\",\"螢幕\",\"หน้าจอ\",\"Layar\"\n" +
"\"Options.DisplayOriginal\",\"Original\",\"Original\",\"Original\",\"Originale\",\"Original\",\"Original\",\"Original\",\"Original\",\"Oryginalny\",\"원본\",\"Оригинал\",\"オリジナル\",\"原始\",\"原始\",\"ต้นฉบับ\",\"Asli\"\n" +
"\"Options.TouchSize\",\"Touch Controls Size\",\"Größe der Steuerung\",\"Taille des commandes\",\"Dimensione comandi\",\"Tamanho dos controlos\",\"Tamanho dos controles\",\"Tamaño de controles\",\"Tamaño de controles\",\"Rozmiar sterowania\",\"컨트롤 크기\",\"Размер управления\",\"操作ボタンの大きさ\",\"Touch Controls Size\",\"Touch Controls Size\",\"ขนาดปุ่มควบคุม\",\"Ukuran Kontrol\"\n" +
"\"Options.TouchOpacity\",\"Touch Controls Opacity\",\"Deckkraft der Steuerung\",\"Opacité des commandes\",\"Opacità comandi\",\"Opacidade dos controlos\",\"Opacidade dos controles\",\"Opacidad de controles\",\"Opacidad de controles\",\"Krycie sterowania\",\"컨트롤 불투명도\",\"Прозрачность управления\",\"Touch Controls Opacity\",\"Touch Controls Opacity\",\"Touch Controls Opacity\",\"ความทึบปุ่มควบคุม\",\"Opasitas Kontrol\"\n" +
"\"Options.PauseOpacity\",\"Pause Button Opacity\",\"Deckkraft der Pause-Taste\",\"Opacité du bouton pause\",\"Opacità tasto pausa\",\"Opacidade do botão pausa\",\"Opacidade do botão pausa\",\"Opacidad del botón pausa\",\"Opacidad del botón pausa\",\"Krycie przycisku pauzy\",\"일시정지 버튼 불투명도\",\"Прозрачность кнопки паузы\",\"Pause Button Opacity\",\"Pause Button Opacity\",\"Pause Button Opacity\",\"ความทึบปุ่มหยุด\",\"Opasitas Tombol Jeda\"\n";
    }
}
