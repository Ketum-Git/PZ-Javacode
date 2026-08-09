// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import zombie.core.math.PZMath;

public final class FastTrig360 {
    private static final float[] sinTable = new float[360];

    public static float sin(float rad) {
        float degf = PZMath.wrap(rad * (180.0F / (float)Math.PI), 0.0F, 360.0F);
        int deg = PZMath.fastfloor(degf);
        return sinTable[deg % 360];
    }

    public static float cos(float rad) {
        return sin(rad + (float) (Math.PI / 2));
    }

    public static float approximateAtan2(float y, float x) {
        if (x == 0.0F) {
            if (y > 0.0F) {
                return (float) (Math.PI / 2);
            } else {
                return y < 0.0F ? (float) (-Math.PI / 2) : 0.0F;
            }
        } else {
            float z = y / x;
            float atan;
            if (Math.abs(z) < 1.0F) {
                atan = z / (1.0F + 0.28086F * z * z);
                if (x < 0.0F) {
                    if (y < 0.0F) {
                        return atan - (float) Math.PI;
                    }

                    return atan + (float) Math.PI;
                }
            } else {
                atan = (float) (Math.PI / 2) - z / (z * z + 0.28086F);
                if (y < 0.0F) {
                    return atan - (float) Math.PI;
                }
            }

            return atan;
        }
    }

    public static void test() {
        double maxDeviation = 0.0;

        for (int i = 0; i < 360; i++) {
            double x = Math.cos(i * (float) (Math.PI / 180.0));
            double y = Math.sin(i * (float) (Math.PI / 180.0));
            double angleAccurate = Math.atan2(y, x) * 180.0F / (float)Math.PI;
            float angleApprox = approximateAtan2((float)y, (float)x) * (180.0F / (float)Math.PI);
            double deviation = Math.abs(angleAccurate - angleApprox);
            maxDeviation = Math.max(maxDeviation, deviation);
            System.out.printf("i=%d deviation=%.4f%n", i, deviation);
        }

        System.out.printf("maxDeviation=%.4f%n", maxDeviation);
    }

    static {
        for (int i = 0; i < sinTable.length; i++) {
            sinTable[i] = (float)Math.sin(i * (float) (Math.PI / 180.0));
        }
    }
}
