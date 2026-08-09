// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.random;

import java.util.concurrent.ThreadLocalRandom;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.network.GameServer;

public interface RandInterface {
    void init();

    int Next(int arg0);

    long Next(long arg0);

    int Next(int arg0, int arg1);

    long Next(long arg0, long arg1);

    float Next(float arg0, float arg1);

    default boolean NextBool(int invProbability) {
        return this.Next(invProbability) == 0;
    }

    default int AdjustForFramerate(int chance) {
        if (GameServer.server) {
            chance = (int)(chance * 0.33333334F);
        } else {
            chance = (int)(chance * (PerformanceSettings.getLockFPS() / 30.0F));
        }

        return chance;
    }

    default boolean NextBool(float chance) {
        float chanceClamped = PZMath.clamp(chance, 0.0F, 1.0F);
        return ThreadLocalRandom.current().nextFloat() < chanceClamped;
    }
}
