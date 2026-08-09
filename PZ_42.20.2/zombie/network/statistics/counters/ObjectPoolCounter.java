// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics.counters;

import java.util.concurrent.atomic.AtomicLong;
import zombie.network.statistics.data.PoolStatistic;

public abstract class ObjectPoolCounter {
    private static final AtomicLong id = new AtomicLong();

    public ObjectPoolCounter(String name) {
        new Counter(PoolStatistic.getInstance(), name + "-" + id.incrementAndGet(), 0.0, () -> this.size(), "Objects in the pool", "number");
    }

    public abstract int size();
}
