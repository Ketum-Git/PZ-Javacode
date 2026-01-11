// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.random;

import java.util.List;

public final class Rand {
    public static int Next(int max) {
        return RandStandard.INSTANCE.Next(max);
    }

    public static long Next(long max) {
        return RandStandard.INSTANCE.Next(max);
    }

    public static int Next(int min, int max) {
        return RandStandard.INSTANCE.Next(min, max);
    }

    public static long Next(long min, long max) {
        return RandStandard.INSTANCE.Next(min, max);
    }

    public static float Next(float min, float max) {
        return RandStandard.INSTANCE.Next(min, max);
    }

    public static boolean NextBool(int invProbability) {
        return RandStandard.INSTANCE.NextBool(invProbability);
    }

    public static boolean NextBool(float chance) {
        return RandStandard.INSTANCE.NextBool(chance);
    }

    public static int AdjustForFramerate(int chance) {
        return RandStandard.INSTANCE.AdjustForFramerate(chance);
    }

    public static int NextInclusive(int inclusiveMin, int inclusiveMax) {
        return Next(inclusiveMin, inclusiveMax + 1);
    }

    public static <T> T Next(List<T> list) {
        return list.get(Next(list.size()));
    }
}
