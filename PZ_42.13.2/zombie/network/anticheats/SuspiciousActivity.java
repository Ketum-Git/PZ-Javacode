// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import java.util.EnumMap;
import java.util.Map.Entry;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;

public class SuspiciousActivity {
    private final EnumMap<AntiCheat, Integer> counters = new EnumMap<>(AntiCheat.class);
    private final UpdateLimit ulDecreaseInterval = new UpdateLimit(150000L);
    protected final UdpConnection connection;

    public SuspiciousActivity(UdpConnection connection) {
        this.connection = connection;
    }

    public void update() {
        boolean doDecrease = this.ulDecreaseInterval.Check();

        for (Entry<AntiCheat, Integer> c : this.counters.entrySet()) {
            if (doDecrease) {
                if (c.getValue() > 0) {
                    c.setValue(c.getValue() - 1);
                    AntiCheat.log(this.connection, c.getKey(), c.getValue(), null);
                } else {
                    c.setValue(0);
                }
            }
        }
    }

    public int report(AntiCheat antiCheat) {
        if (AntiCheat.None == antiCheat) {
            return 0;
        } else {
            int counter = this.counters.getOrDefault(antiCheat, 0) + 1;
            this.counters.put(antiCheat, counter);
            return counter;
        }
    }

    public EnumMap<AntiCheat, Integer> getCounters() {
        return this.counters;
    }

    public void resetCounters() {
        for (Entry<AntiCheat, Integer> entry : this.counters.entrySet()) {
            entry.setValue(0);
        }
    }
}
