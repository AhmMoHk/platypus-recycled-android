package cs.System;

public class AttributeUsageAttribute {
    final int targets;
    boolean allowMultiple, inherited = true;
    public AttributeUsageAttribute(int t) { targets = t; }
    public int get_ValidOn() { return targets; }
    public boolean get_AllowMultiple() { return allowMultiple; }
    public void set_AllowMultiple(boolean v) { allowMultiple = v; }
    public boolean get_Inherited() { return inherited; }
    public void set_Inherited(boolean v) { inherited = v; }
}
