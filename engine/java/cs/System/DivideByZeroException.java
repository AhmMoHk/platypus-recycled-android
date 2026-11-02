package cs.System;

public class DivideByZeroException extends SystemException {
    public DivideByZeroException() { super(); }
    public DivideByZeroException(String message) { super(message); }
    public DivideByZeroException(String message, Exception innerException) { super(message, innerException); }
}
