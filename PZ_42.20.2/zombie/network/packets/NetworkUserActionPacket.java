// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.secure.PZcrypt;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.worldMap.WorldMapVisitedServer;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.ModifyNetworkUsers, handlingType = 1)
public class NetworkUserActionPacket implements INetworkPacket {
    @JSONField
    public NetworkUserActionPacket.Command action;
    @JSONField
    String username;
    @JSONField
    String argument;

    @Override
    public void setData(Object... values) {
        this.action = NetworkUserActionPacket.Command.valueOf((String)values[0]);
        this.username = (String)values[1];
        this.argument = (String)values[2];
        if (this.action == NetworkUserActionPacket.Command.SetPassword) {
            this.argument = PZcrypt.hash(ServerWorldDatabase.encrypt(this.argument));
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putEnum(this.action);
        b.putUTF(this.username);
        b.putUTF(this.argument);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.action = b.getEnum(NetworkUserActionPacket.Command.class);
        this.username = b.getUTF();
        this.argument = b.getUTF();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        try {
            IsoPlayer pl = GameServer.getPlayerByUserName(this.username);
            IsoPlayer admin = connection.players[0];
            UdpConnection c = GameServer.getConnectionFromPlayer(pl);
            switch (this.action) {
                case Add:
                    if (admin.getRole().hasCapability(Capability.ModifyNetworkUsers)) {
                        String password = PZcrypt.hash(ServerWorldDatabase.encrypt(this.argument));
                        ServerWorldDatabase.instance.addUser(this.username, password);
                    }
                    break;
                case Delete:
                    if (admin.getRole().hasCapability(Capability.ModifyNetworkUsers)
                        && admin.getRole().getPosition() >= ServerWorldDatabase.instance.getUserRoleNameByUsername(this.username).getPosition()) {
                        ServerWorldDatabase.instance.removeWholeUserLog(this.username);
                        ServerWorldDatabase.instance.removeUser(this.username);
                        WorldMapVisitedServer.getInstance().deleteUser(this.username);
                    }
                    break;
                case ResetTOTPSecret:
                    if (admin.getRole().hasCapability(Capability.ModifyNetworkUsers)
                        && admin.getRole().getPosition() >= ServerWorldDatabase.instance.getUserRoleNameByUsername(this.username).getPosition()) {
                        ServerWorldDatabase.instance.resetUserGoogleKey(this.username);
                    }
                    break;
                case ResetPassword:
                    if (admin.getRole().hasCapability(Capability.ModifyNetworkUsers)
                        && admin.getRole().getPosition() >= ServerWorldDatabase.instance.getUserRoleNameByUsername(this.username).getPosition()) {
                        ServerWorldDatabase.instance.setPassword(this.username, "");
                    }
                    break;
                case SetPassword:
                    if (admin.getRole().hasCapability(Capability.ModifyNetworkUsers)
                        && admin.getRole().getPosition() >= ServerWorldDatabase.instance.getUserRoleNameByUsername(this.username).getPosition()) {
                        ServerWorldDatabase.instance.setPassword(this.username, this.argument);
                    }
                    break;
                case SetRole:
                    if (admin.getRole().hasCapability(Capability.ChangeAccessLevel)) {
                        GameServer.changeRole(admin.getUsername(), connection, this.username, this.argument);
                    }
            }

            if (connection.getRole().hasCapability(Capability.SeeNetworkUsers)) {
                INetworkPacket.send(connection, PacketTypes.PacketType.NetworkUsers);
            }
        } catch (SQLException var7) {
            DebugType.General.printException(var7, LogSeverity.Error);
        }
    }

    public static enum Command {
        Add,
        Delete,
        ResetTOTPSecret,
        ResetPassword,
        SetPassword,
        SetRole;
    }
}
