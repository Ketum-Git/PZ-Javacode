// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.network.GameServer;

public class PacketValidator extends SuspiciousActivity {
    private static final long PLAYER_UPDATE_TIMEOUT = 30000L;
    private final UpdateLimit ulPlayerUpdateTimeout = new UpdateLimit(30000L);

    public PacketValidator(UdpConnection connection) {
        super(connection);
    }

    public boolean playerUpdateTimeoutCheck() {
        return this.ulPlayerUpdateTimeout.Check();
    }

    public void playerUpdateTimeoutReset() {
        this.ulPlayerUpdateTimeout.Reset(30000L);
    }

    @Override
    public void update() {
        if (!GameServer.fastForward && !GameServer.isDelayedDisconnect(this.connection)) {
            super.update();
            AntiCheat.update(this.connection);
            if (!this.connection.isFullyConnected()) {
                this.playerUpdateTimeoutReset();
            }
        } else {
            this.playerUpdateTimeoutReset();
        }
    }
}
