// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics.data;

public class PoolStatistic extends Statistic implements IStatistic {
    private static final PoolStatistic instance = new PoolStatistic("pool");

    private PoolStatistic(String application) {
        super(application);
    }

    public static PoolStatistic getInstance() {
        return instance;
    }
}
