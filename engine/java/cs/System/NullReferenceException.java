package cs.System;

public class NullReferenceException extends SystemException {
    public NullReferenceException() { super(); }
    public NullReferenceException(String message) { super(message); }
    public NullReferenceException(String message, Exception innerException) { super(message, innerException); }
}
