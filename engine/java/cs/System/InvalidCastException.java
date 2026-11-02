package cs.System;

public class InvalidCastException extends SystemException {
    public InvalidCastException() { super(); }
    public InvalidCastException(String message) { super(message); }
    public InvalidCastException(String message, Exception innerException) { super(message, innerException); }
}
