// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoObject;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class SledgehammerDestroyPacket implements INetworkPacket {
    RemoveItemFromSquarePacket packet = new RemoveItemFromSquarePacket();

    public void set(IsoObject obj) {
        this.packet.set(obj);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.packet.write(b);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.packet.parse(b, connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (ServerOptions.instance.allowDestructionBySledgehammer.getValue()) {
            this.packet.processServer(packetType, connection);

            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (c.getConnectedGUID() != connection.getConnectedGUID()) {
                    ByteBufferWriter b2 = c.startPacket();
                    PacketTypes.PacketType.RemoveItemFromSquare.doPacket(b2);
                    this.packet.write(b2);
                    PacketTypes.PacketType.RemoveItemFromSquare.send(c);
                }
            }
        }
    }
}
