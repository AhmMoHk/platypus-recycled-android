package cs.System;

public class NonSerializedAttribute extends Attribute {
    private String message;
    public NonSerializedAttribute() {}
    public NonSerializedAttribute(String m) { message = m; }
    public NonSerializedAttribute(String m, boolean error) { message = m; }
    public String get_Message() { return message; }
}
