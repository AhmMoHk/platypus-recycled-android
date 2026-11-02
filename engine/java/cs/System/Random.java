package cs.System;

/** System.Random: the classic .NET subtractive generator (same sequence as the original for a given seed). */
public class Random {
    private static final int MBIG = Integer.MAX_VALUE, MSEED = 161803398;
    private int inext, inextp;
    private final int[] seedArray = new int[56];

    public Random() { this((int) System.nanoTime()); }

    public Random(int seed) {
        int ii, mj, mk;
        int subtraction = (seed == Integer.MIN_VALUE) ? Integer.MAX_VALUE : java.lang.Math.abs(seed);
        mj = MSEED - subtraction;
        seedArray[55] = mj;
        mk = 1;
        for (int i = 1; i < 55; i++) {
            ii = (21 * i) % 55;
            seedArray[ii] = mk;
            mk = mj - mk;
            if (mk < 0) mk += MBIG;
            mj = seedArray[ii];
        }
        for (int k = 1; k < 5; k++) {
            for (int i = 1; i < 56; i++) {
                seedArray[i] -= seedArray[1 + (i + 30) % 55];
                if (seedArray[i] < 0) seedArray[i] += MBIG;
            }
        }
        inext = 0;
        inextp = 21;
    }

    protected double Sample() { return internalSample() * (1.0 / MBIG); }

    private int internalSample() {
        int locINext = inext, locINextp = inextp;
        if (++locINext >= 56) locINext = 1;
        if (++locINextp >= 56) locINextp = 1;
        int retVal = seedArray[locINext] - seedArray[locINextp];
        if (retVal == MBIG) retVal--;
        if (retVal < 0) retVal += MBIG;
        seedArray[locINext] = retVal;
        inext = locINext;
        inextp = locINextp;
        return retVal;
    }

    public int Next() { return internalSample(); }

    private double getSampleForLargeRange() {
        int result = internalSample();
        boolean negative = internalSample() % 2 == 0;
        if (negative) result = -result;
        double d = result;
        d += (Integer.MAX_VALUE - 1);
        d /= 2.0 * Integer.MAX_VALUE - 1;
        return d;
    }

    public int Next(int minValue, int maxValue) {
        if (minValue > maxValue) throw new ArgumentOutOfRangeException("minValue");
        long range = (long) maxValue - minValue;
        if (range <= Integer.MAX_VALUE) return (int) (Sample() * range) + minValue;
        return (int) ((long) (getSampleForLargeRange() * range) + minValue);
    }

    public int Next(int maxValue) {
        if (maxValue < 0) throw new ArgumentOutOfRangeException("maxValue");
        return (int) (Sample() * maxValue);
    }

    public double NextDouble() { return Sample(); }

    public void NextBytes(byte[] buffer) {
        for (int i = 0; i < buffer.length; i++) buffer[i] = (byte) (internalSample() % 256);
    }
}
