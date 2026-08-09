// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.core.math.PZMath;

public final class FallingConstants {
    public static final float IsoWorldToPhysicsZScale = 2.44949F;
    public static final float PhysicsToIsoWorldZScale = 0.40824825F;
    public static final float FallAcceleration = 9.8F;
    public static final float IsoFallAcceleration = 5.0010414F;
    public static final float isFallingThreshold = getIsoImpactSpeedFromHeight(0.35F);
    public static final float noDamageThreshold = getIsoImpactSpeedFromHeight(0.5F);
    public static final float hardFallThreshold = getIsoImpactSpeedFromHeight(1.5F);
    public static final float severeFallThreshold = getIsoImpactSpeedFromHeight(2.5F);
    public static final float lethalFallThreshold = getIsoImpactSpeedFromHeight(3.5F);
    public static final float zombieLethalFallThreshold = getIsoImpactSpeedFromHeight(20.0F);
    public static final float fallDamageMultiplier = 115.0F;
    public static final float fallDamageInjuryMultiplier = 55.0F;

    public static float getIsoImpactSpeedFromHeight(float fallHeight) {
        float impactTime = PZMath.sqrt(fallHeight * 2.0F / 5.0010414F);
        return 5.0010414F * impactTime;
    }

    public static boolean isLethalFall(float isoFallSpeed) {
        return isoFallSpeed >= lethalFallThreshold;
    }

    public static boolean isSevereFall(float isoFallSpeed) {
        return isoFallSpeed >= severeFallThreshold && isoFallSpeed < lethalFallThreshold;
    }

    public static boolean isHardFall(float isoFallSpeed) {
        return isoFallSpeed >= hardFallThreshold && isoFallSpeed < severeFallThreshold;
    }

    public static boolean isMoreThanHardFall(float isoFallSpeed) {
        return isoFallSpeed >= severeFallThreshold;
    }

    public static boolean isLightFall(float isoFallSpeed) {
        return isoFallSpeed >= isFallingThreshold && isoFallSpeed < hardFallThreshold;
    }

    public static boolean isMoreThanLightFall(float isoFallSpeed) {
        return isoFallSpeed >= hardFallThreshold;
    }

    public static boolean isFall(float isoFallSpeed) {
        return isoFallSpeed >= isFallingThreshold;
    }

    public static boolean isDamagingFall(float isoFallSpeed) {
        return isoFallSpeed >= noDamageThreshold;
    }

    public static FallSeverity getFallSeverity(float isoFallSpeed) {
        if (!isFall(isoFallSpeed)) {
            return FallSeverity.None;
        } else if (isLightFall(isoFallSpeed)) {
            return FallSeverity.Light;
        } else if (isHardFall(isoFallSpeed)) {
            return FallSeverity.Hard;
        } else if (isSevereFall(isoFallSpeed)) {
            return FallSeverity.Severe;
        } else {
            return isLethalFall(isoFallSpeed) ? FallSeverity.Lethal : FallSeverity.None;
        }
    }

    public static boolean isFallingHeight(float fallHeight) {
        return getIsoImpactSpeedFromHeight(fallHeight) >= isFallingThreshold;
    }
}
