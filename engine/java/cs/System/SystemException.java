package cs.System;

public class SystemException extends Exception {
    public SystemException() { super(); }
    public SystemException(String message) { super(message); }
    public SystemException(String message, Exception innerException) { super(message, innerException); }
}
