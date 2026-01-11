// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.TeleportPlayerToAnotherPlayer, handlingType = 1)
public class TeleportUserActionPacket implements INetworkPacket {
    @JSONField
    public TeleportUserActionPacket.Command action;
    @JSONField
    String username;
    @JSONField
    String argument;

    @Override
    public void setData(Object... values) {
        this.action = TeleportUserActionPacket.Command.valueOf((String)values[0]);
        this.username = (String)values[1];
        this.argument = (String)values[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte((byte)this.action.ordinal());
        GameWindow.WriteStringUTF(b.bb, this.username);
        GameWindow.WriteStringUTF(b.bb, this.argument);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.action = TeleportUserActionPacket.Command.values()[b.get()];
        this.username = GameWindow.ReadString(b);
        this.argument = GameWindow.ReadString(b);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoPlayer pl = GameServer.getPlayerByUserName(this.username);
        IsoPlayer admin = connection.players[0];
        UdpConnection c = GameServer.getConnectionFromPlayer(pl);
        switch (this.action) {
            case Teleport:
                GameServer.sendTeleport(pl, admin.getX(), admin.getY(), admin.getZ());
            default:
                if (connection.role.hasCapability(Capability.SeeNetworkUsers)) {
                    INetworkPacket.send(connection, PacketTypes.PacketType.NetworkUsers);
                }
        }
    }

    public static enum Command {
        Teleport;
    }
}
