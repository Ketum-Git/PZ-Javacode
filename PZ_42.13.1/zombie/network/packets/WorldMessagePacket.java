// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.chat.ChatManager;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.WorkWithUserlog, handlingType = 1)
public class WorldMessagePacket implements INetworkPacket {
    @JSONField
    String username;
    @JSONField
    String message;

    @Override
    public void setData(Object... values) {
        this.username = (String)values[0];
        this.message = (String)values[1];
    }

    @Override
    public void write(ByteBufferWriter b) {
        GameWindow.WriteString(b.bb, this.username);
        GameWindow.WriteString(b.bb, this.message);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.username = GameWindow.ReadString(b);
        this.message = GameWindow.ReadString(b);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.allChatMuted) {
            if (this.message.length() > 256) {
                this.message = this.message.substring(0, 256);
            }

            this.sendToClients(PacketTypes.PacketType.WorldMessage, null);
            GameServer.discordBot.sendMessage(this.username, this.message);
            LoggerManager.getLogger("chat").write(connection.index + " \"" + connection.username + "\" A \"" + this.message + "\"");
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.message = this.message.replaceAll("<", "&lt;");
        this.message = this.message.replaceAll(">", "&gt;");
        ChatManager.getInstance().addMessage(this.username, this.message);
    }
}
