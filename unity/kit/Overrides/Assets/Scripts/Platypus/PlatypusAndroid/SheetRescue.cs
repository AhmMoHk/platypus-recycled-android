// Platypus Reclayed Android port: a sprite sheet the mod loading did not deliver (seen with a Workshop ship: the Tank
// of "Casinopolis Ships" flew invisible, with a white square as its lives icon). The game loads loose sprite sheets
// only while a mod loads and refuses them afterwards; here such a sheet is read from its file when it is first
// asked for, the way the mod loading reads it. The log says once per sheet what happened ("[Ships] ..."), and the
// player's ship gets one line with the picture it shows.
using System;
using System.Collections.Generic;
using System.IO;
using Platypus.Core;
using UnityEngine;

namespace PlatypusAndroid
{
    public static class SheetRescue
    {
        static readonly Dictionary<string, Sprite[]> done = new Dictionary<string, Sprite[]>();
        static ModInstance doneFor;
        static readonly HashSet<string> reported = new HashSet<string>();

        /// <summary>Called when the mod could not give the sheet: reads it from whichever mod folder has its file.</summary>
        public static bool TryLoad(AnimationData data, out Sprite[] sprites)
        {
            sprites = null;
            ModInstance root = ModManager.modRoot;
            if (data == null || string.IsNullOrEmpty(data.Sprite) || root == null) return false;
            if (!ReferenceEquals(root, doneFor)) { done.Clear(); doneFor = root; }
            if (done.TryGetValue(data.Sprite, out sprites)) return sprites != null && sprites.Length > 0;
            string why = "no mod folder has " + data.RelPath;
            foreach (ModInstance m in Mods(root))
            {
                if (m == null || m.meta == null || string.IsNullOrEmpty(m.meta.AssetRoot)) continue;
                if (!File.Exists(Path.Combine(m.meta.AssetRoot, data.RelPath))) continue;
                try
                {
                    bool classic = m.meta.IsOriginalVersion();
                    var ssip = new AssetImporter.SpriteSheetImportParameters
                    {
                        rows = data.Rows,
                        columns = data.Columns,
                        invertSheetOrder = data.InvertSheetOrder,
                        pixelsPerUnit = data.PixelsPerUnit,
                        pivot = data.SpritePivot,
                        tip = new AssetImporter.TextureImportParameters
                        {
                            name = data.Sprite,
                            relPath = data.RelPath,
                            cpuReadable = false,
                            filterMode = classic ? FilterMode.Point : FilterMode.Bilinear,
                            textureModeRepeat = data.TextureModeRepeat,
                            mips = !classic,
                            compressed = !classic && data.Compress,
                            allowReduceSize = data.AllowReduceSize
                        }
                    };
                    sprites = AssetImporter.ImportSpriteSheet(m.meta.AssetRoot, ssip);
                    if (sprites != null && sprites.Length > 0 && sprites[0] != null)
                    {
                        Debug.Log($"[Ships] {data.Sprite}: not delivered by the mod loading, read now from {m.meta.modName} " +
                                  $"({sprites.Length} pictures, {sprites[0].texture.width}x{sprites[0].texture.height} {sprites[0].texture.format})");
                        why = null;
                        break;
                    }
                    why = m.meta.modName + ": the file gave no pictures";
                    sprites = null;
                }
                catch (Exception e)
                {
                    why = $"{m.meta.modName}: {e.GetType().Name}: {e.Message}";
                    sprites = null;
                }
            }
            if (why != null) Debug.LogWarning($"[Ships] {data.Sprite} could not be loaded: {why}");
            done[data.Sprite] = sprites;
            return sprites != null;
        }

        static IEnumerable<ModInstance> Mods(ModInstance root)
        {
            foreach (ModInstance a in root.addons) yield return a;
            for (ModInstance m = root; m != null; m = m.baseMod) yield return m;
        }

        /// <summary>One log line per skin: the picture the player's ship starts with.</summary>
        public static void Report(int player, string skin, string sheet, Sprite shown)
        {
            if (!reported.Add(skin + "|" + sheet)) return;
            if (shown != null && shown.texture != null)
                Debug.Log($"[Ships] player {player + 1}: skin {skin}, ship picture {sheet}: {shown.name} " +
                          $"({shown.texture.width}x{shown.texture.height} {shown.texture.format})");
            else
                Debug.LogWarning($"[Ships] player {player + 1}: skin {skin}, ship picture {sheet}: none (the ship is invisible)");
        }
    }
}
