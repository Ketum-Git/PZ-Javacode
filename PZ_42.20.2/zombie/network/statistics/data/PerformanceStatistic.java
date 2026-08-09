// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics.data;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import zombie.network.statistics.counters.Counter;
import zombie.network.statistics.counters.PoolCounter;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.lambda.IntSupplierFunction;

public class PerformanceStatistic extends Statistic implements IStatistic {
    private static final PerformanceStatistic instance = new PerformanceStatistic("performance");
    public final Counter memoryFree = new Counter(this, "memory-free", 0.0, () -> Runtime.getRuntime().freeMemory() / 1000000L, "Free memory", "bytes");
    public final Counter memoryTotal = new Counter(this, "memory-total", 0.0, () -> Runtime.getRuntime().totalMemory() / 1000000L, "Total memory", "bytes");
    public final Counter memoryUsed = new Counter(
        this, "memory-used", 0.0, () -> (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000L, "Used memory", "bytes"
    );
    public final Counter memoryMax = new Counter(this, "memory-max", 0.0, () -> Runtime.getRuntime().maxMemory() / 1000000L, "Maximum memory", "bytes");
    public final Counter minUpdatePeriod = new Counter(this, "min-update-period", 9.223372E18F, null, "Minimum update cycle duration", "ms");
    public final Counter maxUpdatePeriod = new Counter(this, "max-update-period", 0.0, null, "Maximum update cycle duration", "ms");
    public final Counter avgUpdatePeriod = new Counter(this, "avg-update-period", 0.0, null, "Average update cycle duration", "ms");
    public final Counter fps = new Counter(this, "fps", 0.0, null, "Current update cycle duration", "ms");
    public final Counter allPoolsTotal = new Counter(this, "pool-objs-total", 0.0, this::getAllPoolsTotal, "All Pool object alloc+free", "objects");
    public final Counter allPoolsInUse = new Counter(this, "pool-objs-used", 0.0, this::getAllPoolsInUse, "All Pool object used count", "objects");
    public final Counter allPoolsFree = new Counter(this, "pool-objs-free", 0.0, this::getAllPoolsFree, "All Pool object free count", "objects");
    public final Set<Pool<?>> pools = new HashSet<>();

    private PerformanceStatistic(String application) {
        super(application);
    }

    public static PerformanceStatistic getInstance() {
        return instance;
    }

    public void addUpdate(long period) {
        if (period > this.maxUpdatePeriod.get()) {
            this.maxUpdatePeriod.set(period);
        }

        if (period < this.minUpdatePeriod.get()) {
            this.minUpdatePeriod.set(period);
        }

        this.avgUpdatePeriod.set((long)((period - this.avgUpdatePeriod.get()) * 0.05F));
        this.fps.set(period);
    }

    public <PO extends IPooledObject> void onPooledObjectAlloc(Pool<PO> sender, PO newInstance, boolean isReused, Pool.PoolStacks threadLocalPoolStacks) {
        this.getPoolCounter(sender, newInstance).onPooledObjectAlloc(threadLocalPoolStacks, isReused);
    }

    public <PO extends IPooledObject> void onPooledObjectReleased(Pool<PO> sender, PO releasedInstance, Pool.PoolStacks threadLocalPoolStacks) {
        this.getPoolCounter(sender, releasedInstance).onPooledObjectReleased(threadLocalPoolStacks);
    }

    private <PO extends IPooledObject> PoolCounter getPoolCounter(Pool<PO> sender, PO newInstance) {
        PoolCounter foundCounter = sender.getMonitoringPoolCounter();
        if (foundCounter != null) {
            return foundCounter;
        } else {
            synchronized (this.pools) {
                foundCounter = sender.getMonitoringPoolCounter();
                if (foundCounter != null) {
                    return foundCounter;
                } else {
                    String poolName = sender.getClass().getSimpleName() + "<" + newInstance.getClass().getSimpleName() + ">_" + sender.getID();
                    foundCounter = new PoolCounter(this, sender, poolName);
                    sender.setMonitoringPoolCounter(foundCounter);
                    this.pools.add(sender);
                    return foundCounter;
                }
            }
        }
    }

    public void visitAllPools(Consumer<PoolCounter> visitor) {
        synchronized (this.pools) {
            for (Pool<?> pool : this.pools) {
                PoolCounter counter = pool.getMonitoringPoolCounter();
                visitor.accept(counter);
            }
        }
    }

    public int addUpAllPoolValues(IntSupplierFunction<PoolCounter> intGetterFunction) {
        int result = 0;
        synchronized (this.pools) {
            for (Pool<?> pool : this.pools) {
                PoolCounter counter = pool.getMonitoringPoolCounter();
                result += intGetterFunction.getInt(counter);
            }

            return result;
        }
    }

    private double getAllPoolsTotal() {
        return this.addUpAllPoolValues(PoolCounter::getTotalPoolCount);
    }

    private double getAllPoolsInUse() {
        return this.addUpAllPoolValues(PoolCounter::getUsedCount);
    }

    private double getAllPoolsFree() {
        return this.addUpAllPoolValues(PoolCounter::getFreeCount);
    }
}
