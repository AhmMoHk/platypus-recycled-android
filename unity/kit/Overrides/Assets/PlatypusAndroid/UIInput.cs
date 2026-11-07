// Platypus Reclayed Android port: see TouchUI.cs.
using UnityEngine.EventSystems;

namespace PlatypusAndroid
{
    /// <summary>The input module's view of the device: UI navigation comes from the input stand-in, and on a phone the
    /// touches are kept from uGUI (TouchUI handles them).</summary>
    public class UIInput : BaseInput
    {
        static Rewired.Player P => Rewired.ReInput.players.SystemPlayer;

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

        public override int touchCount => TouchUI.Active ? 0 : base.touchCount;
    }
}
