// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

public class BoxedStaticValues {
    static Double[] doubles = new Double[10000];
    static Double[] negdoubles = new Double[10000];
    static Double[] doublesh = new Double[10000];
    static Double[] negdoublesh = new Double[10000];

    public static Double toDouble(double d) {
        if (d > -10000.0 && d < 10000.0) {
            double abs = Math.abs(d);
            if ((int)abs == abs) {
                if (d < 0.0) {
                    return negdoubles[(int)(-d)];
                }

                return doubles[(int)d];
            }

            if ((int)abs == abs - 0.5) {
                if (d < 0.0) {
                    return negdoublesh[(int)(-d)];
                }

                return doublesh[(int)d];
            }
        }

        return d;
    }

    static {
        for (int x = 0; x < 10000; x++) {
            doubles[x] = (double)x;
            negdoubles[x] = -doubles[x];
            doublesh[x] = x + 0.5;
            negdoublesh[x] = -(doubles[x] + 0.5);
        }
    }
}
