package cs.System;

public class ArithmeticException extends SystemException {
    public ArithmeticException() { super(); }
    public ArithmeticException(String message) { super(message); }
    public ArithmeticException(String message, Exception innerException) { super(message, innerException); }
}
