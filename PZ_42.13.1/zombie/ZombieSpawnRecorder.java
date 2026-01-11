// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.ArrayList;
import zombie.characters.IsoZombie;
import zombie.core.logger.ZLogger;

public final class ZombieSpawnRecorder {
    public static final ZombieSpawnRecorder instance = new ZombieSpawnRecorder();
    public ZLogger logger;
    private final StringBuilder stringBuilder = new StringBuilder();
    private static final boolean disableLog = true;

    public void init() {
    }

    public void quit() {
    }

    public void record(IsoZombie zombie, String reason) {
    }

    public void record(ArrayList<IsoZombie> zombies, String reason) {
    }
}
