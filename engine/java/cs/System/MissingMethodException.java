package cs.System;

public class MissingMethodException extends SystemException {
    public MissingMethodException() { super(); }
    public MissingMethodException(String message) { super(message); }
    public MissingMethodException(String message, Exception innerException) { super(message, innerException); }
}
