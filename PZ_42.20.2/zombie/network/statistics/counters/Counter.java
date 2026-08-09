// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics.counters;

import com.google.common.util.concurrent.AtomicDouble;
import zombie.network.statistics.data.Statistic;

public class Counter implements ICounter {
    protected final String name;
    protected final String tooltip;
    protected final String units;
    protected final double defaultValue;
    protected final ICounter lambda;
    protected final AtomicDouble value;
    protected final boolean isPerishable;

    public Counter(Statistic statistic, String name, double value, ICounter lambda, String tooltip, String units, boolean isPerishable) {
        this.name = name;
        this.tooltip = tooltip;
        this.units = units;
        this.value = new AtomicDouble(value);
        this.defaultValue = value;
        this.lambda = lambda;
        this.isPerishable = isPerishable;
        statistic.store(this);
    }

    public Counter(Statistic statistic, String name, double value, ICounter lambda, String tooltip, String units) {
        this(statistic, name, value, lambda, tooltip, units, true);
    }

    public String getName() {
        return this.name;
    }

    public String getTooltip() {
        return this.tooltip;
    }

    public String getUnits() {
        return this.units;
    }

    public Boolean isPerishable() {
        return this.isPerishable;
    }

    public void clear() {
        this.value.set(this.defaultValue);
    }

    public void increase() {
        this.value.addAndGet(1.0);
    }

    public void increase(double value) {
        this.value.addAndGet(value);
    }

    @Override
    public double get() {
        return this.lambda == null ? this.value.get() : this.lambda.get();
    }

    public void set(double value) {
        this.value.set(value);
    }
}
