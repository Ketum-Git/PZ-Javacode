// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.JSONField;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.CanSeeMessageForAdmin, handlingType = 2)
public class MessageForAdminPacket implements INetworkPacket {
    @JSONField
    String message;
    @JSONField
    int x;
    @JSONField
    int y;
    @JSONField
    int z;

    @Override
    public void setData(Object... values) {
        this.message = (String)values[0];
        this.x = (Integer)values[1];
        this.y = (Integer)values[2];
        this.z = (Integer)values[3];
    }

    @Override
    public void write(ByteBufferWriter b) {
        GameWindow.WriteString(b.bb, this.message);
        b.putInt(this.x);
        b.putInt(this.y);
        b.putInt(this.z);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.message = GameWindow.ReadString(b);
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.getInt();
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (GameClient.connection.role.hasCapability(Capability.CanSeeMessageForAdmin)) {
            LuaEventManager.triggerEvent("OnAdminMessage", this.message, this.x, this.y, this.z);
        }
    }
}
