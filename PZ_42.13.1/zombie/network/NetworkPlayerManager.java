// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import zombie.characters.IsoPlayer;
import zombie.core.utils.UpdateLimit;

public class NetworkPlayerManager {
    private static final UpdateLimit damageUpdateLimit = new UpdateLimit(2000L);
    private static final UpdateLimit statsUpdateLimit = new UpdateLimit(1000L);
    private static final UpdateLimit healthUpdateLimit = new UpdateLimit(500L);
    private static final NetworkPlayerManager instance = new NetworkPlayerManager();

    public static NetworkPlayerManager getInstance() {
        return instance;
    }

    public void update() {
        if (GameServer.server) {
            boolean damageUpdate = damageUpdateLimit.Check();
            boolean statsUpdate = statsUpdateLimit.Check();
            boolean healthUpdate = healthUpdateLimit.Check();

            for (IsoPlayer player : GameServer.IDToPlayerMap.values()) {
                if (damageUpdate) {
                    player.getNetworkCharacterAI().syncDamage();
                }

                if (statsUpdate) {
                    player.getNetworkCharacterAI().syncStats();
                    player.getNetworkCharacterAI().syncXp();
                }

                if (healthUpdate) {
                    player.getNetworkCharacterAI().syncHealth();
                }
            }
        }
    }
}
