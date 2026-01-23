// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.SafehouseID;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class SafehouseChangeTitlePacket extends SafehouseID implements INetworkPacket {
    @JSONField
    public String title;

    @Override
    public void setData(Object... values) {
        this.set((SafeHouse)values[0]);
        this.title = (String)values[1];
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putUTF(this.title);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.title = GameWindow.ReadString(b);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        if (!super.isConsistent(connection)) {
            DebugLog.Multiplayer.error("safehouse is not found");
            return false;
        } else if (StringUtils.isNullOrEmpty(this.title)) {
            DebugLog.Multiplayer.error("title is not set");
            return false;
        } else if (!connection.role.hasCapability(Capability.CanSetupSafehouses) && !connection.havePlayer(this.getSafehouse().getOwner())) {
            DebugLog.Multiplayer.error("player is not owner");
            return false;
        } else if (!connection.havePlayer(this.getSafehouse().getOwner())) {
            DebugLog.Multiplayer.error("player renaming safehouse is not the owner");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.getSafehouse().setTitle(this.title);

        for (UdpConnection c : GameServer.udpEngine.connections) {
            if (c.isFullyConnected()) {
                INetworkPacket.send(c, PacketTypes.PacketType.SafehouseSync, this.getSafehouse(), false);
            }
        }
    }
}
