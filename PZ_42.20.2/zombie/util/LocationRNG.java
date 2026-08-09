// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

public final class LocationRNG {
    public static final LocationRNG instance = new LocationRNG();
    private static final float INT_TO_FLOAT = Float.intBitsToFloat(864026624);
    private long s0;
    private long s1;
    private long state;

    public void setSeed(long seed) {
        this.state = seed;
        this.s0 = this.nextSplitMix64();
        this.s1 = this.nextSplitMix64();
    }

    public long getSeed() {
        return this.state;
    }

    private long nextSplitMix64() {
        long z = this.state += -7046029254386353131L;
        z = (z ^ z >>> 30) * -4658895280553007687L;
        z = (z ^ z >>> 27) * -7723592293110705685L;
        return z ^ z >>> 31;
    }

    public float nextFloat() {
        return (this.nextInt() >>> 8) * INT_TO_FLOAT;
    }

    private int nextInt() {
        long s0 = this.s0;
        long s1 = this.s1;
        long result = s0 + s1;
        s1 ^= s0;
        this.s0 = Long.rotateLeft(s0, 55) ^ s1 ^ s1 << 14;
        this.s1 = Long.rotateLeft(s1, 36);
        return (int)(result & -1L);
    }

    public int nextInt(int n) {
        long r = this.nextInt() >>> 1;
        r = r * n >> 31;
        return (int)r;
    }

    public int nextInt(int n, int x, int y, int z) {
        this.setSeed((long)z << 16 | (long)y << 32 | x);
        return this.nextInt(n);
    }
}
