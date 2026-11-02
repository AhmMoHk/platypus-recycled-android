package cs.System;

public class ArgumentOutOfRangeException extends ArgumentException {
    public ArgumentOutOfRangeException() { super(); }
    public ArgumentOutOfRangeException(String message) { super(message); }
    public ArgumentOutOfRangeException(String paramName, String message) { super(message, paramName); }
    public ArgumentOutOfRangeException(String message, Exception innerException) { super(message, innerException); }
}
