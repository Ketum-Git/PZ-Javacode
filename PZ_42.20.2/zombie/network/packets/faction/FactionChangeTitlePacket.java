// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.faction;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.FactionId;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class FactionChangeTitlePacket extends FactionId implements INetworkPacket {
    @JSONField
    private String title;

    @Override
    public void setData(Object... values) {
        this.set((Faction)values[0]);
        this.title = (String)values[1];
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putUTF(this.title);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.title = b.getUTF();
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            return false;
        } else if (StringUtils.isNullOrEmpty(this.title)) {
            DebugType.Multiplayer.error("title is not set");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.getFaction().setName(this.title);
        LuaEventManager.triggerEvent("SyncFaction", this.title);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.getRole().hasCapability(Capability.FactionCheat) && !connection.hasPlayer(this.getFaction().getOwner())) {
            DebugType.Multiplayer.error("owner not found");
        } else {
            this.getFaction().setName(this.title);
            this.sendToClients(packetType, null);
        }
    }
}
