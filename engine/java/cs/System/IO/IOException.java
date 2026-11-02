package cs.System.IO;

public class IOException extends cs.System.SystemException {
    public IOException() { super(); }
    public IOException(String message) { super(message); }
    public IOException(String message, cs.System.Exception innerException) { super(message, innerException); }
}
