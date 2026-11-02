package cs.System;

public class PlatformNotSupportedException extends NotSupportedException {
    public PlatformNotSupportedException() { super(); }
    public PlatformNotSupportedException(String message) { super(message); }
    public PlatformNotSupportedException(String message, Exception innerException) { super(message, innerException); }
}
