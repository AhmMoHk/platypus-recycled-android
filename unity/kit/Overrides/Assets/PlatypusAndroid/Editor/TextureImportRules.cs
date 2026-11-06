// Platypus Reclayed Android port: import rules applied on the first import, so the 1300 mod textures are encoded
// once, straight to the phone format. The full-size art is kept (no 2K cap); only the block size of the ASTC
// encoding is chosen (build_android.bat -astc 4x4 | 5x5 | 6x6 | 8x8, default 6x6).
using System;
using UnityEditor;
using UnityEngine;

namespace PlatypusAndroid.EditorTools
{
    public class TextureImportRules : AssetPostprocessor
    {
        public const string ModArt = "Assets/generatedmods/";
        const string PrefKey = "PlatypusAndroid.Astc";

        public static TextureImporterFormat Format
        {
            get
            {
                string v = Environment.GetEnvironmentVariable("PLATYPUS_ASTC");
                if (string.IsNullOrEmpty(v)) v = EditorPrefs.GetString(PrefKey, "6x6");
                switch (v.Trim().ToLowerInvariant())
                {
                    case "4x4": return TextureImporterFormat.ASTC_4x4;
                    case "5x5": return TextureImporterFormat.ASTC_5x5;
                    case "8x8": return TextureImporterFormat.ASTC_8x8;
                    default: return TextureImporterFormat.ASTC_6x6;
                }
            }
        }

        public static void Remember(string block) => EditorPrefs.SetString(PrefKey, block);

        /// <summary>true when the importer already has the Android settings this port wants</summary>
        public static bool Apply(TextureImporter ti, bool modArt)
        {
            var s = ti.GetPlatformTextureSettings("Android");
            int max = Mathf.Max(ti.maxTextureSize, 32);
            if (!modArt)
            {
                // the clay touch buttons, the icon: keep them crisp, uncompressed
                if (!ti.assetPath.StartsWith("Assets/PlatypusAndroid/", StringComparison.Ordinal)) return true;
                if (s.overridden && s.format == TextureImporterFormat.RGBA32) return true;
                s.overridden = true; s.format = TextureImporterFormat.RGBA32; s.maxTextureSize = max;
                ti.SetPlatformTextureSettings(s);
                return false;
            }
            var f = Format;
            if (s.overridden && s.format == f && s.maxTextureSize == max) return true;
            s.overridden = true;
            s.format = f;
            s.maxTextureSize = max;                       // the size the game shipped with (up to 8192)
            s.compressionQuality = 50;
            s.textureCompression = TextureImporterCompression.CompressedHQ;
            ti.SetPlatformTextureSettings(s);
            return false;
        }

        void OnPreprocessTexture()
        {
            Apply((TextureImporter)assetImporter, assetPath.StartsWith(ModArt, StringComparison.Ordinal));
        }

        void OnPreprocessAudio()
        {
            if (!assetPath.StartsWith(ModArt, StringComparison.Ordinal)) return;
            var ai = (AudioImporter)assetImporter;
            var s = ai.GetOverrideSampleSettings("Android");
            if (ai.ContainsSampleSettingsOverride("Android") && s.compressionFormat == AudioCompressionFormat.Vorbis) return;
            s = ai.defaultSampleSettings;
            s.compressionFormat = AudioCompressionFormat.Vorbis;
            s.quality = Mathf.Max(s.quality, 0.7f);
            ai.SetOverrideSampleSettings("Android", s);
        }
    }
}
