// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;
import zombie.popman.NetworkZombieSimulator;

@PacketSetting(ordering = 0, priority = 1, reliability = 0, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ZombieListPacket implements INetworkPacket {
    public final ArrayList<Short> zombiesAuth = new ArrayList<>();

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        NetworkZombieSimulator.getInstance().clear();
        short numAuths = b.getShort();

        for (short n = 0; n < numAuths; n++) {
            short zombieId = b.getShort();
            NetworkZombieSimulator.getInstance().add(zombieId);
        }

        NetworkZombieSimulator.getInstance().added();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort((short)this.zombiesAuth.size());

        for (Short az : this.zombiesAuth) {
            b.putShort(az);
        }
    }
}
