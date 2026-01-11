// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.SeeNetworkUsers, handlingType = 1)
public class RequestNetworkUsersPacket implements INetworkPacket {
    @Override
    public void write(ByteBufferWriter b) {
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (connection.role.hasCapability(Capability.SeeNetworkUsers)) {
            INetworkPacket.send(connection, PacketTypes.PacketType.NetworkUsers);
        }
    }
}
