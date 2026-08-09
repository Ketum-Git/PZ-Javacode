// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.util.List;
import zombie.SandboxOptions;
import zombie.core.Color;
import zombie.core.ImmutableColor;
import zombie.util.LocationRNG;

public final class OutfitRNG {
    private static final ThreadLocal<LocationRNG> RNG = ThreadLocal.withInitial(LocationRNG::new);

    public static void setSeed(long seed) {
        RNG.get().setSeed(seed);
    }

    public static long getSeed() {
        return RNG.get().getSeed();
    }

    public static int Next(int max) {
        return RNG.get().nextInt(max);
    }

    public static int Next(int min, int max) {
        if (max == min) {
            return min;
        } else {
            if (min > max) {
                int temp = min;
                min = max;
                max = temp;
            }

            int n = RNG.get().nextInt(max - min);
            return n + min;
        }
    }

    public static float Next(float min, float max) {
        if (max == min) {
            return min;
        } else {
            if (min > max) {
                float temp = min;
                min = max;
                max = temp;
            }

            return min + RNG.get().nextFloat() * (max - min);
        }
    }

    public static boolean NextBool(int invProbability) {
        return Next(invProbability) == 0;
    }

    public static <E> E pickRandom(List<E> collection) {
        if (collection.isEmpty()) {
            return null;
        } else if (collection.size() == 1) {
            return collection.get(0);
        } else {
            int randomIndex = Next(collection.size());
            return collection.get(randomIndex);
        }
    }

    public static ImmutableColor randomImmutableColor() {
        return randomImmutableColor(false);
    }

    public static ImmutableColor randomImmutableColor(boolean noBlack) {
        float colorHue = Next(0.0F, 1.0F);
        float colorSaturation = Next(0.0F, 0.6F);
        float minimumBrightness = 0.1F;
        if (SandboxOptions.instance.noBlackClothes.getValue() || noBlack) {
            minimumBrightness = 0.2F;
        }

        float colorBrightness = Next(minimumBrightness, 0.9F);
        Color newC = Color.HSBtoRGB(colorHue, colorSaturation, colorBrightness);
        return new ImmutableColor(newC);
    }
}
