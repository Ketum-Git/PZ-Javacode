// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.WarManager;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class WarSyncPacket extends WarStateSyncPacket implements INetworkPacket {
    @JSONField
    protected long timestamp;

    public void set(WarManager.War war) {
        this.set(war.getOnlineID(), war.getAttacker(), war.getState());
        this.timestamp = war.getTimestamp();
    }

    @Override
    public void setData(Object... values) {
        this.set((WarManager.War)values[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putLong(this.timestamp);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.timestamp = b.getLong();
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        switch (this.state) {
            case Ended:
                WarManager.removeWar(this.onlineId, this.attacker);
                break;
            default:
                WarManager.updateWar(this.onlineId, this.attacker, this.state, this.timestamp);
        }

        LuaEventManager.triggerEvent("OnWarUpdate");
    }
}
