package cs.System.IO;

public class DirectoryInfo extends FileSystemInfo {
    public DirectoryInfo(String p) { path = p; }
    public DirectoryInfo get_Parent() { String d = Path.GetDirectoryName(path); return d == null ? null : new DirectoryInfo(d); }
    @Override public boolean get_Exists() { return Directory.Exists(path); }
    public DirectoryInfo[] GetDirectories() {
        String[] d = Directory.GetDirectories(path);
        DirectoryInfo[] r = new DirectoryInfo[d.length];
        for (int i = 0; i < d.length; i++) r[i] = new DirectoryInfo(d[i]);
        return r;
    }
    public FileInfo[] GetFiles() { return GetFiles("*"); }
    public FileInfo[] GetFiles(String pattern) {
        String[] d = Directory.GetFiles(path, pattern);
        FileInfo[] r = new FileInfo[d.length];
        for (int i = 0; i < d.length; i++) r[i] = new FileInfo(d[i]);
        return r;
    }
    public void Create() { Directory.CreateDirectory(path); }
}
