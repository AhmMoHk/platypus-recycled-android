package cs.System;

public class ObjectDisposedException extends InvalidOperationException {
    public ObjectDisposedException() { super(); }
    public ObjectDisposedException(String message) { super(message); }
    public ObjectDisposedException(String message, Exception innerException) { super(message, innerException); }
}
