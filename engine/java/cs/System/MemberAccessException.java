package cs.System;

public class MemberAccessException extends SystemException {
    public MemberAccessException() { super(); }
    public MemberAccessException(String message) { super(message); }
    public MemberAccessException(String message, Exception innerException) { super(message, innerException); }
}
