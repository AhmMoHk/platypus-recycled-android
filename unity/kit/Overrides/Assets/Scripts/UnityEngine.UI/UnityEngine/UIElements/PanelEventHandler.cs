// Platypus Reclayed Android port: uGUI's bridge that forwards input to UI Toolkit panels, reduced to an empty
// component. The game has no UI Toolkit panels, and the original bridge is built on UI Toolkit internals that
// differ between Unity 2022 and Unity 6 (used to build without the Made with Unity splash).
using UnityEngine.EventSystems;

namespace UnityEngine.UIElements
{
    [AddComponentMenu("")]
    public class PanelEventHandler : UIBehaviour
    {
        public IPanel panel { get; set; }
    }
}
