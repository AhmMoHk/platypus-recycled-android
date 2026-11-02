package cs.System;

public final class BitConverter {
    private BitConverter() {}
    public static final boolean IsLittleEndian = true;
    public static long DoubleToInt64Bits(double d) { return Double.doubleToRawLongBits(d); }
    public static double Int64BitsToDouble(long v) { return Double.longBitsToDouble(v); }
    public static int SingleToInt32Bits(float f) { return Float.floatToRawIntBits(f); }
    public static float Int32BitsToSingle(int v) { return Float.intBitsToFloat(v); }
    public static byte[] GetBytes(int v) { return new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)}; }
    public static byte[] GetBytes(float v) { return GetBytes(Float.floatToRawIntBits(v)); }
    public static int ToInt32(byte[] d, int i) { return (d[i] & 255) | (d[i + 1] & 255) << 8 | (d[i + 2] & 255) << 16 | (d[i + 3] & 255) << 24; }
    public static float ToSingle(byte[] d, int i) { return Float.intBitsToFloat(ToInt32(d, i)); }
}
