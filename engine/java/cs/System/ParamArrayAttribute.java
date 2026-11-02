package cs.System;

public class ParamArrayAttribute extends Attribute {
    private String message;
    public ParamArrayAttribute() {}
    public ParamArrayAttribute(String m) { message = m; }
    public ParamArrayAttribute(String m, boolean error) { message = m; }
    public String get_Message() { return message; }
}
