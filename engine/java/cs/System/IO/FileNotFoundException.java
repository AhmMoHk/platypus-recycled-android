package cs.System.IO;

public class FileNotFoundException extends IOException {
    public FileNotFoundException() { super(); }
    public FileNotFoundException(String message) { super(message); }
    public FileNotFoundException(String message, cs.System.Exception innerException) { super(message, innerException); }
}
