package cs.System;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

/** System.Decimal backed by BigDecimal (.NET conversion rules: double -> 15 significant digits, float -> 7). */
public class Decimal implements cs.rt.Struct {
    private static final MathContext MC = new MathContext(29, RoundingMode.HALF_EVEN);
    public BigDecimal v = BigDecimal.ZERO;
    public static Decimal Zero = new Decimal();
    public static Decimal One = of(BigDecimal.ONE);
    public static Decimal MinusOne = of(BigDecimal.ONE.negate());

    public Decimal() {}
    public Decimal(int i) { v = BigDecimal.valueOf(i); }
    public Decimal(long i) { v = BigDecimal.valueOf(i); }
    public Decimal(double d) { v = fromDouble(d, 15); }
    public Decimal(float f) { v = fromDouble(f, 7); }
    public Decimal(int lo, int mid, int hi, boolean neg, byte scale) {
        BigInteger m = BigInteger.valueOf(hi & 0xffffffffL).shiftLeft(64)
            .or(BigInteger.valueOf(mid & 0xffffffffL).shiftLeft(32)).or(BigInteger.valueOf(lo & 0xffffffffL));
        if (neg) m = m.negate();
        v = new BigDecimal(m, scale & 0xff);
    }

    static Decimal of(BigDecimal b) { Decimal d = new Decimal(); d.v = b; return d; }

    private static BigDecimal fromDouble(double d, int digits) {
        if (Double.isNaN(d) || Double.isInfinite(d)) throw new OverflowException("Value was either too large or too small for a Decimal.");
        if (d == 0) return BigDecimal.ZERO;
        BigDecimal b = new BigDecimal(d).round(new MathContext(digits, RoundingMode.HALF_EVEN));
        b = b.stripTrailingZeros();
        if (b.scale() < 0) b = b.setScale(0);
        if (b.scale() > 28) b = b.setScale(28, RoundingMode.HALF_EVEN);
        return b;
    }

    public static Decimal op_Explicit$Decimal(double d) { return of(fromDouble(d, 15)); }
    public static Decimal op_Explicit$Decimal(float d) { return of(fromDouble(d, 7)); }
    public static Decimal op_Implicit$Decimal(int d) { return of(BigDecimal.valueOf(d)); }
    public static Decimal op_Implicit$Decimal(long d) { return of(BigDecimal.valueOf(d)); }
    public static double op_Explicit$double_(Decimal d) { return d.v.doubleValue(); }
    public static float op_Explicit$float_(Decimal d) { return d.v.floatValue(); }
    public static long op_Explicit$long_(Decimal d) { return d.v.setScale(0, RoundingMode.DOWN).longValue(); }
    public static int op_Explicit$int_(Decimal d) { return d.v.setScale(0, RoundingMode.DOWN).intValue(); }

    public static Decimal op_Addition(Decimal a, Decimal b) { return of(a.v.add(b.v, MC)); }
    public static Decimal op_Subtraction(Decimal a, Decimal b) { return of(a.v.subtract(b.v, MC)); }
    public static Decimal op_Multiply(Decimal a, Decimal b) { return of(a.v.multiply(b.v, MC)); }
    public static Decimal op_Division(Decimal a, Decimal b) {
        if (b.v.signum() == 0) throw new DivideByZeroException();
        return of(a.v.divide(b.v, MC).stripTrailingZeros());
    }
    public static Decimal op_UnaryNegation(Decimal a) { return of(a.v.negate()); }
    public static boolean op_GreaterThan(Decimal a, Decimal b) { return a.v.compareTo(b.v) > 0; }
    public static boolean op_GreaterThanOrEqual(Decimal a, Decimal b) { return a.v.compareTo(b.v) >= 0; }
    public static boolean op_LessThan(Decimal a, Decimal b) { return a.v.compareTo(b.v) < 0; }
    public static boolean op_LessThanOrEqual(Decimal a, Decimal b) { return a.v.compareTo(b.v) <= 0; }
    public static boolean op_Equality(Decimal a, Decimal b) { return a.v.compareTo(b.v) == 0; }
    public static boolean op_Inequality(Decimal a, Decimal b) { return a.v.compareTo(b.v) != 0; }
    public static Decimal Round(Decimal d, int decimals) { return of(d.v.setScale(decimals, RoundingMode.HALF_EVEN)); }
    public static Decimal Truncate(Decimal d) { return of(d.v.setScale(0, RoundingMode.DOWN)); }
    public static Decimal Floor(Decimal d) { return of(d.v.setScale(0, RoundingMode.FLOOR)); }
    public static Decimal Parse(String s) { return of(new BigDecimal(s.trim())); }
    public static double ToDouble(Decimal d) { return d.v.doubleValue(); }

    public Decimal $copy() { return of(v); }
    public void $set(Object o) { v = ((Decimal) o).v; }
    public void $clear() { v = BigDecimal.ZERO; }
    public static Decimal[] $newArray(int n) { Decimal[] a = new Decimal[n]; for (int i = 0; i < n; i++) a[i] = new Decimal(); return a; }
    @Override public String toString() { return v.toPlainString(); }
    @Override public boolean equals(Object o) { return o instanceof Decimal && ((Decimal) o).v.compareTo(v) == 0; }
    @Override public int hashCode() { return v.stripTrailingZeros().hashCode(); }
}
