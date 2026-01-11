// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;
import zombie.popman.NetworkZombiePacker;

public class ZombieSimulationPacket implements INetworkPacket {
    public final ArrayDeque<IsoZombie> sendQueue = new ArrayDeque<>();

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        int zombieZombies = b.getShort();

        for (int i = 0; i < zombieZombies; i++) {
            NetworkZombiePacker.getInstance().parseZombie(b, connection);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort((short)this.sendQueue.size());

        while (!this.sendQueue.isEmpty()) {
            IsoZombie z = this.sendQueue.poll();
            z.zombiePacket.set(z);
            z.zombiePacket.write(b);
        }
    }
}
