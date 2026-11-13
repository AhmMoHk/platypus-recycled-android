// Platypus Reclayed Android port: ship colours per ship pack. Each pack (the base game or a Workshop add-on: the
// hats, Perry the Platypus, Casinopolis Ships...) keeps a colour of its own. "Change Color" on a ship changes the
// colour of every ship of that pack, and of no other; moving to another ship shows it in its own pack's colour and
// changes nothing else. The PC game instead carried the colour over by skin name, which only works for packs named
// like the base game: ships of other packs fell back to their first colour (yellow), and the row showed a mix.
// Where two add-ons define a skin of the same name (the hats and Casinopolis both redefine the base game's skins),
// the definition the game flies with is used everywhere, so the ship screen shows the hats the ship really wears.
// The colours are remembered between sessions.
using System.Collections.Generic;
using System.IO;
using System.Text;
using Platypus.Core;
using UnityEngine;

namespace PlatypusAndroid
{
    public static class ShipColours
    {
        const string PrefsKey = "PlatypusAndroid.PackColours";
        static Dictionary<string, string> colours;                     // pack -> colour word ("yellow", "teal" ...)
        static ModInstance builtFor;
        static readonly Dictionary<string, List<SkinData>> skinsOf = new Dictionary<string, List<SkinData>>();
        static readonly Dictionary<string, string> packOf = new Dictionary<string, string>();

        static void Build()
        {
            ModInstance root = ModManager.modRoot;
            if (root == null || ReferenceEquals(root, builtFor)) return;
            builtFor = root;
            skinsOf.Clear();
            packOf.Clear();
            foreach (SkinData k in root.GetAllSkinData())
            {
                if (k == null || string.IsNullOrEmpty(k.name)) continue;
                SkinData real = root.TryGetSkinData(out SkinData r, k.name) && r != null ? r : k;   // what the game flies with
                string ship = real.targetShip ?? "";
                if (!skinsOf.TryGetValue(ship, out List<SkinData> list)) skinsOf[ship] = list = new List<SkinData>();
                list.Add(real);
            }
        }

        /// <summary>The pack a ship comes from: the add-on (or base mod) whose definition of it the game uses.</summary>
        static string Pack(string ship)
        {
            if (string.IsNullOrEmpty(ship)) return "";
            if (packOf.TryGetValue(ship, out string p)) return p;
            p = "";
            ModInstance root = ModManager.modRoot;
            if (root != null)
            {
                foreach (ModInstance a in root.addons) if (Defines(a, ship)) { p = Key(a); break; }
                if (p == "") for (ModInstance m = root; m != null; m = m.baseMod) if (Defines(m, ship)) { p = Key(m); break; }
            }
            packOf[ship] = p;
            return p;
        }

        static bool Defines(ModInstance m, string ship) => m != null && m.data != null && m.data.TryGetShipData(out ShipData _, ship);

        static string Key(ModInstance m) =>
            m.meta == null ? "" : (m.meta.modName ?? "") + "|" + Path.GetFileName((m.meta.AssetRoot ?? "").TrimEnd('/', '\\'));

        /// <summary>The colour part of a skin's name: "beginnerYellow2" of ship "beginner2" is "yellow",
        /// "perryBeginnerTeal" of "perryBeginner" is "teal".</summary>
        static string Word(SkinData k)
        {
            string n = k.name ?? "", s = k.targetShip ?? "";
            int i = 0;
            while (i < n.Length && i < s.Length && char.ToLowerInvariant(n[i]) == char.ToLowerInvariant(s[i])) i++;
            return n.Substring(i).TrimEnd('0', '1', '2', '3', '4', '5', '6', '7', '8', '9').ToLowerInvariant();
        }

        static SkinData Find(List<SkinData> list, string name)
        {
            foreach (SkinData k in list) if (k.name == name) return k;
            return null;
        }

        static SkinData InColour(List<SkinData> list, string word)
        {
            if (string.IsNullOrEmpty(word)) return null;
            foreach (SkinData k in list) if (Word(k) == word) return k;
            return null;
        }

        /// <summary>In place of the game's RefitSkin (start of the ship screen, another ship chosen): the ship's own
        /// saved skin, else its pack's colour, else its first colour. False when the ship has no skins (the game decides).</summary>
        public static bool Refit(int player)
        {
            Build();
            string ship = Globals.playerShips.Get(player, "regular");
            if (ship == null || !skinsOf.TryGetValue(ship, out List<SkinData> list) || list.Count == 0) return false;
            string pack = Pack(ship);
            Load();
            SkinData pick = Find(list, Globals.playerSkin.Get(player, ""));
            if (pick == null && colours.TryGetValue(pack, out string word)) pick = InColour(list, word);
            if (pick == null) pick = list[0];
            Globals.playerSkin[player] = pick.name;
            Remember(pack, Word(pick));
            return true;
        }

        /// <summary>After "Change Color": the chosen colour becomes the colour of the ship's pack.</summary>
        public static void Chosen(int player)
        {
            Build();
            string ship = Globals.playerShips.Get(player, "regular");
            if (ship == null || !skinsOf.TryGetValue(ship, out List<SkinData> list)) return;
            SkinData k = Find(list, Globals.playerSkin.Get(player, ""));
            if (k != null) Remember(Pack(ship), Word(k));
        }

        /// <summary>The skin a ship is shown in on the ship screen: the chosen ship its chosen skin, the others their
        /// pack's colour.</summary>
        public static SkinData SkinFor(string ship)
        {
            Build();
            if (ship == null || !skinsOf.TryGetValue(ship, out List<SkinData> list) || list.Count == 0) return null;
            if (ship == Globals.playerShips.Get(0, null))
            {
                SkinData own = Find(list, Globals.playerSkin.Get(0, ""));
                if (own != null) return own;
            }
            Load();
            if (colours.TryGetValue(Pack(ship), out string word))
            {
                SkinData k = InColour(list, word);
                if (k != null) return k;
            }
            return list[0];
        }

        static void Load()
        {
            if (colours != null) return;
            colours = new Dictionary<string, string>();
            string s = "";
            try { s = PlayerPrefs.GetString(PrefsKey, ""); } catch { }
            foreach (string line in s.Split('\n'))
            {
                int tab = line.IndexOf('\t');
                if (tab > 0) colours[line.Substring(0, tab)] = line.Substring(tab + 1);
            }
        }

        static void Remember(string pack, string word)
        {
            Load();
            if (colours.TryGetValue(pack, out string had) && had == word) return;
            colours[pack] = word;
            var sb = new StringBuilder();
            foreach (KeyValuePair<string, string> kv in colours) sb.Append(kv.Key).Append('\t').Append(kv.Value).Append('\n');
            try { PlayerPrefs.SetString(PrefsKey, sb.ToString()); } catch { }
        }
    }
}
