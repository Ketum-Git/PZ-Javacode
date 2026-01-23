// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.globalObjects.CGlobalObjectNetwork;
import zombie.globalObjects.SGlobalObjectNetwork;
import zombie.network.GameServer;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class GlobalObjectsPacket implements INetworkPacket {
    ByteBuffer buffer;

    public void set(ByteBuffer bb) {
        this.buffer = bb;
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.buffer.flip();
        b.bb.put(this.buffer);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        if (GameServer.server) {
            int playerIndex = b.get();
            IsoPlayer player = GameServer.getPlayerFromConnection(connection, playerIndex);
            if (playerIndex == -1) {
                player = GameServer.getAnyPlayerFromConnection(connection);
            }

            if (player == null) {
                DebugLog.log("receiveGlobalObjects: player is null");
                return;
            }

            SGlobalObjectNetwork.receive(b, player);
        } else {
            try {
                CGlobalObjectNetwork.receive(b);
            } catch (IOException var5) {
                throw new RuntimeException(var5);
            }
        }
    }
}
