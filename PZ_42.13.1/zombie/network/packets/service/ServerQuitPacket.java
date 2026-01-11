// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 0, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ServerQuitPacket implements INetworkPacket {
    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
    }

    @Override
    public void write(ByteBufferWriter b) {
    }

    @Override
    public void processClient(UdpConnection connection) {
        GameWindow.kickReason = "Server shut down safely. Players and map data saved.";
        GameWindow.serverDisconnected = true;
        ConnectionManager.log("receive-packet", "server-quit", GameClient.connection);
    }
}
