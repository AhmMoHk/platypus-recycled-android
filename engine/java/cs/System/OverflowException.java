package cs.System;

public class OverflowException extends SystemException {
    public OverflowException() { super(); }
    public OverflowException(String message) { super(message); }
    public OverflowException(String message, Exception innerException) { super(message, innerException); }
}
