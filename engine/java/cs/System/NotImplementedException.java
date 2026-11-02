package cs.System;

public class NotImplementedException extends SystemException {
    public NotImplementedException() { super(); }
    public NotImplementedException(String message) { super(message); }
    public NotImplementedException(String message, Exception innerException) { super(message, innerException); }
}
