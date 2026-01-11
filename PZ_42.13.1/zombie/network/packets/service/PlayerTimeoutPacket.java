// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 6)
public class PlayerTimeoutPacket implements INetworkPacket {
    @JSONField
    short onlineId = -1;

    @Override
    public void setData(Object... values) {
        this.onlineId = ((IsoPlayer)values[0]).getOnlineID();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort(this.onlineId);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.onlineId = b.getShort();
    }

    @Override
    public void processClient(UdpConnection connection) {
        GameClient.receivePlayerTimeout(this.onlineId);
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
    }
}
