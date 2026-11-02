package cs.System;

public class SerializableAttribute extends Attribute {
    private String message;
    public SerializableAttribute() {}
    public SerializableAttribute(String m) { message = m; }
    public SerializableAttribute(String m, boolean error) { message = m; }
    public String get_Message() { return message; }
}
