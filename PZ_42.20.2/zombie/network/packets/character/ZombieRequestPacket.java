// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;
import zombie.popman.NetworkZombiePacker;
import zombie.popman.NetworkZombieSimulator;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class ZombieRequestPacket implements INetworkPacket {
    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        int zombieRequests = b.getShort();

        for (int i = 0; i < zombieRequests; i++) {
            int zombieID = b.getShort();
            IsoZombie z = ServerMap.instance.zombieMap.get((short)zombieID);
            if (z != null) {
                NetworkZombiePacker.getInstance().zombiesRequest.getNetworkZombie(connection).zombies.add(z);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        int unknownZombiesCount = NetworkZombieSimulator.getInstance().unknownZombies.size();
        b.putShort(unknownZombiesCount);

        for (int k = 0; k < unknownZombiesCount; k++) {
            b.putShort(NetworkZombieSimulator.getInstance().unknownZombies.get(k));
        }

        NetworkZombieSimulator.getInstance().unknownZombies.clear();
    }
}
