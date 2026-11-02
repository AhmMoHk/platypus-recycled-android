package cs.System;

public class ObsoleteAttribute extends Attribute {
    private String message;
    public ObsoleteAttribute() {}
    public ObsoleteAttribute(String m) { message = m; }
    public ObsoleteAttribute(String m, boolean error) { message = m; }
    public String get_Message() { return message; }
}
