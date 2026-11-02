package cs.System;

public class NotSupportedException extends SystemException {
    public NotSupportedException() { super(); }
    public NotSupportedException(String message) { super(message); }
    public NotSupportedException(String message, Exception innerException) { super(message, innerException); }
}
