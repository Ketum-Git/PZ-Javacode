// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.BanSystem;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.BanUnbanUser, handlingType = 1)
public class BanUnbanUserActionPacket implements INetworkPacket {
    @JSONField
    public BanUnbanUserActionPacket.Command action;
    @JSONField
    String username;
    @JSONField
    String argument;

    @Override
    public void setData(Object... values) {
        this.action = BanUnbanUserActionPacket.Command.valueOf((String)values[0]);
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
        this.action = BanUnbanUserActionPacket.Command.values()[b.get()];
        this.username = GameWindow.ReadString(b);
        this.argument = GameWindow.ReadString(b);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        try {
            IsoPlayer pl = GameServer.getPlayerByUserName(this.username);
            IsoPlayer admin = connection.players[0];
            UdpConnection c = GameServer.getConnectionFromPlayer(pl);
            switch (this.action) {
                case Kick:
                    if (admin.getRole().hasCapability(Capability.KickUser)) {
                        LoggerManager.getLogger("admin").write(connection.username + " kicked user " + this.username);
                        ServerWorldDatabase.instance.addUserlog(this.username, Userlog.UserlogType.Kicked, this.argument, connection.username, 1);
                        if (c != null) {
                            if ("".equals(this.argument)) {
                                GameServer.kick(c, "UI_Policy_Kick", null);
                            } else {
                                GameServer.kick(c, "UI_Policy_KickReason", this.argument);
                            }

                            c.forceDisconnect("command-kick");
                        }
                    }
                    break;
                case Ban:
                    BanSystem.BanUser(this.username, connection, this.argument, true);
                    break;
                case UnBan:
                    BanSystem.BanUser(this.username, connection, this.argument, false);
                    break;
                case BanIP:
                    BanSystem.BanUserByIP(this.username, connection, this.argument, true);
                    break;
                case UnBanIP:
                    BanSystem.BanUserByIP(this.username, connection, this.argument, false);
                    break;
                case UnBanIPOnly:
                    BanSystem.BanIP(this.username, connection, this.argument, false);
                    break;
                case BanSteamID:
                    BanSystem.BanUserBySteamID(this.username, connection, this.argument, true);
                    break;
                case UnBanSteamID:
                    BanSystem.BanUserBySteamID(this.username, connection, this.argument, false);
            }

            if (connection.role.hasCapability(Capability.SeeNetworkUsers)) {
                INetworkPacket.send(connection, PacketTypes.PacketType.NetworkUsers);
            }
        } catch (SQLException var6) {
            var6.printStackTrace();
        }
    }

    public static enum Command {
        Kick,
        Ban,
        UnBan,
        BanIP,
        UnBanIP,
        UnBanIPOnly,
        BanSteamID,
        UnBanSteamID;
    }
}
