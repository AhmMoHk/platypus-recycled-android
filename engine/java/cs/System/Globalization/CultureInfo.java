package cs.System.Globalization;

public class CultureInfo implements cs.System.IFormatProvider {
    private final String name;
    private static final CultureInfo INVARIANT = new CultureInfo("");
    private static CultureInfo current = new CultureInfo("en-US");
    public CultureInfo(String n) { name = n; }
    public static CultureInfo get_CurrentCulture() { return current; }
    public static CultureInfo get_CurrentUICulture() { return current; }
    public static CultureInfo get_InvariantCulture() { return INVARIANT; }
    public static void set_CurrentCulture(CultureInfo c) { current = c; }
    public String get_Name() { return name; }
    private final NumberFormatInfo nfi = new NumberFormatInfo();
    public NumberFormatInfo get_NumberFormat() { return nfi; }
    public Object GetFormat(cs.System.Type t) { return nfi; }
    public String get_TwoLetterISOLanguageName() { return name.isEmpty() ? "iv" : name.substring(0, 2); }
    public String get_ThreeLetterISOLanguageName() { return name.isEmpty() ? "ivl" : "eng"; }
    @Override public String toString() { return name; }
}
