// UGUI input module of the port: keyboard / gamepad navigation comes from the system player of RewiredFacade
// (UIHorizontal, UIVertical, UISubmit, UICancel); a PC mouse works as in uGUI; on a phone every touch goes through
// PlatypusAndroid.TouchUI (tap = press the button under the finger).
using UnityEngine;
using UnityEngine.EventSystems;

namespace Rewired.Integration.UnityUI
{
    public class RewiredStandaloneInputModule : StandaloneInputModule
    {
        public bool allowMouseInput = true;
        public bool allowTouchInput = true;
        public bool useAllRewiredGamePlayers = true;
        public bool useRewiredSystemPlayer = true;
        public bool moveOneElementPerAxisPress = true;

        protected override void Awake()
        {
            base.Awake();
            horizontalAxis = "UIHorizontal";
            verticalAxis = "UIVertical";
            submitButton = "UISubmit";
            cancelButton = "UICancel";
            var ui = GetComponent<PlatypusAndroid.UIInput>();
            inputOverride = ui != null ? ui : gameObject.AddComponent<PlatypusAndroid.UIInput>();
            if (PlatypusAndroid.TouchUI.Active) Input.simulateMouseWithTouches = false;   // touches are not a second, fake mouse
        }

        public override bool IsModuleSupported() => true;

        public override bool ShouldActivateModule() => base.ShouldActivateModule() || (PlatypusAndroid.TouchUI.Active && Input.touchCount > 0);

        public override void Process()
        {
            base.Process();
            if (PlatypusAndroid.TouchUI.Active) PlatypusAndroid.TouchUI.Process(eventSystem);
        }
    }
}
