// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.utils.UpdateLimit;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public class PacketValidator extends SuspiciousActivity implements AntiCheatRecipeUpdate.IAntiCheatUpdate, AntiCheatPlayerUpdate.IAntiCheatUpdate {
    private static final long PLAYER_UPDATE_TIMEOUT = 5000L;
    private static final long CHECKSUM_INTERVAL = 4000L;
    private static final long CHECKSUM_TIMEOUT = 10000L;
    private final UpdateLimit ulPlayerUpdateTimeout = new UpdateLimit(5000L);
    private final UpdateLimit ulChecksumInterval = new UpdateLimit(4000L);
    private final UpdateLimit ulChecksumTimeout = new UpdateLimit(10000L);
    private int salt;

    @Override
    public void checksumSend(boolean queued, boolean done) {
        this.salt = Rand.Next(Integer.MAX_VALUE);
        INetworkPacket.send(this.connection, PacketTypes.PacketType.Validate, this.salt, queued, done);
        this.ulChecksumInterval.Reset(4000L);
    }

    @Override
    public boolean checksumIntervalCheck() {
        return this.ulChecksumInterval.Check();
    }

    @Override
    public boolean checksumTimeoutCheck() {
        return this.ulChecksumTimeout.Check();
    }

    @Override
    public void checksumTimeoutReset() {
        this.ulChecksumTimeout.Reset(10000L);
    }

    @Override
    public boolean playerUpdateTimeoutCheck() {
        return this.ulPlayerUpdateTimeout.Check();
    }

    @Override
    public void playerUpdateTimeoutReset() {
        this.ulPlayerUpdateTimeout.Reset(5000L);
    }

    public PacketValidator(UdpConnection connection) {
        super(connection);
    }

    public void reset() {
        this.salt = Rand.Next(Integer.MAX_VALUE);
        this.resetTimers();
    }

    public int getSalt() {
        return this.salt;
    }

    public void resetTimers() {
        this.ulPlayerUpdateTimeout.Reset(5000L);
        this.ulChecksumInterval.Reset(4000L);
        this.ulChecksumTimeout.Reset(10000L);
    }

    @Override
    public void update() {
        if (!GameServer.fastForward && !GameServer.isDelayedDisconnect(this.connection)) {
            super.update();
            if (this.connection.isFullyConnected()) {
                AntiCheat.update(this.connection);
            } else {
                AntiCheat.preUpdate(this.connection);
                this.resetTimers();
            }
        } else {
            this.resetTimers();
        }
    }
}
