package cs.System;

public class FlagsAttribute extends Attribute {
    private String message;
    public FlagsAttribute() {}
    public FlagsAttribute(String m) { message = m; }
    public FlagsAttribute(String m, boolean error) { message = m; }
    public String get_Message() { return message; }
}
