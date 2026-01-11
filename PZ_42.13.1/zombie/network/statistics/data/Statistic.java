// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics.data;

import io.prometheus.metrics.core.metrics.Gauge;
import java.util.HashMap;
import java.util.Map.Entry;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.network.statistics.counters.Counter;

public abstract class Statistic implements IStatistic {
    private final String name;
    public final HashMap<String, Counter> counters = new HashMap<>();
    public final HashMap<String, Double> statistics = new HashMap<>();
    protected final KahluaTable localTable = LuaManager.platform.newTable();
    protected final KahluaTable remoteTable = LuaManager.platform.newTable();
    public Gauge prometheus;
    private Counter counterForDelete;

    public Statistic(String name) {
        this.name = name;
        this.init();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void store(Counter counter) {
        this.counters.put(counter.getName(), counter);
    }

    @Override
    public void update() {
        this.statistics.clear();
        this.localTable.wipe();

        for (Counter counter : this.counters.values()) {
            try {
                double value = counter.get();
                this.statistics.put(counter.getName(), value);
                this.localTable.rawset(counter.getName(), value);
            } catch (Exception var6) {
                this.counterForDelete = counter;
                counter.clear();
                break;
            }

            counter.clear();
        }

        if (this.counterForDelete != null) {
            try {
                this.counters.remove(this.counterForDelete.getName());
                this.counterForDelete = null;
            } catch (Exception var5) {
            }
        }
    }

    public Counter getCounter(String counter) {
        return this.counters.get(counter);
    }

    public String getValue(String counter) {
        return this.statistics.containsKey(counter) ? String.valueOf(this.statistics.get(counter)) : "";
    }

    public String getList() {
        StringBuilder stringBuilder = new StringBuilder();

        for (Entry<String, Double> counter : this.statistics.entrySet()) {
            stringBuilder.append(counter.getKey()).append(", ");
        }

        return stringBuilder.toString();
    }

    public String getAll() {
        StringBuilder stringBuilder = new StringBuilder();

        for (Entry<String, Double> counter : this.statistics.entrySet()) {
            stringBuilder.append("\n").append(counter.getKey()).append(": ").append(counter.getValue());
        }

        return stringBuilder.toString();
    }

    public KahluaTableImpl getLocalTable() {
        return (KahluaTableImpl)this.localTable;
    }

    public KahluaTableImpl getRemoteTable() {
        return (KahluaTableImpl)this.remoteTable;
    }
}
