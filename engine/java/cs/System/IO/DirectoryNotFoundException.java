package cs.System.IO;

public class DirectoryNotFoundException extends IOException {
    public DirectoryNotFoundException() { super(); }
    public DirectoryNotFoundException(String message) { super(message); }
    public DirectoryNotFoundException(String message, cs.System.Exception innerException) { super(message, innerException); }
}
