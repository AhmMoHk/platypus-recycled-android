package cs.System;

public class ArgumentNullException extends ArgumentException {
    public ArgumentNullException() { super(); }
    public ArgumentNullException(String message) { super(message); }
    public ArgumentNullException(String paramName, String message) { super(message, paramName); }
    public ArgumentNullException(String message, Exception innerException) { super(message, innerException); }
}
