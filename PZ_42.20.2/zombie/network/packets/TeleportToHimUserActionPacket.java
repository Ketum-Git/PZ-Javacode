// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.TeleportToPlayer, handlingType = 1)
public class TeleportToHimUserActionPacket implements INetworkPacket {
    @JSONField
    public TeleportToHimUserActionPacket.Command action;
    @JSONField
    String username;
    @JSONField
    String argument;

    @Override
    public void setData(Object... values) {
        this.action = TeleportToHimUserActionPacket.Command.valueOf((String)values[0]);
        this.username = (String)values[1];
        this.argument = (String)values[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putEnum(this.action);
        b.putUTF(this.username);
        b.putUTF(this.argument);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.action = b.getEnum(TeleportToHimUserActionPacket.Command.class);
        this.username = b.getUTF();
        this.argument = b.getUTF();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoPlayer pl = GameServer.getPlayerByUserName(this.username);
        IsoPlayer admin = connection.players[0];
        UdpConnection c = GameServer.getConnectionFromPlayer(pl);
        switch (this.action) {
            case TeleportToHim:
                GameServer.sendTeleport(admin, pl.getX(), pl.getY(), pl.getZ());
            default:
                if (connection.getRole().hasCapability(Capability.SeeNetworkUsers)) {
                    INetworkPacket.send(connection, PacketTypes.PacketType.NetworkUsers);
                }
        }
    }

    public static enum Command {
        TeleportToHim;
    }
}
