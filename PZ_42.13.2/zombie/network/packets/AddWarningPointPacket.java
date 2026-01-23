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
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.AddUserlog, handlingType = 1)
public class AddWarningPointPacket implements INetworkPacket {
    @JSONField
    String username;
    @JSONField
    String reason;
    @JSONField
    int amount;

    @Override
    public void setData(Object... values) {
        this.username = (String)values[0];
        this.reason = (String)values[1];
        this.amount = (Integer)values[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        GameWindow.WriteString(b.bb, this.username);
        GameWindow.WriteString(b.bb, this.reason);
        b.putInt(this.amount);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.username = GameWindow.ReadString(b);
        this.reason = GameWindow.ReadString(b);
        this.amount = b.getInt();
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        try {
            ServerWorldDatabase.instance.addWarningPoint(this.username, this.reason, this.amount, connection.username);
        } catch (SQLException var4) {
            var4.printStackTrace();
        }

        LoggerManager.getLogger("admin")
            .write(connection.username + " added " + this.amount + " warning point(s) on " + this.username + ", reason:" + this.reason);
        IsoPlayer user = GameServer.getPlayerByRealUserName(this.username);
        if (user != null) {
            INetworkPacket.send(
                user, PacketTypes.PacketType.WorldMessage, connection.username, " gave you " + this.amount + " warning point(s), reason: " + this.reason + " "
            );
        }
    }
}
