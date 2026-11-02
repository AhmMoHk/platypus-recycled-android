package cs.System;

public class FieldAccessException extends MemberAccessException {
    public FieldAccessException() { super(); }
    public FieldAccessException(String message) { super(message); }
    public FieldAccessException(String message, Exception innerException) { super(message, innerException); }
}
