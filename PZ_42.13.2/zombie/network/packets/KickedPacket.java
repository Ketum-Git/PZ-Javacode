// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.chat.ChatManager;
import zombie.core.Translator;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.gameStates.IngameState;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.util.StringUtils;

@PacketSetting(ordering = 0, priority = 0, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 6)
public class KickedPacket implements INetworkPacket {
    @JSONField
    public String description;
    @JSONField
    public String reason;

    @Override
    public void setData(Object... values) {
        this.description = (String)values[0];
        this.reason = (String)values[1];
    }

    @Override
    public void write(ByteBufferWriter b) {
        GameWindow.WriteString(b.bb, this.description);
        GameWindow.WriteString(b.bb, this.reason);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.description = GameWindow.ReadString(b);
        this.reason = GameWindow.ReadString(b);
    }

    public String getMessage() {
        String message = Translator.getText(this.description);
        if (!StringUtils.isNullOrEmpty(this.reason)) {
            message = message + " ";
            String text = Translator.getTextOrNull(this.reason);
            if (text != null) {
                message = message + text;
            } else {
                message = message + this.reason;
            }
        }

        return message;
    }

    @Override
    public void processClient(UdpConnection connection) {
        String message = this.getMessage();
        if (GameWindow.states.current == IngameState.instance) {
            if (!StringUtils.isNullOrEmpty(message)) {
                ChatManager.getInstance().showServerChatMessage(message);
            }
        } else {
            LuaEventManager.triggerEvent("OnConnectFailed", message);
        }

        connection.username = null;
        GameWindow.kickReason = message;
        GameWindow.serverDisconnected = true;
        connection.forceDisconnect(message);
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        this.processClient(connection);
    }
}
