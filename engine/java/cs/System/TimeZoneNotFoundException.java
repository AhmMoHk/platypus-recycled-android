package cs.System;

public class TimeZoneNotFoundException extends Exception {
    public TimeZoneNotFoundException() { super(); }
    public TimeZoneNotFoundException(String message) { super(message); }
    public TimeZoneNotFoundException(String message, Exception innerException) { super(message, innerException); }
}
