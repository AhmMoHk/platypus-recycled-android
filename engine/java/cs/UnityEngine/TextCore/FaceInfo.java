package cs.UnityEngine.TextCore;

public class FaceInfo implements cs.rt.Struct {
    public static final Object[] $fmeta = {"m_FaceIndex", "m_FaceIndex", "i4", 8, "m_FamilyName", "m_FamilyName", "string", 8, "m_StyleName", "m_StyleName", "string", 8, "m_PointSize", "m_PointSize", "i4", 8, "m_Scale", "m_Scale", "r4", 8, "m_UnitsPerEM", "m_UnitsPerEM", "i4", 8, "m_LineHeight", "m_LineHeight", "r4", 8, "m_AscentLine", "m_AscentLine", "r4", 8, "m_CapLine", "m_CapLine", "r4", 8, "m_MeanLine", "m_MeanLine", "r4", 8, "m_Baseline", "m_Baseline", "r4", 8, "m_DescentLine", "m_DescentLine", "r4", 8, "m_SuperscriptOffset", "m_SuperscriptOffset", "r4", 8, "m_SuperscriptSize", "m_SuperscriptSize", "r4", 8, "m_SubscriptOffset", "m_SubscriptOffset", "r4", 8, "m_SubscriptSize", "m_SubscriptSize", "r4", 8, "m_UnderlineOffset", "m_UnderlineOffset", "r4", 8, "m_UnderlineThickness", "m_UnderlineThickness", "r4", 8, "m_StrikethroughOffset", "m_StrikethroughOffset", "r4", 8, "m_StrikethroughThickness", "m_StrikethroughThickness", "r4", 8, "m_TabWidth", "m_TabWidth", "r4", 8};
    public int m_FaceIndex;
    public String m_FamilyName = "";
    public String m_StyleName = "";
    public int m_PointSize;
    public float m_Scale = 1;
    public int m_UnitsPerEM;
    public float m_LineHeight;
    public float m_AscentLine;
    public float m_CapLine;
    public float m_MeanLine;
    public float m_Baseline;
    public float m_DescentLine;
    public float m_SuperscriptOffset;
    public float m_SuperscriptSize;
    public float m_SubscriptOffset;
    public float m_SubscriptSize;
    public float m_UnderlineOffset;
    public float m_UnderlineThickness;
    public float m_StrikethroughOffset;
    public float m_StrikethroughThickness;
    public float m_TabWidth;
    public FaceInfo() {}

    public int get_faceIndex() { return m_FaceIndex; }
    public void set_faceIndex(int v) { m_FaceIndex = v; }
    public String get_familyName() { return m_FamilyName; }
    public void set_familyName(String v) { m_FamilyName = v; }
    public String get_styleName() { return m_StyleName; }
    public void set_styleName(String v) { m_StyleName = v; }
    public int get_pointSize() { return m_PointSize; }
    public void set_pointSize(int v) { m_PointSize = v; }
    public float get_scale() { return m_Scale; }
    public void set_scale(float v) { m_Scale = v; }
    public int get_unitsPerEM() { return m_UnitsPerEM; }
    public void set_unitsPerEM(int v) { m_UnitsPerEM = v; }
    public float get_lineHeight() { return m_LineHeight; }
    public void set_lineHeight(float v) { m_LineHeight = v; }
    public float get_ascentLine() { return m_AscentLine; }
    public void set_ascentLine(float v) { m_AscentLine = v; }
    public float get_capLine() { return m_CapLine; }
    public void set_capLine(float v) { m_CapLine = v; }
    public float get_meanLine() { return m_MeanLine; }
    public void set_meanLine(float v) { m_MeanLine = v; }
    public float get_baseline() { return m_Baseline; }
    public void set_baseline(float v) { m_Baseline = v; }
    public float get_descentLine() { return m_DescentLine; }
    public void set_descentLine(float v) { m_DescentLine = v; }
    public float get_superscriptOffset() { return m_SuperscriptOffset; }
    public void set_superscriptOffset(float v) { m_SuperscriptOffset = v; }
    public float get_superscriptSize() { return m_SuperscriptSize; }
    public void set_superscriptSize(float v) { m_SuperscriptSize = v; }
    public float get_subscriptOffset() { return m_SubscriptOffset; }
    public void set_subscriptOffset(float v) { m_SubscriptOffset = v; }
    public float get_subscriptSize() { return m_SubscriptSize; }
    public void set_subscriptSize(float v) { m_SubscriptSize = v; }
    public float get_underlineOffset() { return m_UnderlineOffset; }
    public void set_underlineOffset(float v) { m_UnderlineOffset = v; }
    public float get_underlineThickness() { return m_UnderlineThickness; }
    public void set_underlineThickness(float v) { m_UnderlineThickness = v; }
    public float get_strikethroughOffset() { return m_StrikethroughOffset; }
    public void set_strikethroughOffset(float v) { m_StrikethroughOffset = v; }
    public float get_strikethroughThickness() { return m_StrikethroughThickness; }
    public void set_strikethroughThickness(float v) { m_StrikethroughThickness = v; }
    public float get_tabWidth() { return m_TabWidth; }
    public void set_tabWidth(float v) { m_TabWidth = v; }

    public FaceInfo $copy() { FaceInfo c = new FaceInfo(); c.$set(this); return c; }
    public void $set(Object o) { FaceInfo s = (FaceInfo) o; m_FaceIndex = s.m_FaceIndex; m_FamilyName = s.m_FamilyName; m_StyleName = s.m_StyleName; m_PointSize = s.m_PointSize; m_Scale = s.m_Scale; m_UnitsPerEM = s.m_UnitsPerEM; m_LineHeight = s.m_LineHeight; m_AscentLine = s.m_AscentLine; m_CapLine = s.m_CapLine; m_MeanLine = s.m_MeanLine; m_Baseline = s.m_Baseline; m_DescentLine = s.m_DescentLine; m_SuperscriptOffset = s.m_SuperscriptOffset; m_SuperscriptSize = s.m_SuperscriptSize; m_SubscriptOffset = s.m_SubscriptOffset; m_SubscriptSize = s.m_SubscriptSize; m_UnderlineOffset = s.m_UnderlineOffset; m_UnderlineThickness = s.m_UnderlineThickness; m_StrikethroughOffset = s.m_StrikethroughOffset; m_StrikethroughThickness = s.m_StrikethroughThickness; m_TabWidth = s.m_TabWidth; }
    public void $clear() { $set(new FaceInfo()); }
    public static FaceInfo[] $newArray(int n) { FaceInfo[] a = new FaceInfo[n]; for (int i = 0; i < n; i++) a[i] = new FaceInfo(); return a; }
}
