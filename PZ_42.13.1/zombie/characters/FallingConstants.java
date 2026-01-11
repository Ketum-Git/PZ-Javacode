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

    public static float getIsoImpactSpeedFromHeight(float in_isoWorldHeight) {
        float fallAcceleration = 5.0010414F;
        float impactTime = PZMath.sqrt(in_isoWorldHeight * 2.0F / 5.0010414F);
        return 5.0010414F * impactTime;
    }

    public static boolean isLethalFall(float in_isoFallSpeed) {
        return in_isoFallSpeed >= lethalFallThreshold;
    }

    public static boolean isSevereFall(float in_isoFallSpeed) {
        return in_isoFallSpeed >= severeFallThreshold && in_isoFallSpeed < lethalFallThreshold;
    }

    public static boolean isHardFall(float in_isoFallSpeed) {
        return in_isoFallSpeed >= hardFallThreshold && in_isoFallSpeed < severeFallThreshold;
    }

    public static boolean isMoreThanHardFall(float in_isoFallSpeed) {
        return in_isoFallSpeed >= severeFallThreshold;
    }

    public static boolean isLightFall(float in_isoFallSpeed) {
        return in_isoFallSpeed >= isFallingThreshold && in_isoFallSpeed < hardFallThreshold;
    }

    public static boolean isMoreThanLightFall(float in_isoFallSpeed) {
        return in_isoFallSpeed >= hardFallThreshold;
    }

    public static boolean isFall(float in_isoFallSpeed) {
        return in_isoFallSpeed >= isFallingThreshold;
    }

    public static boolean isDamagingFall(float in_isoFallSpeed) {
        return in_isoFallSpeed >= noDamageThreshold;
    }

    public static FallSeverity getFallSeverity(float in_isoFallSpeed) {
        if (!isFall(in_isoFallSpeed)) {
            return FallSeverity.None;
        } else if (isLightFall(in_isoFallSpeed)) {
            return FallSeverity.Light;
        } else if (isHardFall(in_isoFallSpeed)) {
            return FallSeverity.Hard;
        } else if (isSevereFall(in_isoFallSpeed)) {
            return FallSeverity.Severe;
        } else {
            return isLethalFall(in_isoFallSpeed) ? FallSeverity.Lethal : FallSeverity.None;
        }
    }
}
