package cs.System.IO;

public class FileInfo extends FileSystemInfo {
    public FileInfo(String p) { path = p; }
    public long get_Length() { return File.f(path).length(); }
    public String get_DirectoryName() { return Path.GetDirectoryName(path); }
    @Override public boolean get_Exists() { return File.Exists(path); }
}
