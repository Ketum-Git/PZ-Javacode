// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.PVPLogTool;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.PVPLogTool, handlingType = 3)
public class PVPEventsPacket implements INetworkPacket {
    private boolean clear;

    @Override
    public void setData(Object... values) {
        this.clear = (Boolean)values[0];
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putBoolean(this.clear);
        b.putInt(PVPLogTool.getEvents().size());

        for (PVPLogTool.PVPEvent event : PVPLogTool.getEvents()) {
            b.putUTF(event.timestamp);
            b.putUTF(event.wielder);
            b.putUTF(event.target);
            b.putFloat(event.x);
            b.putFloat(event.y);
            b.putFloat(event.z);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.clear = b.get() != 0;
        if (GameClient.client) {
            int size = b.getInt();

            for (int i = 0; i < size; i++) {
                String timestamp = GameWindow.ReadString(b);
                String wielder = GameWindow.ReadString(b);
                String target = GameWindow.ReadString(b);
                float x = b.getFloat();
                float y = b.getFloat();
                float z = b.getFloat();
                PVPLogTool.getEvents().get(i).reset(timestamp, wielder, target, x, y, z);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (connection.role.hasCapability(Capability.PVPLogTool)) {
            if (this.clear) {
                PVPLogTool.clearEvents();
            }

            INetworkPacket.send(connection, PacketTypes.PacketType.PVPEvents, false);
        }
    }
}
