package cs.System.IO;

public class InvalidDataException extends cs.System.SystemException {
    public InvalidDataException() { super(); }
    public InvalidDataException(String message) { super(message); }
    public InvalidDataException(String message, cs.System.Exception innerException) { super(message, innerException); }
}
