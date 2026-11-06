// UGUI input module: taps and mouse go to UI elements directly; keyboard / gamepad / touch-control navigation
// comes from the system player of RewiredFacade (UIHorizontal, UIVertical, UISubmit, UICancel).
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

        sealed class FacadeInput : BaseInput
        {
            static Player P => ReInput.players.SystemPlayer;
            public override float GetAxisRaw(string axisName)
            {
                if (axisName == "UIHorizontal" || axisName == "UIVertical") return P.GetAxisRaw(axisName);
                return base.GetAxisRaw(axisName);
            }
            public override bool GetButtonDown(string buttonName)
            {
                if (buttonName == "UISubmit" || buttonName == "UICancel") return P.GetButtonDown(buttonName);
                return base.GetButtonDown(buttonName);
            }
        }

        protected override void Awake()
        {
            base.Awake();
            horizontalAxis = "UIHorizontal";
            verticalAxis = "UIVertical";
            submitButton = "UISubmit";
            cancelButton = "UICancel";
            inputOverride = gameObject.AddComponent<FacadeInput>();
        }

        public override bool IsModuleSupported() => true;
    }
}
