package cs.UnityEngine;

public class Behaviour extends Component {
    public boolean m_Enabled = true;

    public boolean get_enabled() { return m_Enabled; }
    public void set_enabled(boolean v) { Eng.setEnabled(this, v); }
    public boolean get_isActiveAndEnabled() { return m_Enabled && gameObject != null && gameObject.get_activeInHierarchy(); }

    /** engine callbacks for built-in behaviours */
    public void $nativeEnable() {}
    public void $nativeDisable() {}
}
