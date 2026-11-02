package cs.System.Reflection;

public class CustomAttributeData {
    public final Object attribute;
    public CustomAttributeData(Object a) { attribute = a; }
    public CustomAttributeData() { attribute = null; }
    public cs.System.Type get_AttributeType() { return cs.System.Type.ofObject(attribute); }
}
