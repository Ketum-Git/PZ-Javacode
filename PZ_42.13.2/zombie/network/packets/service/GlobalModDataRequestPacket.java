// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;
import zombie.world.moddata.GlobalModData;

@PacketSetting(ordering = 0, priority = 0, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class GlobalModDataRequestPacket implements INetworkPacket {
    String tag;

    public void set(String tag) {
        this.tag = tag;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.tag);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        GlobalModData.instance.receiveRequest(b, connection);
    }
}
