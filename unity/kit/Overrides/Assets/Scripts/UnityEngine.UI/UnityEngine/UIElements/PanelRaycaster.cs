// Platypus Reclayed Android port: see PanelEventHandler.cs (no UI Toolkit panels in this game).
using System.Collections.Generic;
using UnityEngine.EventSystems;

namespace UnityEngine.UIElements
{
    [AddComponentMenu("")]
    public class PanelRaycaster : BaseRaycaster
    {
        public IPanel panel { get; set; }
        public override Camera eventCamera => null;
        public override void Raycast(PointerEventData eventData, List<RaycastResult> resultAppendList) { }
    }
}
