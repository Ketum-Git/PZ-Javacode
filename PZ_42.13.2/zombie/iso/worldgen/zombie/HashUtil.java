// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.zombie;

public class HashUtil {
    public static final int X_PRIME = 1619;
    public static final int Y_PRIME = 31337;
    public static final int Z_PRIME = 6971;
    public static final int W_PRIME = 1013;

    private HashUtil() {
    }

    public static int hash2D(long seed, long x, long y) {
        long hash = seed ^ 1619L * x;
        hash ^= 31337L * y;
        return (int)finalizeHash(hash);
    }

    private static long finalizeHash(long hash) {
        hash = hash * hash * hash * 60493L;
        return hash >> 13 ^ hash;
    }
}
