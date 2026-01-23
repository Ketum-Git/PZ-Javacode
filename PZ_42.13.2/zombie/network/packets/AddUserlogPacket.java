// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.AddUserlog, handlingType = 1)
public class AddUserlogPacket implements INetworkPacket {
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
        ServerWorldDatabase.instance.addUserlog(this.username, Userlog.UserlogType.FromString(this.type), this.text, connection.username, 1);
        LoggerManager.getLogger("admin").write(connection.username + " added log on user " + this.username + ", log: " + this.text);
    }
}
