// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.WorkWithUserlog, handlingType = 1)
public class RemoveUserlogPacket implements INetworkPacket {
    @JSONField
    String username;
    @JSONField
    String type;
    @JSONField
    String text;

    @Override
    public void setData(Object... values) {
        this.username = (String)values[0];
        this.type = (String)values[1];
        this.text = (String)values[2];
    }

    @Override
    public void write(ByteBufferWriter b) {
        GameWindow.WriteString(b.bb, this.username);
        GameWindow.WriteString(b.bb, this.type);
        GameWindow.WriteString(b.bb, this.text);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.username = GameWindow.ReadString(b);
        this.type = GameWindow.ReadString(b);
        this.text = GameWindow.ReadString(b);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        try {
            ServerWorldDatabase.instance.removeUserLog(this.username, this.type, this.text);
        } catch (SQLException var4) {
            var4.printStackTrace();
        }

        LoggerManager.getLogger("admin").write(connection.username + " removed log on user " + this.username + ", type:" + this.type + ", log: " + this.text);
    }
}
