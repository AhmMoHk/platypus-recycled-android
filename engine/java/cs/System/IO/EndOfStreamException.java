package cs.System.IO;

public class EndOfStreamException extends IOException {
    public EndOfStreamException() { super(); }
    public EndOfStreamException(String message) { super(message); }
    public EndOfStreamException(String message, cs.System.Exception innerException) { super(message, innerException); }
}
