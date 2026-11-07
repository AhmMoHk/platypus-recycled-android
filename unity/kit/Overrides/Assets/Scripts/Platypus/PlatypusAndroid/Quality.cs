// Platypus Reclayed Android port: Video options for a phone. Resolution / window mode / VSync are gone; the
// Resolution row of the original menu becomes Quality (Low / Medium / High / Ultra) and FPS offers 30-144.
using System;
using Blackdrop.Platforms;
using UnityEngine;

namespace PlatypusAndroid
{
    public static class Quality
    {
        public static readonly string[] FpsOptions = { "30", "60", "90", "120", "144" };
        static readonly string[] Ids = { "Options.QualityLow", "Options.QualityMedium", "Options.QualityHigh", "Options.QualityUltra" };
        static readonly int[] Msaa = { 0, 0, 2, 4 };
        static readonly int[] MipLimit = { 2, 1, 0, 0 };           // Low: quarter-size textures, Medium: half
        const string Key = "AndroidQuality";

        static PlatformStore Store => Platform.Instance.LocalStore;

        public static int Level
        {
            get => Mathf.Clamp(Store.GetInt(Key, 2), 0, 3);
            set { Store.SetInt(Key, Mathf.Clamp(value, 0, 3)); Apply(); }
        }

        /// <summary>FPS limit kept to the offered values (the PC default was 240).</summary>
        public static int NormalizeFps(int fps)
        {
            int best = 60, d = int.MaxValue;
            foreach (string s in FpsOptions) { int v = int.Parse(s); if (Math.Abs(v - fps) < d) { d = Math.Abs(v - fps); best = v; } }
            return fps >= 200 || fps <= 0 ? 60 : best;
        }

        public static void Apply()
        {
            // No resolution change: the game always renders at the screen's own size (changing it at run time
            // risks touches and picture disagreeing). The levels trade texture detail and edge smoothing.
            int level = Level;
            QualitySettings.vSyncCount = 0;
            QualitySettings.antiAliasing = Msaa[level];
            QualitySettings.globalTextureMipmapLimit = MipLimit[level];
            QualitySettings.anisotropicFiltering = level >= 2 ? AnisotropicFiltering.ForceEnable : AnisotropicFiltering.Disable;
        }

        /// <summary>Turns the original Resolution row into the Quality row.</summary>
        public static void SetupSelector(OptionSelectorString selector, Action changed)
        {
            var label = selector.GetComponent<ButtonScript>();
            if (label != null)
            {
                label.localizedText = "Options.QualityLabel";
                if (label.text != null) { label.text.useLocalizedText = true; label.text.localizedTextID = "Options.QualityLabel"; label.text.UpdateText(); }
            }
            selector.SetOptions(new[] { "0", "1", "2", "3" }, Ids, Level);
            selector.OnOptionChanged += (string option, int index) => { Level = index; changed?.Invoke(); };
        }

        /// <summary>Menu strings for the new rows, in the game's languages (English where its fonts lack the glyphs).</summary>
        public static void ApplyStrings() => LocalisationManager.ApplyString(Csv);

        const string Csv =
"\"Meta.Key\",\"en_gb\",\"de_de\",\"fr_fr\",\"it_it\",\"pt_pt\",\"pt_br\",\"es_es\",\"es_mx\",\"pl_pl\",\"ko_kr\",\"ru_ru\",\"ja_jp\",\"zh_cn\",\"zh_tw\",\"th_th\",\"id_id\"\n" +
"\"Options.QualityLabel\",\"Quality\",\"Qualität\",\"Qualité\",\"Qualità\",\"Qualidade\",\"Qualidade\",\"Calidad\",\"Calidad\",\"Jakość\",\"Quality\",\"Качество\",\"Quality\",\"Quality\",\"Quality\",\"คุณภาพ\",\"Kualitas\"\n" +
"\"Options.QualityLow\",\"Low\",\"Niedrig\",\"Basse\",\"Bassa\",\"Baixa\",\"Baixa\",\"Baja\",\"Baja\",\"Niska\",\"Low\",\"Низкое\",\"Low\",\"Low\",\"Low\",\"ต่ำ\",\"Rendah\"\n" +
"\"Options.QualityMedium\",\"Medium\",\"Mittel\",\"Moyenne\",\"Media\",\"Média\",\"Média\",\"Media\",\"Media\",\"Średnia\",\"Medium\",\"Среднее\",\"Medium\",\"Medium\",\"Medium\",\"ปานกลาง\",\"Sedang\"\n" +
"\"Options.QualityHigh\",\"High\",\"Hoch\",\"Haute\",\"Alta\",\"Alta\",\"Alta\",\"Alta\",\"Alta\",\"Wysoka\",\"High\",\"Высокое\",\"High\",\"High\",\"High\",\"สูง\",\"Tinggi\"\n" +
"\"Options.QualityUltra\",\"Ultra\",\"Ultra\",\"Ultra\",\"Ultra\",\"Ultra\",\"Ultra\",\"Ultra\",\"Ultra\",\"Ultra\",\"Ultra\",\"Ультра\",\"Ultra\",\"Ultra\",\"Ultra\",\"สูงสุด\",\"Ultra\"\n";
    }
}
