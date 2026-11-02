package cs.UnityEngine;

public class Resolution implements cs.rt.Struct {
    public int m_Width, m_Height, m_RefreshRate = 60;
    public Resolution() {}
    static Resolution of(int w, int h) { Resolution r = new Resolution(); r.m_Width = w; r.m_Height = h; return r; }
    public int get_width() { return m_Width; }
    public void set_width(int v) { m_Width = v; }
    public int get_height() { return m_Height; }
    public void set_height(int v) { m_Height = v; }
    public int get_refreshRate() { return m_RefreshRate; }
    public void set_refreshRate(int v) { m_RefreshRate = v; }
    @Override public String toString() { return m_Width + " x " + m_Height + " @ " + m_RefreshRate + "Hz"; }
    public Resolution $copy() { Resolution r = of(m_Width, m_Height); r.m_RefreshRate = m_RefreshRate; return r; }
    public void $set(Object o) { Resolution r = (Resolution) o; m_Width = r.m_Width; m_Height = r.m_Height; m_RefreshRate = r.m_RefreshRate; }
    public void $clear() { m_Width = m_Height = m_RefreshRate = 0; }
    public static Resolution[] $newArray(int n) { Resolution[] a = new Resolution[n]; for (int i = 0; i < n; i++) a[i] = new Resolution(); return a; }
}
