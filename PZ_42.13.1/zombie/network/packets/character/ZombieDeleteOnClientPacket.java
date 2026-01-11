// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.VirtualZombieManager;
import zombie.characters.Capability;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;
import zombie.popman.NetworkZombiePacker;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ZombieDeleteOnClientPacket implements INetworkPacket {
    @JSONField
    public final ArrayList<Short> zombiesDeleted = new ArrayList<>();

    @Override
    public void setData(Object... values) {
        this.zombiesDeleted.clear();

        for (NetworkZombiePacker.DeletedZombie dz : (ArrayList)values[1]) {
            if (((UdpConnection)values[0]).RelevantTo(dz.x, dz.y)) {
                this.zombiesDeleted.add(dz.onlineId);
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        short numDeletedZombies = b.getShort();

        for (short n = 0; n < numDeletedZombies; n++) {
            short zombieId = b.getShort();
            IsoZombie z = GameClient.IDToZombieMap.get(zombieId);
            if (z != null) {
                VirtualZombieManager.instance.removeZombieFromWorld(z);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort((short)this.zombiesDeleted.size());

        for (Short dz : this.zombiesDeleted) {
            b.putShort(dz);
        }
    }
}
