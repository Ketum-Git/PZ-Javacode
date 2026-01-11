// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.statistics.data;

import zombie.network.statistics.counters.Counter;

public class ConnectionQueueStatistic extends Statistic implements IStatistic {
    private static final ConnectionQueueStatistic instance = new ConnectionQueueStatistic("connection");
    public final Counter zombiesKilledByFireToday = new Counter(this, "zombies-killed-by-fire-today", 0.0, null, "Zombies killed by fire today", "number");
    public final Counter zombiesKilledToday = new Counter(this, "zombies-killed-today", 0.0, null, "Zombies killed today", "number");
    public final Counter zombifiedPlayersToday = new Counter(this, "zombified-players-today", 0.0, null, "Zombified players today", "number");
    public final Counter playersKilledByFireToday = new Counter(this, "players-killed-by-fire-today", 0.0, null, "Players killed by fire today", "number");
    public final Counter playersKilledByZombieToday = new Counter(this, "players-killed-by-zombie-today", 0.0, null, "Players killed by zombie today", "number");
    public final Counter playersKilledByPlayerToday = new Counter(this, "players-killed-by-player-today", 0.0, null, "Players killed by player today", "number");
    public final Counter burnedCorpsesToday = new Counter(this, "burned-corpses-today", 0.0, null, "", "number");

    public ConnectionQueueStatistic(String application) {
        super(application);
    }

    public static ConnectionQueueStatistic getInstance() {
        return instance;
    }
}
