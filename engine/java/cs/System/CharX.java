package cs.System;

public final class CharX {
    private CharX() {}
    public static String ToString(char c) { return String.valueOf(c); }
    public static String ToString(char c, IFormatProvider p) { return String.valueOf(c); }
    public static boolean IsDigit(char c) { return Character.isDigit(c); }
    public static boolean IsDigit(String s, int i) { return Character.isDigit(s.charAt(i)); }
    public static boolean IsLetter(char c) { return Character.isLetter(c); }
    public static boolean IsLetterOrDigit(char c) { return Character.isLetterOrDigit(c); }
    public static boolean IsLower(char c) { return Character.isLowerCase(c); }
    public static boolean IsUpper(char c) { return Character.isUpperCase(c); }
    public static boolean IsNumber(char c) {
        int t = Character.getType(c);
        return t == Character.DECIMAL_DIGIT_NUMBER || t == Character.LETTER_NUMBER || t == Character.OTHER_NUMBER;
    }
    public static boolean IsSeparator(char c) {
        int t = Character.getType(c);
        return t == Character.SPACE_SEPARATOR || t == Character.LINE_SEPARATOR || t == Character.PARAGRAPH_SEPARATOR;
    }
    public static boolean IsPunctuation(char c) {
        int t = Character.getType(c);
        return t == Character.CONNECTOR_PUNCTUATION || t == Character.DASH_PUNCTUATION || t == Character.START_PUNCTUATION
            || t == Character.END_PUNCTUATION || t == Character.INITIAL_QUOTE_PUNCTUATION || t == Character.FINAL_QUOTE_PUNCTUATION
            || t == Character.OTHER_PUNCTUATION;
    }
    public static boolean IsSymbol(char c) {
        int t = Character.getType(c);
        return t == Character.MATH_SYMBOL || t == Character.CURRENCY_SYMBOL || t == Character.MODIFIER_SYMBOL || t == Character.OTHER_SYMBOL;
    }
    public static boolean IsControl(char c) { return Character.isISOControl(c); }
    public static boolean IsWhiteSpace(char c) { return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\u0085'; }
    public static boolean IsWhiteSpace(String s, int i) { return IsWhiteSpace(s.charAt(i)); }
    public static boolean IsSurrogate(char c) { return Character.isSurrogate(c); }
    public static boolean IsHighSurrogate(char c) { return Character.isHighSurrogate(c); }
    public static boolean IsLowSurrogate(char c) { return Character.isLowSurrogate(c); }
    public static char ToLower(char c) { return Character.toLowerCase(c); }
    public static char ToUpper(char c) { return Character.toUpperCase(c); }
    public static char ToLowerInvariant(char c) { return Character.toLowerCase(c); }
    public static char ToUpperInvariant(char c) { return Character.toUpperCase(c); }
    public static String ConvertFromUtf32(int cp) { return new String(Character.toChars(cp)); }
    public static int ConvertToUtf32(char hi, char lo) { return Character.toCodePoint(hi, lo); }
    public static double GetNumericValue(char c) { int v = Character.getNumericValue(c); return v < 0 ? -1 : v; }
    public static char Parse(String s) { if (s.length() != 1) throw new FormatException("String must be exactly one character long."); return s.charAt(0); }
    public static boolean Equals(char a, char b) { return a == b; }
    public static boolean Equals(char a, Object b) { return b instanceof Character && (Character) b == a; }
    public static int GetHashCode(char c) { return c | (c << 16); }
    public static int CompareTo(char a, char b) { return Character.compare(a, b); }
}
