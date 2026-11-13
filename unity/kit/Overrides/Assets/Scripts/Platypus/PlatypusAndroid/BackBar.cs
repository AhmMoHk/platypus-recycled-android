// Platypus Reclayed Android port: the black bar at the bottom of the menus. On PC it lists the controls of the
// screen (Select, Back, Start, Navigate...). On a phone only BACK is kept, once, in the middle of the bar, and only
// where the screen has no Back item of its own: not on "Press fire", not in the Hall of Fame, not in the options
// screens with their own Back (except the Reclayed music selector). Pressed, BACK turns red like the selected menu
// items and makes the sound of a menu button.
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

namespace PlatypusAndroid
{
    public static class BackBar
    {
        static readonly InputHints.Hint[] None = new InputHints.Hint[0];
        static InputHints.Hint[] from;                 // the game's hints the pick below was made of
        static InputHints.Hint[] picked = None;

        sealed class SceneInfo
        {
            public bool score;                                             // the Hall of Fame / score screen
            public MainMenuManager menu;
            public readonly Dictionary<int, bool> ownBack = new Dictionary<int, bool>();
        }

        static readonly Dictionary<int, SceneInfo> scenes = new Dictionary<int, SceneInfo>();
        static bool hooked;
        static readonly List<RaycastResult> hits = new List<RaycastResult>();

        /// <summary>The hints to show: BACK alone, or nothing. Called by InputHints every frame; returns the same
        /// array while nothing changes.</summary>
        public static InputHints.Hint[] Pick(InputHints.Hint[] hints, Component owner)
        {
            if (hints == null || owner == null || Hidden(Info(owner.gameObject.scene))) return None;
            if (!ReferenceEquals(hints, from))
            {
                from = hints;
                picked = None;
                foreach (InputHints.Hint h in hints)
                    if (h.actionName == "UICancel") { picked = new[] { h }; break; }
            }
            return picked;
        }

        /// <summary>What the scene of a bar is (looked up once per scene).</summary>
        static SceneInfo Info(Scene scene)
        {
            if (!hooked) { hooked = true; SceneManager.sceneUnloaded += s => scenes.Remove(s.handle); }
            if (scenes.TryGetValue(scene.handle, out SceneInfo info) && (info.menu != null || ReferenceEquals(info.menu, null))) return info;
            info = new SceneInfo();
            if (scene.IsValid() && scene.isLoaded)
                foreach (GameObject root in scene.GetRootGameObjects())
                {
                    if (!info.score && root.GetComponentInChildren<ScoreMenu>(true) != null) info.score = true;
                    if (info.menu == null) info.menu = root.GetComponentInChildren<MainMenuManager>(true);
                }
            scenes[scene.handle] = info;
            return info;
        }

        static bool Hidden(SceneInfo info)
        {
            if (info.score) return true;                                   // the Hall of Fame: a tap goes back
            if (info.menu == null || MainMenuManager.menuStack.Count == 0) return false;
            MainMenuManager.SubMenu m = MainMenuManager.menuStack.Peek();
            switch (m)
            {
                case MainMenuManager.SubMenu.PressFireToStart: return true;
                case MainMenuManager.SubMenu.MusicCustomiser:
                case MainMenuManager.SubMenu.MusicTrackSelection: return false;
            }
            return OwnBack(info, m);
        }

        /// <summary>The panel of this main menu screen has a "Back" item of its own.</summary>
        static bool OwnBack(SceneInfo info, MainMenuManager.SubMenu m)
        {
            if (info.ownBack.TryGetValue((int)m, out bool has)) return has;
            bool shown = false;
            has = false;
            foreach (MainMenuSubMenu sm in info.menu.subMenus)
            {
                if (sm == null || sm.menu != m || !sm.gameObject.activeInHierarchy) continue;
                shown = true;
                foreach (ButtonScript b in sm.GetComponentsInChildren<ButtonScript>(true))
                    if (b != null && b.localizedText == "MainMenu.Back") { has = true; break; }
                if (has) break;
            }
            if (shown) info.ownBack[(int)m] = has;                         // decided once the panel is up
            return has;
        }

        /// <summary>Gives a bar button its press: red while a finger is on it, a menu sound, then the screen's own Back (what the
        /// Back key does there, e.g. the level select first closes an open level panel).</summary>
        public static void Dress(Button button)
        {
            if (button.GetComponent<BackBarPress>() == null) button.gameObject.AddComponent<BackBarPress>();
            button.onClick.AddListener(() =>
            {
                Audio.PlaySound("s_ui_submit");
                MainMenuManager menu = button != null ? Info(button.gameObject.scene).menu : null;
                if (menu != null) menu.NavigateBack();                     // the main menu's way back, without its own second sound
                else TouchInput.back = true;                               // the screen's Back key
            });
        }

        /// <summary>The finger (or mouse) that pressed is on a button: Hall of Fame taps elsewhere leave at once.</summary>
        public static bool PointerOnButton()
        {
            if (Input.touchCount == 0 && !Input.GetMouseButton(0)) return false;      // a key or a gamepad button
            Vector2 pos = Input.touchCount > 0 ? Input.GetTouch(0).position : (Vector2)Input.mousePosition;
            EventSystem es = EventSystem.current;
            if (es != null)
            {
                hits.Clear();
                es.RaycastAll(new PointerEventData(es) { position = pos }, hits);
                foreach (RaycastResult h in hits)
                {
                    Selectable s = h.gameObject.GetComponentInParent<Selectable>();
                    if (s != null && Usable(s)) return true;
                }
            }
            // the menus switch their raycasters off at times: the buttons' own rectangles
            foreach (Selectable s in Selectable.allSelectablesArray)
            {
                if (s == null || !Usable(s) || !(s.transform is RectTransform rt)) continue;
                Canvas c = s.GetComponentInParent<Canvas>();
                if (c == null) continue;
                Canvas root = c.rootCanvas;
                Camera cam = root.renderMode == RenderMode.ScreenSpaceOverlay ? null : root.worldCamera;
                if (RectTransformUtility.RectangleContainsScreenPoint(rt, pos, cam)) return true;
            }
            return false;
        }

        static readonly List<CanvasGroup> groups = new List<CanvasGroup>();

        static bool Usable(Selectable s)
        {
            if (!s.isActiveAndEnabled || !s.IsInteractable()) return false;
            if (s.GetComponentInParent<InputHints>() != null) return false;          // the (empty) bar
            s.GetComponentsInParent(false, groups);
            float alpha = 1f;
            foreach (CanvasGroup g in groups)
            {
                if (!g.enabled) continue;
                alpha *= g.alpha;
                if (g.ignoreParentGroups) break;
            }
            return alpha > 0.05f;
        }
    }
}
