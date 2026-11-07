// Platypus Reclayed Android port: options for the on-screen controls, in Options > Gameplay > Controls where the
// PC mouse options were. "Mouse Sensitivity" becomes "Touch Controls Size", and two copies of that row are made for
// "Touch Controls Opacity" and "Pause Button Opacity". "Allow P2 To Use Mouse" and "Show Mouse Hint" are hidden
// (one player, no mouse on a phone). The pause menu's "Mouse Sensitivity" row gets the same three rows. The values
// are read by TouchControls every frame.
using System;
using Blackdrop.Platforms;
using UnityEngine;
using UnityEngine.UI;

namespace PlatypusAndroid
{
    public static class TouchSettings
    {
        static readonly int[] SizeSteps = { 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150 };
        static readonly int[] OpacitySteps = { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
        const string SizeKey = "AndroidTouchSize", OpacityKey = "AndroidTouchOpacity", PauseKey = "AndroidPauseOpacity";
        const int DefaultSize = 100, DefaultOpacity = 60, DefaultPause = 70;

        static int size = -1, opacity = -1, pause = -1;
        static PlatformStore Store => Platform.Instance.LocalStore;

        static int Read(ref int cache, string key, int def)
        {
            if (cache < 0) { try { cache = Store.GetInt(key, def); } catch { cache = def; } }
            return cache;
        }

        static void Write(ref int cache, string key, int value) { cache = value; Store.SetInt(key, value); }

        /// <summary>Size of the joystick, fire and pause buttons, 0.5 .. 1.5 (1 = the port's default size).</summary>
        public static float Size => Mathf.Clamp(Read(ref size, SizeKey, DefaultSize), 50, 150) / 100f;
        /// <summary>Opacity of the joystick and fire button, 0.1 .. 1.</summary>
        public static float Opacity => Mathf.Clamp(Read(ref opacity, OpacityKey, DefaultOpacity), 10, 100) / 100f;
        /// <summary>Opacity of the pause button, 0.1 .. 1.</summary>
        public static float PauseOpacity => Mathf.Clamp(Read(ref pause, PauseKey, DefaultPause), 10, 100) / 100f;

        static string[] Labels(int[] steps)
        {
            var r = new string[steps.Length];
            for (int i = 0; i < steps.Length; i++) r[i] = steps[i] + "%";
            return r;
        }

        static int IndexOf(int[] steps, int value)
        {
            int best = 0;
            for (int i = 0; i < steps.Length; i++) if (Math.Abs(steps[i] - value) < Math.Abs(steps[best] - value)) best = i;
            return best;
        }

        static void Relabel(Component row, string id)
        {
            var label = row.GetComponent<ButtonScript>();
            if (label == null) return;
            label.localizedText = id;
            if (label.text != null) { label.text.useLocalizedText = true; label.text.localizedTextID = id; label.text.UpdateText(); }
        }

        /// <summary>Called by the options menu in place of the mouse sensitivity row's setup.</summary>
        public static void Setup(OptionSelectorString sizeRow, Component allowP2ToUseMouse, Component showMouseHint, Action changed)
        {
            int rows = 2;                                              // rows added to the list (two copies, minus hidden)
            if (allowP2ToUseMouse != null && allowP2ToUseMouse.gameObject.activeSelf) { allowP2ToUseMouse.gameObject.SetActive(false); rows--; }
            if (showMouseHint != null && showMouseHint.gameObject.activeSelf) { showMouseHint.gameObject.SetActive(false); rows--; }
            if (sizeRow == null) return;

            Transform parent = sizeRow.transform.parent;
            int at = sizeRow.transform.GetSiblingIndex();
            OptionSelectorString opacityRow = UnityEngine.Object.Instantiate(sizeRow, parent, false);
            OptionSelectorString pauseRow = UnityEngine.Object.Instantiate(sizeRow, parent, false);
            opacityRow.name = "Touch Controls Opacity";
            pauseRow.name = "Pause Button Opacity";
            sizeRow.name = "Touch Controls Size";
            opacityRow.transform.SetSiblingIndex(at + 1);
            pauseRow.transform.SetSiblingIndex(at + 2);
            // a list of fixed height (the pause menu's options) grows by the rows added, so its backing grows too
            if (rows != 0 && parent is RectTransform list && list.GetComponent<ContentSizeFitter>() == null)
                list.sizeDelta += new Vector2(0f, rows * ((RectTransform)sizeRow.transform).rect.height);

            Relabel(sizeRow, "Options.TouchSize");
            Relabel(opacityRow, "Options.TouchOpacity");
            Relabel(pauseRow, "Options.PauseOpacity");

            sizeRow.SetOptions(Labels(SizeSteps), null, IndexOf(SizeSteps, Mathf.RoundToInt(Size * 100f)));
            sizeRow.OnOptionChanged += (string o, int i) => { Write(ref size, SizeKey, SizeSteps[i]); changed?.Invoke(); };
            opacityRow.SetOptions(Labels(OpacitySteps), null, IndexOf(OpacitySteps, Mathf.RoundToInt(Opacity * 100f)));
            opacityRow.OnOptionChanged += (string o, int i) => { Write(ref opacity, OpacityKey, OpacitySteps[i]); changed?.Invoke(); };
            pauseRow.SetOptions(Labels(OpacitySteps), null, IndexOf(OpacitySteps, Mathf.RoundToInt(PauseOpacity * 100f)));
            pauseRow.OnOptionChanged += (string o, int i) => { Write(ref pause, PauseKey, OpacitySteps[i]); changed?.Invoke(); };
        }
    }
}
