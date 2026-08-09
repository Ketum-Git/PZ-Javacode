// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.util.ArrayDeque;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameClient;
import zombie.network.IConnection;
import zombie.network.packets.INetworkPacket;
import zombie.popman.NetworkZombieSimulator;

public class ZombieSynchronizationPacket implements INetworkPacket {
    public boolean hasNeighborPlayer;
    public final ArrayDeque<IsoZombie> sendQueue = new ArrayDeque<>();

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.hasNeighborPlayer = b.getBoolean();
        if (this.hasNeighborPlayer) {
            GameClient.instance.sendZombieTimer.setUpdatePeriod(200L);
        } else {
            GameClient.instance.sendZombieTimer.setUpdatePeriod(4000L);
        }

        NetworkZombieSimulator.getInstance().receivePacket(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putBoolean(this.hasNeighborPlayer);
        b.putShort(this.sendQueue.size());

        while (!this.sendQueue.isEmpty()) {
            IsoZombie z = this.sendQueue.poll();
            z.zombiePacket.write(b);
        }
    }
}
