package cs.System.IO;

public class FileSystemInfo {
    protected String path;
    public FileSystemInfo() {}
    public String get_Name() { return Path.GetFileName(path.endsWith("/") ? path.substring(0, path.length() - 1) : path); }
    public String get_FullName() { return Path.GetFullPath(path); }
    public boolean get_Exists() { return File.f(path).exists(); }
    public String get_Extension() { return Path.GetExtension(path); }
}
