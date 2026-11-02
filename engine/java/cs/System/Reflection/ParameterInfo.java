package cs.System.Reflection;

public class ParameterInfo {
    private final String name;
    private final cs.System.Type type;
    private final int pos;
    private final boolean byref;

    public ParameterInfo() { this("", cs.System.Type.OBJECT, 0); }
    public ParameterInfo(String name, cs.System.Type type, int pos) { this(name, type, pos, false); }
    public ParameterInfo(String name, cs.System.Type type, int pos, boolean byref) { this.name = name; this.type = type; this.pos = pos; this.byref = byref; }

    public String get_Name() { return name; }
    public cs.System.Type get_ParameterType() { return byref ? type.makeByRef() : type; }
    public int get_Position() { return pos; }
    public boolean get_IsOptional() { return false; }
    public boolean get_IsOut() { return false; }
    public boolean get_IsIn() { return false; }
    public boolean get_HasDefaultValue() { return false; }
    public Object get_DefaultValue() { return null; }
    public Object[] GetCustomAttributes(cs.System.Type t, boolean inherit) { return (Object[]) java.lang.reflect.Array.newInstance(t.cls, 0); }
    public boolean IsDefined(cs.System.Type t, boolean inherit) { return false; }
}
