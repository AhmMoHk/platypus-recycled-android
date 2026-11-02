package cs.System;

public class ArgumentException extends SystemException {
    public ArgumentException() { super(); }
    public ArgumentException(String message) { super(message); }
    public ArgumentException(String message, Exception innerException) { super(message, innerException); }
    public ArgumentException(String message, String paramName) { super(message + " (Parameter '" + paramName + "')"); }
}
