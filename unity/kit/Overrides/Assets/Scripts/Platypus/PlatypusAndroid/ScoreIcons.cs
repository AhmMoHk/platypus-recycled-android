// Platypus Reclayed Android port: the Hall of Fame shows the ship of each score by the icon of the skin it was flown
// with. When that skin is not found (a score from another mod, an add-on that is off, a skin renamed in an update) or
// its icon is not loaded, the row showed no ship at all. Then the icon of another skin of the same ship is shown, and
// the log says once per skin why ("[Scores] ...").
using System.Collections.Generic;
using Platypus.Core;
using UnityEngine;
using UnityEngine.UI;

namespace PlatypusAndroid
{
    public static class ScoreIcons
    {
        static readonly HashSet<string> told = new HashSet<string>();

        public static bool Fallback(Image icon, ScoreData d)
        {
            ModInstance root = ModManager.modRoot;
            if (icon == null || d == null || root == null) return false;
            string why = root.TryGetSkinData(out SkinData own, d.skin)
                ? $"the icon {own.iconSprite} of skin {d.skin} did not load"
                : $"there is no skin {d.skin}";
            SkinData shown = null;
            foreach (SkinData k in root.GetAllSkinData())
            {
                if (k == null || k.targetShip != d.ship || k.name == d.skin) continue;
                if (TryIcon(root, k, icon)) { shown = k; break; }
            }
            if (told.Add(d.skin + "|" + d.ship))
                Debug.Log($"[Scores] score {d.score} ({d.mod}, ship {d.ship}): {why}; " +
                          (shown != null ? $"showing the icon of {shown.name}" : "no other skin of that ship either"));
            return shown != null;
        }

        static bool TryIcon(ModInstance root, SkinData k, Image icon)
        {
            if (string.IsNullOrEmpty(k.iconSprite) || !root.TryGetAnimationData(out AnimationData a, k.iconSprite)) return false;
            if (!root.TryLoadSpriteSheet(out Sprite[] sprites, a) || sprites == null || sprites.Length == 0) return false;
            icon.sprite = sprites[0];
            return true;
        }
    }
}
