// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 0, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 7)
public class TimeSyncPacket implements INetworkPacket {
    @JSONField
    private long clientTime;
    @JSONField
    private long serverTime;

    @Override
    public void setData(Object... values) {
        if (GameClient.client) {
            this.clientTime = System.nanoTime();
        } else if (GameServer.server) {
            this.serverTime = System.nanoTime();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putLong(this.clientTime);
        b.putLong(this.serverTime);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.clientTime = b.getLong();
        this.serverTime = b.getLong();
    }

    @Override
    public void processClient(UdpConnection connection) {
        GameTime.syncServerTime(this.clientTime, this.serverTime, System.nanoTime());
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        INetworkPacket.send(connection, PacketTypes.PacketType.TimeSync);
    }
}
