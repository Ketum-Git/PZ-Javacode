// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 0, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class DeadPlayerPacket extends DeadCharacterPacket implements INetworkPacket {
    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.writeCharacterInventory(b);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.parseCharacterInventory(b);
    }

    @Override
    public boolean isPostponed() {
        return false;
    }
}
