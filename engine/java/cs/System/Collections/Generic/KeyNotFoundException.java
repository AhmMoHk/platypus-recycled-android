package cs.System.Collections.Generic;

public class KeyNotFoundException extends cs.System.SystemException {
    public KeyNotFoundException() { super(); }
    public KeyNotFoundException(String message) { super(message); }
    public KeyNotFoundException(String message, cs.System.Exception innerException) { super(message, innerException); }
}
