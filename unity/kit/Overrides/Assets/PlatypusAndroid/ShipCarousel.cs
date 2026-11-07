// Platypus Reclayed Android port: the single-player ship screen.
//  - Player 2's panel is gone and player 1 is joined on arrival (no "Press Fire To Join" / sleeping ship); the
//    panel takes the whole width.
//  - The ships stand side by side: the chosen one big in the middle, its neighbours smaller and dimmer either side
//    (all three at once with the game's three ships). The arrows, a gamepad, or a tap on a neighbour slide the row
//    along with the chosen ship growing into the middle; ship packs with more ships (9 and so on) scroll the same way.
//  - Every ship in the row wears the colour chosen with "Change Color" (the same colour family the game itself keeps
//    when you switch ships).
//  - The ship's stats sit at the far left.
// The game's own preview image is hidden and used as the material template; the panel logic is the game's.
using System;
using System.Collections.Generic;
using System.Linq;
using Platypus.Core;
using Rewired;
using UnityEngine;
using UnityEngine.UI;

namespace PlatypusAndroid
{
    public class ShipCarousel : MonoBehaviour
    {
        const float Side = 0.62f;          // neighbour size / chosen size
        const float SideAlpha = 0.6f;      // neighbour opacity (the chosen ship is fully opaque)
        const float SideShade = 0.8f;      // neighbour brightness

        class Slot
        {
            public RectTransform rt; public Image img; public Button button; public Material mat;
            public string key; public float offset;
        }

        ShipPanelScript panel;
        Image source;
        RectTransform area, holder;
        readonly Slot[] slots = new Slot[6];          // ships at base-2 .. base+3 around the animated position
        ShipData[] ships = new ShipData[0];
        SkinData[] skins = new SkinData[0];
        float pos, target;                            // ship index, animated / wanted (not wrapped)
        string shownShip;
        bool ready;
        int order = -1;

        void LateUpdate()
        {
            if (!ready && !Init()) return;
            Animate();
        }

        bool Init()
        {
            panel = GetComponent<ShipPanelScript>();
            if (panel == null || panel.shipImage == null) { enabled = false; return false; }
            source = panel.shipImage;
            area = source.transform.parent as RectTransform;
            if (area == null || source.material == null) return false;
            ready = true;

            // player 1 is in from the start: touch (with the paired mouse facet) and any gamepad
            Rewired.Player p0 = ReInput.players.GetPlayer(0);
            if (p0 != null && p0.controllers.Controllers.Count == 0)
            {
                p0.controllers.AddController(ReInput.controllers.Mouse, false);
                Joystick pad = ReInput.controllers.GetJoystick(1);
                if (pad != null) p0.controllers.AddController(pad, false);
            }

            ships = ModManager.modRoot.GetAllShipData() ?? new ShipData[0];
            skins = (ModManager.modRoot.GetAllSkinData() ?? Enumerable.Empty<SkinData>()).ToArray();

            // the game's preview becomes the frame of the row: full panel, picture hidden
            RectTransform srt = source.rectTransform;
            srt.anchorMin = Vector2.zero; srt.anchorMax = Vector2.one;
            srt.offsetMin = srt.offsetMax = Vector2.zero;
            source.enabled = false;
            var hgo = new GameObject("Ship Row", typeof(RectTransform));
            holder = (RectTransform)hgo.transform;
            holder.SetParent(srt, false);
            holder.anchorMin = Vector2.zero; holder.anchorMax = Vector2.one;
            holder.offsetMin = holder.offsetMax = Vector2.zero;
            // fixed drawing order through nested canvases: panel backdrop < ship row < stats / arrows / buttons < overlays
            Canvas root = area.GetComponentInParent<Canvas>();
            int order = root != null ? root.sortingOrder : 0, layer = root != null ? root.sortingLayerID : 0;
            Layer(holder.gameObject, order + 1, layer);
            if (panel.joinedOverlay != null) Layer(panel.joinedOverlay.gameObject, order + 2, layer);
            if (panel.notJoinedOverlay != null) Layer(panel.notJoinedOverlay.gameObject, order + 3, layer);
            if (panel.lockedInOverlay != null) Layer(panel.lockedInOverlay.gameObject, order + 3, layer);
            shipShader = Resources.Load<Shader>("PlatypusMultiTintUI");
            for (int i = 0; i < slots.Length; i++) slots[i] = MakeSlot(i);

            // stats to the far left
            Transform stats = panel.shipFlavourName != null ? panel.shipFlavourName.transform.parent : null;
            if (stats is RectTransform st) st.anchoredPosition = new Vector2(24f, st.anchoredPosition.y);

            int idx = CurrentIndex();
            pos = target = idx < 0 ? 0 : idx;
            shownShip = CurrentShip();
            return true;
        }

        Shader shipShader;

        static void Layer(GameObject go, int order, int sortingLayer)
        {
            Canvas c = go.GetComponent<Canvas>();
            if (c == null) c = go.AddComponent<Canvas>();
            c.overrideSorting = true;
            c.sortingLayerID = sortingLayer;
            c.sortingOrder = order;
            if (go.GetComponent<GraphicRaycaster>() == null) go.AddComponent<GraphicRaycaster>();
        }

        Slot MakeSlot(int i)
        {
            var go = new GameObject("Ship " + i, typeof(RectTransform));
            go.transform.SetParent(holder, false);
            var s = new Slot { rt = (RectTransform)go.transform };
            s.rt.anchorMin = s.rt.anchorMax = new Vector2(0.5f, 0.5f);
            s.rt.pivot = new Vector2(0.5f, 0.5f);
            s.img = go.AddComponent<Image>();
            s.img.preserveAspect = true;
            s.mat = shipShader != null ? new Material(shipShader) : new Material(source.material);
            s.img.material = s.mat;
            s.button = go.AddComponent<Button>();
            s.button.transition = Selectable.Transition.None;
            s.button.navigation = new Navigation { mode = Navigation.Mode.None };
            s.button.onClick.AddListener(() => Tapped(s));
            go.SetActive(false);
            return s;
        }

        void Tapped(Slot s)
        {
            if (!panel.PlayerConnected || panel.IsLockedIn) return;
            if (s.offset <= -0.5f && s.offset > -1.6f) panel.OnNavLeft();
            else if (s.offset >= 0.5f && s.offset < 1.6f) panel.OnNavRight();
        }

        static string CurrentShip() => Globals.playerShips.TryGetValue(0, out string v) ? v : null;
        static string CurrentSkin() => Globals.playerSkin.TryGetValue(0, out string v) ? v : null;

        int CurrentIndex()
        {
            string name = CurrentShip();
            for (int i = 0; i < ships.Length; i++) if (ships[i].name == name) return i;
            return -1;
        }

        /// <summary>The skin of <paramref name="ship"/> in the colour family of the chosen skin (the game's own
        /// rule when you switch ships).</summary>
        SkinData SkinFor(ShipData ship, string chosenShip, string chosenSkin)
        {
            SkinData first = null;
            string want = (chosenSkin ?? "").ToLowerInvariant();
            foreach (SkinData k in skins)
            {
                if (k == null || k.targetShip != ship.name) continue;
                if (ship.name == chosenShip && k.name == chosenSkin) return k;
                if (first == null) first = k;
                if (ship.name != chosenShip && k.name.Length >= ship.name.Length &&
                    want.Contains(k.name.Substring(ship.name.Length).ToLowerInvariant())) return k;
            }
            return first;
        }

        void Dress(Slot s, ShipData ship, SkinData skin)
        {
            string key = ship.name + "|" + (skin != null ? skin.name : "");
            if (s.key == key) return;
            s.key = key;
            if (skin == null) { s.img.sprite = null; return; }
            if (ModManager.modRoot.TryLoadSprite(out Sprite sprite, skin.previewSprite)) s.img.sprite = sprite;
            Material m = s.mat;
            m.SetColor("_RedHighTint", skin.uiPrimaryColorHigh);
            m.SetColor("_GreenHighTint", skin.uiSecondaryColorHigh);
            m.SetColor("_BlueHighTint", skin.uiTertiaryColorHigh);
            m.SetColor("_RedLowTint", skin.uiPrimaryColorLow);
            m.SetColor("_GreenLowTint", skin.uiSecondaryColorLow);
            m.SetColor("_BlueLowTint", skin.uiTertiaryColorLow);
            m.SetFloat("_RedHighLowThreshold", skin.uiPrimaryColorMidpoint);
            m.SetFloat("_GreenHighLowThreshold", skin.uiSecondaryColorMidpoint);
            m.SetFloat("_BlueHighLowThreshold", skin.uiTertiaryColorMidpoint);
            m.SetFloat("_RedSmoothness", skin.uiPrimaryColorSmoothness);
            m.SetFloat("_GreenSmoothness", skin.uiSecondaryColorSmoothness);
            m.SetFloat("_BlueSmoothness", skin.uiTertiaryColorSmoothness);
        }

        static int Wrap(int i, int n) => ((i % n) + n) % n;

        void Animate()
        {
            int count = ships.Length;
            if (count == 0) return;
            bool forced = !string.IsNullOrEmpty(Globals.forceShipChoice);

            // follow the game's choice (arrows, gamepad, a tap on a neighbour)
            string ship = CurrentShip();
            if (ship != shownShip)
            {
                int was = Wrap(Mathf.RoundToInt(target), count), now = CurrentIndex();
                shownShip = ship;
                if (now >= 0)
                {
                    int d = Wrap(now - was, count);
                    if (d > count / 2) d -= count;                        // the short way round (2 ships: forward)
                    target += d;
                }
            }
            float dt = Mathf.Min(Time.unscaledDeltaTime, 0.05f);
            pos = Mathf.Abs(target - pos) < 0.002f ? target : pos + (target - pos) * (1f - Mathf.Exp(-14f * dt));

            Rect r = area.rect;
            float W = r.width, H = r.height;
            float boxH = H * 0.56f, boxW = Mathf.Min(W * 0.4f, boxH * 2.4f);
            float spacing = W * 0.29f, y = H * 0.06f;
            int b = Mathf.FloorToInt(pos);
            string chosenShip = ship, chosenSkin = CurrentSkin();
            bool canTap = panel.PlayerConnected && !panel.IsLockedIn && !forced;

            int newOrder = 0;
            for (int i = 0; i < slots.Length; i++)
            {
                Slot s = slots[i];
                int k = i - 2;
                float o = b + k - pos;                                    // offset from the middle, in ships
                float a = Mathf.Abs(o);
                float alpha = a < 0.02f ? 1f : a <= 1f ? Mathf.Lerp(1f, SideAlpha, a) : Mathf.Lerp(SideAlpha, 0f, a - 1f);
                if (count == 1 || forced) alpha = a < 0.5f ? 1f : 0f;
                else if (count == 2 && o < 0f) alpha *= Mathf.Clamp01(1f + o);   // two ships: the neighbour is on the right
                s.offset = o;
                bool show = alpha > 0.01f;
                if (s.rt.gameObject.activeSelf != show) s.rt.gameObject.SetActive(show);
                if (!show) continue;

                ShipData sd = forced ? FindShip(chosenShip) : ships[Wrap(b + k, count)];
                if (sd == null) { s.rt.gameObject.SetActive(false); continue; }
                Dress(s, sd, SkinFor(sd, chosenShip, chosenSkin));

                float scale = a <= 1f ? Mathf.Lerp(1f, Side, a) : Mathf.Lerp(Side, Side * 0.75f, a - 1f);
                float x = a <= 1f ? o * spacing : Mathf.Sign(o) * (spacing + (a - 1f) * spacing * 0.55f);
                s.rt.anchoredPosition = new Vector2(x, y);
                s.rt.sizeDelta = new Vector2(boxW, boxH) * scale;
                float v = a < 0.02f ? 1f : a <= 1f ? Mathf.Lerp(1f, SideShade, a) : SideShade;
                s.img.color = new Color(v, v, v, alpha);
                s.button.interactable = canTap && a >= 0.5f && a < 1.6f;
                newOrder = newOrder * 7 + Mathf.Clamp(Mathf.RoundToInt(a * 2f), 0, 6);
            }

            // the middle ship in front of its neighbours while they slide past each other
            if (newOrder != order)
            {
                order = newOrder;
                var byDistance = slots.OrderByDescending(sl => Mathf.Abs(sl.offset));
                int at = 0;
                foreach (Slot sl in byDistance) sl.rt.SetSiblingIndex(at++);
            }
        }

        ShipData FindShip(string name)
        {
            foreach (ShipData s in ships) if (s.name == name) return s;
            return ships.Length > 0 ? ships[0] : null;
        }
    }
}
