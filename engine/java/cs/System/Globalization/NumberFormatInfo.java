package cs.System.Globalization;

public class NumberFormatInfo implements cs.System.IFormatProvider {
    public String get_NumberDecimalSeparator() { return "."; }
    public String get_NumberGroupSeparator() { return ","; }
    public String get_NegativeSign() { return "-"; }
    public String get_PositiveSign() { return "+"; }
    public static NumberFormatInfo get_InvariantInfo() { return new NumberFormatInfo(); }
    public static NumberFormatInfo get_CurrentInfo() { return new NumberFormatInfo(); }
    public Object GetFormat(cs.System.Type t) { return this; }
}
