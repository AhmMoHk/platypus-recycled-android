// Platypus Reclayed Android port: a small stand-in for the Rewired input library (whose shipped build is
// obfuscated and Windows-bound). It implements only what the game uses, driven by the on-screen clay controls,
// a keyboard or any gamepad. Single player: Players = [player 0]; the system player drives the menus.
using System;
using System.Collections.Generic;
using UnityEngine;

namespace PlatypusAndroid
{
    /// <summary>State written by the on-screen touch controls (TouchControls in the game assembly).</summary>
    public static class TouchInput
    {
        public static float x, y;            // joystick, -1..1 (dx / maxRadius, dy / maxRadius)
        public static bool fire;             // fire button held
        public static bool pause;            // pause button pressed this frame
        public static bool back;             // Android back / Back button pressed this frame
        public static bool anyTouchDown;     // a finger went down this frame
        public static bool gameplayVisible;  // the joystick and fire button are on screen
    }
}

namespace PlatypusAndroid
{
    /// <summary>No controller button glyphs on a phone: the glyph lookup returns NoGlyph and texts drop it
    /// together with its separator ("O - BACK" -> "BACK", "Change color O" -> "Change color").</summary>
    public static class Hints
    {
        public const char NoGlyph = '\uE000';
        static readonly System.Text.RegularExpressions.Regex rx = new System.Text.RegularExpressions.Regex(@"[ \t]*[-:\u2013]?[ \t]*\uE000[ \t]*[-:\u2013]?[ \t]*");
        public static string Clean(string s)
        {
            if (string.IsNullOrEmpty(s) || s.IndexOf(NoGlyph) < 0) return s;
            s = rx.Replace(s, " ");
            return s.Trim(' ', '\t');
        }
    }
}

namespace Rewired
{
    public enum ControllerType { Keyboard = 0, Mouse = 1, Joystick = 2, Custom = 20 }
    public enum UpdateLoopType { Update = 0, FixedUpdate = 1, OnGUI = 2 }
    public enum InputActionEventType { Update = 0, ButtonJustPressed = 4, ButtonPressed = 6, ButtonJustReleased = 8, AxisActive = 12, AxisRawActive = 13, ButtonJustPressedForTime = 15 }
    public enum ControllerElementType { Axis = 0, Button = 1 }
    public enum Pole { Positive = 0, Negative = 1 }
    public enum AxisRange { Full = 0, Positive = 1, Negative = 2 }

    public class ControllerStatusChangedEventArgs : EventArgs
    {
        public string name; public int controllerId; public ControllerType controllerType;
        public Controller controller => ReInput.controllers.GetController(controllerType, controllerId);
    }
    public class ControllerAssignmentChangedEventArgs : EventArgs
    {
        public Player player; public Controller controller; public bool state;
    }

    public struct InputActionEventData
    {
        public int playerId; public string actionName; public InputActionEventType eventType; public UpdateLoopType updateLoop;
        public Player player => ReInput.players.GetPlayer(playerId);
        public string actionDescriptiveName => actionName;
        public bool GetButton() => player.GetButton(actionName);
        public bool GetButtonDown() => player.GetButtonDown(actionName);
        public bool GetButtonUp() => player.GetButtonUp(actionName);
        public float GetAxis() => player.GetAxis(actionName);
        public float GetAxisRaw() => player.GetAxisRaw(actionName);
    }

    public class ActionElementMap
    {
        public string actionDescriptiveName = ""; public string elementIdentifierName = ""; public int actionId; public int elementIdentifierId;
        public ControllerElementType elementType; public Pole axisContribution; public AxisRange axisRange; public KeyCode keyCode;
        public ControllerMap controllerMap;
    }

    public class ControllerMap
    {
        public string categoryName = "Default"; public int categoryId; public bool enabled = true; public int id;
        public ControllerType controllerType; public Controller controller;
        // no button glyphs or prompts on a phone: maps report no elements
        public ActionElementMap[] GetElementMapsWithAction(string action) => new ActionElementMap[0];
        public ActionElementMap[] GetElementMapsWithAction(int action) => new ActionElementMap[0];
        public IList<ActionElementMap> AllMaps => new ActionElementMap[0];
        public IList<ActionElementMap> ButtonMaps => new ActionElementMap[0];
        public IList<ActionElementMap> AxisMaps => new ActionElementMap[0];
    }

    public class Controller
    {
        public class Element { public string name = "Button"; public ControllerElementType type = ControllerElementType.Button; }
        public class Button : Element
        {
            internal Controller owner;
            public bool value => owner != null && owner.any;
            public bool justPressed => owner != null && owner.any && !owner.anyPrev;
            public bool justReleased => owner != null && !owner.any && owner.anyPrev;
            public bool justChangedState => justPressed || justReleased;
        }
        public readonly int id;
        public readonly ControllerType type;
        public readonly string name;
        internal bool any, anyPrev;
        internal Controller(int id, ControllerType t, string n) { this.id = id; type = t; name = n; }
        public string hardwareName => name;
        public string hardwareIdentifier => name;
        public Guid hardwareTypeGuid => Guid.Empty;
        public Guid deviceInstanceGuid => Guid.Empty;
        public bool isConnected => true;
        public bool enabled { get; set; } = true;
        public int buttonCount => 1;
        public IList<Button> Buttons => new Button[] { new Button { owner = this } };
        public string tag { get; set; } = "";
        public bool GetAnyButton() => any;
        public bool GetAnyButtonDown() => any && !anyPrev;
        public bool GetAnyButtonUp() => !any && anyPrev;
        public bool GetAnyButtonPrev() => anyPrev;
        public bool GetButtonDown(int index) => GetAnyButtonDown();
        public bool GetButton(int index) => any;
        public float GetLastTimeActive() => 0;
    }
    public class ControllerWithMap : Controller { internal ControllerWithMap(int id, ControllerType t, string n) : base(id, t, n) { } }
    public class ControllerWithAxes : ControllerWithMap { internal ControllerWithAxes(int id, ControllerType t, string n) : base(id, t, n) { } }
    public class Keyboard : ControllerWithMap { internal Keyboard() : base(0, ControllerType.Keyboard, "Keyboard") { } public bool GetKey(KeyCode k) => Input.GetKey(k); public bool GetKeyDown(KeyCode k) => Input.GetKeyDown(k); }
    public class Mouse : ControllerWithAxes { internal Mouse() : base(0, ControllerType.Mouse, "Mouse") { } public Vector2 screenPosition => Input.mousePosition; }
    public class Joystick : ControllerWithAxes
    {
        internal Joystick(int id, string n) : base(id, ControllerType.Joystick, n) { }
        public bool supportsVibration => false;
        public void SetVibration(int motor, float level) { }
        public void SetVibration(int motor, float level, float duration) { }
        public void StopVibration() { }
    }
    public class CustomController : ControllerWithAxes { internal CustomController(int id, string n) : base(id, ControllerType.Custom, n) { } }

    public class Player
    {
        public class ControllerHelper
        {
            public class MapHelper
            {
                readonly Player p; internal MapHelper(Player p) { this.p = p; }
                public IList<ControllerMap> GetMaps(ControllerType t, int id) => new ControllerMap[0];
                public IList<ControllerMap> GetMaps(Controller c) => new ControllerMap[0];
                public IEnumerable<ControllerMap> GetAllMaps() => new ControllerMap[0];
                public IEnumerable<ControllerMap> GetAllMaps(ControllerType t) => new ControllerMap[0];
                public ControllerMap GetMap(ControllerType t, int controllerId, string category, string layout) => null;
                public ControllerMap GetFirstMapInCategory(ControllerType t, int controllerId, string category) => null;
                public ControllerMap GetFirstMapInCategory(Controller c, string category) => null;
                public IEnumerable<ActionElementMap> ElementMapsWithAction(string action, bool skipDisabledMaps) => new ActionElementMap[0];
                public IEnumerable<ActionElementMap> ElementMapsWithAction(ControllerType t, string action, bool skipDisabledMaps) => new ActionElementMap[0];
                public IEnumerable<ActionElementMap> ButtonMapsWithAction(ControllerType t, string action, bool skipDisabledMaps) => new ActionElementMap[0];
                public ActionElementMap GetFirstElementMapWithAction(string action, bool skipDisabledMaps) => null;
                public ActionElementMap GetFirstElementMapWithAction(ControllerType t, string action, bool skipDisabledMaps) => null;
                public ActionElementMap GetFirstButtonMapWithAction(string action, bool skipDisabledMaps) => null;
                public ActionElementMap GetFirstButtonMapWithAction(ControllerType t, string action, bool skipDisabledMaps) => null;
                public int SetMapsEnabled(bool state, string category) => 0;
                public int SetMapsEnabled(bool state, ControllerType t, string category) => 0;
                public int SetMapsEnabled(bool state, string category, string layout) => 0;
                public void SetAllMapsEnabled(bool state) { }
                public void SetAllMapsEnabled(bool state, ControllerType t) { }
                public void LoadMap(ControllerType t, int controllerId, string category, string layout) { }
                public void LoadDefaultMaps(ControllerType t) { }
                public void ClearAllMaps(bool userAssignableOnly) { }
            }
            readonly Player p;
            internal readonly List<Controller> list = new List<Controller>();
            public readonly MapHelper maps;
            internal ControllerHelper(Player p) { this.p = p; maps = new MapHelper(p); }
            public bool hasKeyboard { get => list.Contains(ReInput.keyboard); set { if (value) AddController(ReInput.keyboard, false); else RemoveController(ReInput.keyboard); } }
            public bool hasMouse { get => list.Contains(ReInput.mouse); set { if (value) AddController(ReInput.mouse, false); else RemoveController(ReInput.mouse); } }
            public bool excludeFromControllerAutoAssignment { get; set; }
            public int controllerCount => list.Count;
            public int joystickCount { get { int n = 0; foreach (var c in list) if (c.type == ControllerType.Joystick) n++; return n; } }
            public IList<Controller> Controllers => list;
            public IList<Joystick> Joysticks { get { var r = new List<Joystick>(); foreach (var c in list) if (c is Joystick j) r.Add(j); return r; } }
            public Keyboard Keyboard => ReInput.keyboard;
            public Mouse Mouse => ReInput.mouse;
            public void AddController(Controller c, bool removeFromOtherPlayers)
            {
                if (c == null || list.Contains(c)) return;
                if (removeFromOtherPlayers) foreach (var o in ReInput.players.AllPlayers) if (o != p && !o.isSystem) o.controllers.RemoveController(c);
                list.Add(c);
                ControllerAddedEvent?.Invoke(new ControllerAssignmentChangedEventArgs { player = p, controller = c, state = true });
                // the touch screen is the mouse in menus and the clay joystick in gameplay: one device, two facets
                if (!p.isSystem && (c == ReInput.mouse || c == ReInput.touch)) AddController(c == ReInput.mouse ? ReInput.touch : ReInput.mouse, removeFromOtherPlayers);
            }
            public void AddController(ControllerType t, int id, bool removeFromOtherPlayers) => AddController(ReInput.controllers.GetController(t, id), removeFromOtherPlayers);
            public bool RemoveController(Controller c)
            {
                if (c == null || !list.Remove(c)) return false;
                ControllerRemovedEvent?.Invoke(new ControllerAssignmentChangedEventArgs { player = p, controller = c, state = false });
                if (!p.isSystem && (c == ReInput.mouse || c == ReInput.touch)) RemoveController(c == ReInput.mouse ? ReInput.touch : ReInput.mouse);
                return true;
            }
            public bool RemoveController(ControllerType t, int id) => RemoveController(ReInput.controllers.GetController(t, id));
            public void ClearAllControllers() { foreach (var c in list.ToArray()) RemoveController(c); }
            public void ClearControllersOfType(ControllerType t) { foreach (var c in list.ToArray()) if (c.type == t) RemoveController(c); }
            public bool ContainsController(Controller c) => c != null && list.Contains(c);
            public bool ContainsController(ControllerType t, int id) => ContainsController(ReInput.controllers.GetController(t, id));
            public Controller GetController(ControllerType t, int id) { foreach (var c in list) if (c.type == t && c.id == id) return c; return null; }
            public Controller GetLastActiveController() => ReInput.lastActive != null && list.Contains(ReInput.lastActive) ? ReInput.lastActive : (list.Count > 0 ? list[0] : null);
            public Controller GetLastActiveController(ControllerType t) { var c = GetLastActiveController(); return c != null && c.type == t ? c : null; }
            public event Action<ControllerAssignmentChangedEventArgs> ControllerAddedEvent;
            public event Action<ControllerAssignmentChangedEventArgs> ControllerRemovedEvent;
            readonly List<PlayerActiveControllerChangedDelegate> lastActiveDelegates = new List<PlayerActiveControllerChangedDelegate>();
            public void AddLastActiveControllerChangedDelegate(PlayerActiveControllerChangedDelegate d) { if (d != null) lastActiveDelegates.Add(d); }
            public void RemoveLastActiveControllerChangedDelegate(PlayerActiveControllerChangedDelegate d) { lastActiveDelegates.Remove(d); }
            public void RemoveLastActiveControllerChangedDelegate() { lastActiveDelegates.Clear(); }
            internal void FireLastActive(Controller c) { foreach (var d in lastActiveDelegates.ToArray()) d(p, c); }
        }

        public readonly int id;
        public readonly string name;
        public readonly bool isSystem;
        public readonly ControllerHelper controllers;
        internal Player(int id, string name, bool system) { this.id = id; this.name = name; isSystem = system; controllers = new ControllerHelper(this); }
        public string descriptiveName => name;
        public bool isPlaying { get; set; } = true;

        internal readonly Dictionary<string, float> cur = new Dictionary<string, float>(), prev = new Dictionary<string, float>();
        float Val(Dictionary<string, float> d, string a) => a != null && d.TryGetValue(a, out float v) ? v : 0f;
        public float GetAxis(string action) => Val(cur, action);
        public float GetAxisRaw(string action) => Val(cur, action);
        public float GetAxisPrev(string action) => Val(prev, action);
        public float GetAxisDelta(string action) => Val(cur, action) - Val(prev, action);
        public bool GetButton(string action) => Val(cur, action) > 0.5f;
        public bool GetButtonDown(string action) => Val(cur, action) > 0.5f && Val(prev, action) <= 0.5f;
        public bool GetButtonUp(string action) => Val(cur, action) <= 0.5f && Val(prev, action) > 0.5f;
        public bool GetButtonPrev(string action) => Val(prev, action) > 0.5f;
        public bool GetNegativeButton(string action) => Val(cur, action) < -0.5f;
        public bool GetNegativeButtonDown(string action) => Val(cur, action) < -0.5f && Val(prev, action) >= -0.5f;
        public bool GetNegativeButtonUp(string action) => Val(cur, action) >= -0.5f && Val(prev, action) < -0.5f;
        public bool GetButtonRepeating(string action) => GetButtonDown(action);
        public bool GetButtonSinglePressDown(string action) => GetButtonDown(action);
        public bool GetButtonDoublePressDown(string action) => false;
        public bool GetAnyButton() { foreach (var kv in cur) if (kv.Value > 0.5f) return true; return false; }
        public bool GetAnyButtonDown() { foreach (var kv in cur) if (kv.Value > 0.5f && Val(prev, kv.Key) <= 0.5f) return true; return false; }
        public bool GetAnyButtonUp() { foreach (var kv in prev) if (kv.Value > 0.5f && Val(cur, kv.Key) <= 0.5f) return true; return false; }
        public bool GetAnyNegativeButton() { foreach (var kv in cur) if (kv.Value < -0.5f) return true; return false; }
        public float GetButtonTimePressed(string action) => 0f;
        public void SetVibration(int motor, float level) { }
        public void SetVibration(int motor, float level, float duration) { }
        public void SetVibration(int motor, float level, bool stopOtherMotors) { }
        public void SetVibration(int motor, float level, float duration, bool stopOtherMotors) { }
        public void StopVibration() { }

        struct Handler { public Action<InputActionEventData> cb; public UpdateLoopType loop; public InputActionEventType type; public string action; }
        readonly List<Handler> handlers = new List<Handler>();
        public void AddInputEventDelegate(Action<InputActionEventData> cb, UpdateLoopType loop, InputActionEventType type, string action) => handlers.Add(new Handler { cb = cb, loop = loop, type = type, action = action });
        public void AddInputEventDelegate(Action<InputActionEventData> cb, UpdateLoopType loop, InputActionEventType type) => handlers.Add(new Handler { cb = cb, loop = loop, type = type });
        public void AddInputEventDelegate(Action<InputActionEventData> cb, UpdateLoopType loop) => handlers.Add(new Handler { cb = cb, loop = loop, type = InputActionEventType.Update });
        public void RemoveInputEventDelegate(Action<InputActionEventData> cb) => handlers.RemoveAll(h => h.cb == cb);
        public void RemoveInputEventDelegate(Action<InputActionEventData> cb, UpdateLoopType loop, InputActionEventType type, string action) => handlers.RemoveAll(h => h.cb == cb && h.action == action);
        public void RemoveInputEventDelegate(Action<InputActionEventData> cb, UpdateLoopType loop, InputActionEventType type) => handlers.RemoveAll(h => h.cb == cb && h.type == type);
        public void ClearInputEventDelegates() => handlers.Clear();

        internal void Dispatch(UpdateLoopType loop)
        {
            if (handlers.Count == 0) return;
            foreach (var h in handlers.ToArray())
            {
                if (h.loop != loop) continue;
                foreach (string a in h.action != null ? new[] { h.action } : ReInput.Actions)
                {
                    bool fire = false;
                    switch (h.type)
                    {
                        case InputActionEventType.Update: fire = true; break;
                        case InputActionEventType.ButtonJustPressed: fire = GetButtonDown(a); break;
                        case InputActionEventType.ButtonPressed: fire = GetButton(a); break;
                        case InputActionEventType.ButtonJustReleased: fire = GetButtonUp(a); break;
                        case InputActionEventType.AxisActive: case InputActionEventType.AxisRawActive: fire = GetAxis(a) != 0; break;
                        default: fire = GetButtonDown(a); break;
                    }
                    if (fire) h.cb(new InputActionEventData { playerId = id, actionName = a, eventType = h.type, updateLoop = loop });
                }
            }
        }
    }

    public delegate void PlayerActiveControllerChangedDelegate(Player player, Controller controller);

    public static class ReInput
    {
        public sealed class PlayerHelper
        {
            internal readonly List<Player> players = new List<Player>(), all = new List<Player>();
            public IList<Player> Players => players;
            public IList<Player> AllPlayers => all;
            public Player SystemPlayer => system;
            public Player GetSystemPlayer() => system;
            public int playerCount => players.Count;
            public int allPlayerCount => all.Count;
            public Player GetPlayer(int id) { if (id == 9999999) return system; foreach (var p in all) if (p.id == id) return p; return ghost(id); }
            public Player GetPlayer(string name) { foreach (var p in all) if (p.name == name) return p; return null; }
            readonly Dictionary<int, Player> ghosts = new Dictionary<int, Player>();
            Player ghost(int id) { if (!ghosts.TryGetValue(id, out var p)) ghosts[id] = p = new Player(id, "Player" + id, false); return p; }   // single player: other ids read nothing
        }
        public sealed class ControllerHelper
        {
            internal readonly List<Controller> list = new List<Controller>();
            public IList<Controller> Controllers => list;
            public IList<Joystick> Joysticks { get { var r = new List<Joystick>(); foreach (var c in list) if (c is Joystick j) r.Add(j); return r; } }
            public int controllerCount => list.Count;
            public int joystickCount => Joysticks.Count;
            public Keyboard Keyboard => keyboard;
            public Mouse Mouse => mouse;
            public Controller GetController(ControllerType t, int id) { foreach (var c in list) if (c.type == t && c.id == id) return c; return null; }
            public Joystick GetJoystick(int id) => GetController(ControllerType.Joystick, id) as Joystick;
            public Controller GetLastActiveController() => lastActive ?? touch;
            public Controller GetLastActiveController(ControllerType t) { var c = GetLastActiveController(); return c != null && c.type == t ? c : null; }
            public ControllerType GetLastActiveControllerType() => GetLastActiveController().type;
            public bool GetAnyButton() { foreach (var c in list) if (c.GetAnyButton()) return true; return false; }
            public bool GetAnyButtonDown() { foreach (var c in list) if (c.GetAnyButtonDown()) return true; return false; }
            public bool IsControllerAssigned(ControllerType t, int id) { var c = GetController(t, id); foreach (var p in players.players) if (p.controllers.ContainsController(c)) return true; return false; }
            public void AutoAssignJoysticks() { }
        }

        public static readonly string[] Actions = { "MoveHorizontal", "MoveVertical", "Fire", "Pause", "UIHorizontal", "UIVertical", "UISubmit", "UICancel", "UISkinChange" };
        public static readonly PlayerHelper players = new PlayerHelper();
        public static readonly ControllerHelper controllers = new ControllerHelper();
        internal static readonly Keyboard keyboard = new Keyboard();
        internal static readonly Mouse mouse = new Mouse();
        internal static readonly Joystick touch = new Joystick(0, "Touch Screen");      // the clay on-screen controls
        internal static readonly Joystick pad = new Joystick(1, "Gamepad");
        internal static Controller lastActive;
        static Player system;
        public static bool isReady => true;
        public static float unscaledTime => Time.unscaledTime;
        public static int currentFrame => Time.frameCount;
        public static event Action<ControllerStatusChangedEventArgs> ControllerConnectedEvent { add { } remove { } }
        public static event Action<ControllerStatusChangedEventArgs> ControllerDisconnectedEvent { add { } remove { } }
        public static event Action<ControllerStatusChangedEventArgs> ControllerPreDisconnectEvent { add { } remove { } }
        public static event Action InputSourceUpdateEvent;
        public static event Action ShutDownEvent { add { } remove { } }

        static ReInput()
        {
            system = new Player(9999999, "System", true);
            var p0 = new Player(0, "Player0", false);
            players.players.Add(p0);
            players.all.Add(system);
            players.all.Add(p0);
            controllers.list.Add(keyboard);
            controllers.list.Add(mouse);
            controllers.list.Add(touch);
            controllers.list.Add(pad);
            system.controllers.list.AddRange(new Controller[] { keyboard, mouse, touch, pad });
        }

        [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
        static void Boot()
        {
            var go = new GameObject("[Input]");
            UnityEngine.Object.DontDestroyOnLoad(go);
            go.hideFlags = HideFlags.HideInHierarchy;
            go.AddComponent<InputDriver>();
        }

        static float Key(KeyCode a, KeyCode b = KeyCode.None) => Input.GetKey(a) || (b != KeyCode.None && Input.GetKey(b)) ? 1f : 0f;
        static float Pad(string axis) { try { return Input.GetAxisRaw(axis); } catch (ArgumentException) { return 0f; } }
        static bool Btn(int n) => Input.GetKey(KeyCode.JoystickButton0 + n);

        /// <summary>sampled once per frame, before any game Update</summary>
        internal static void Sample()
        {
            float kx = Key(KeyCode.RightArrow, KeyCode.D) - Key(KeyCode.LeftArrow, KeyCode.A);
            float ky = Key(KeyCode.UpArrow, KeyCode.W) - Key(KeyCode.DownArrow, KeyCode.S);
            float kFire = Key(KeyCode.Space, KeyCode.Z), kSubmit = Key(KeyCode.Return, KeyCode.Space), kCancel = Key(KeyCode.Escape, KeyCode.Backspace);
            float kPause = Key(KeyCode.Escape, KeyCode.P);
            float px = Pad("Horizontal"), py = Pad("Vertical");
            float pFire = Btn(0) || Btn(2) ? 1f : 0f, pSubmit = Btn(0) ? 1f : 0f, pCancel = Btn(1) ? 1f : 0f, pPause = Btn(7) || Btn(9) || Btn(10) ? 1f : 0f, pSkin = Btn(3) ? 1f : 0f;
            float tx = PlatypusAndroid.TouchInput.x, ty = PlatypusAndroid.TouchInput.y, tFire = PlatypusAndroid.TouchInput.fire ? 1f : 0f;
            float tBack = PlatypusAndroid.TouchInput.back ? 1f : 0f, tPause = PlatypusAndroid.TouchInput.pause ? 1f : 0f;

            // Touch outside gameplay acts as the game's own mouse controls (left click = "Fire", the UI is clicked
            // through the EventSystem): a touch that starts while the joystick is hidden presses the mouse button.
            bool gameplay = PlatypusAndroid.TouchInput.gameplayVisible;
            bool pointerDown = Input.touchCount > 0 || Input.GetMouseButton(0), began = Input.GetMouseButtonDown(0);
            for (int i = 0; i < Input.touchCount; i++) if (Input.GetTouch(i).phase == TouchPhase.Began) began = true;
            if (began && !gameplay) { mouseArmed = true; mouseHoldUntil = Time.unscaledTime + 0.12f; }   // a quick tap still lasts a few frames
            if (!pointerDown && Time.unscaledTime >= mouseHoldUntil) mouseArmed = false;
            float mFire = mouseArmed && !gameplay ? 1f : 0f;        // a finger lifting after "Resume" does not fire

            keyboard.anyPrev = keyboard.any; keyboard.any = Input.anyKey && !pointerDown && pSubmit + pFire + pCancel + pPause == 0;
            mouse.anyPrev = mouse.any; mouse.any = mFire > 0;
            touch.anyPrev = touch.any; touch.any = gameplay && (tFire > 0 || Mathf.Abs(tx) > 0.2f || Mathf.Abs(ty) > 0.2f);
            pad.anyPrev = pad.any; pad.any = pFire + pSubmit + pCancel + pPause + pSkin > 0 || Mathf.Abs(px) > 0.5f || Mathf.Abs(py) > 0.5f;
            foreach (var c in new Controller[] { keyboard, mouse, touch, pad }) if (c.GetAnyButtonDown()) SetLastActive(c);
            if (tPause > 0) SetLastActive(mouse);          // the pause menu is then used by touch

            foreach (var pl in players.all)
            {
                var hc = pl.controllers;
                bool k = hc.ContainsController(keyboard), t = hc.ContainsController(touch), g = hc.ContainsController(pad), m = hc.ContainsController(mouse);
                float mx = Mathf.Clamp((k ? kx : 0) + (g ? px : 0) + (t ? tx : 0), -1f, 1f);
                float my = Mathf.Clamp((k ? ky : 0) + (g ? py : 0) + (t ? ty : 0), -1f, 1f);
                var d = pl.prev; pl.prev.Clear(); foreach (var kv in pl.cur) d[kv.Key] = kv.Value;
                pl.cur["MoveHorizontal"] = mx;
                pl.cur["MoveVertical"] = my;
                pl.cur["Fire"] = Mathf.Max(Mathf.Max(k ? kFire : 0, g ? pFire : 0), Mathf.Max(t ? tFire : 0, m ? mFire : 0));
                pl.cur["Pause"] = Mathf.Max(k ? kPause : 0, g ? pPause : 0, t ? tPause : 0);
                pl.cur["UIHorizontal"] = Mathf.Abs(mx) > 0.5f ? Mathf.Sign(mx) : 0f;
                pl.cur["UIVertical"] = Mathf.Abs(my) > 0.5f ? Mathf.Sign(my) : 0f;
                pl.cur["UISubmit"] = Mathf.Max(k ? kSubmit : 0, g ? pSubmit : 0, t ? tFire : 0);
                pl.cur["UICancel"] = Mathf.Max(k ? kCancel : 0, g ? pCancel : 0, t ? tBack : 0);
                pl.cur["UISkinChange"] = g ? pSkin : 0;
            }
            InputSourceUpdateEvent?.Invoke();
            PlatypusAndroid.TouchInput.back = PlatypusAndroid.TouchInput.pause = PlatypusAndroid.TouchInput.anyTouchDown = false;
        }

        static bool mouseArmed;
        static float mouseHoldUntil;
        static void SetLastActive(Controller c)
        {
            if (lastActive == c) return;
            lastActive = c;
            foreach (var pl in players.all) pl.controllers.FireLastActive(c);
        }

        internal static void Dispatch(UpdateLoopType loop) { foreach (var p in players.all) p.Dispatch(loop); }
    }

    [DefaultExecutionOrder(-32000)]
    internal sealed class InputDriver : MonoBehaviour
    {
        void Update() { ReInput.Sample(); ReInput.Dispatch(UpdateLoopType.Update); }
        void FixedUpdate() { ReInput.Dispatch(UpdateLoopType.FixedUpdate); }
    }
}
