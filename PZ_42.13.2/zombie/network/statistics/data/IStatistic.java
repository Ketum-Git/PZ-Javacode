// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics.data;

import zombie.network.statistics.counters.Counter;

public interface IStatistic {
    String getName();

    void store(Counter var1);

    default void init() {
    }

    void update();
}
