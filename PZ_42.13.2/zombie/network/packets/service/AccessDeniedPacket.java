// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.Translator;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 0, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 4)
public class AccessDeniedPacket implements INetworkPacket {
    @JSONField
    String reason;

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.reason);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.reason = GameWindow.ReadString(b);
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        String[] ss = this.reason.split("##");
        String translation = ss.length > 0
            ? Translator.getText("UI_OnConnectFailed_" + ss[0], ss.length > 1 ? ss[1] : null, ss.length > 2 ? ss[2] : null, ss.length > 3 ? ss[3] : null)
            : null;
        LuaEventManager.triggerEvent("OnConnectFailed", translation);
        DebugLog.Multiplayer.warn("ReceiveAccessDenied: " + translation);
    }

    @Override
    public void setData(Object... values) {
        this.reason = (String)values[0];
    }
}
