// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.faction;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 3, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class FactionStatsPacket extends PlayerID implements INetworkPacket {
    @JSONField
    private boolean isShowTag;
    @JSONField
    private boolean isFactionPvp;

    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0]);
        this.isShowTag = this.getPlayer().isShowTag();
        this.isFactionPvp = this.getPlayer().isFactionPvp();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putBoolean(this.isShowTag);
        b.putBoolean(this.isFactionPvp);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.isShowTag = b.getBoolean();
        this.isFactionPvp = b.getBoolean();
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            DebugType.Multiplayer.error("player is not set");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.hasPlayer(this.getPlayer().getUsername())) {
            DebugType.Multiplayer.error("player not found");
        } else {
            this.getPlayer().setShowTag(this.isShowTag);
            this.getPlayer().setFactionPvp(this.isFactionPvp);
            this.sendToClients(PacketTypes.PacketType.FactionStats, connection);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.getPlayer().setShowTag(this.isShowTag);
        this.getPlayer().setFactionPvp(this.isFactionPvp);
        LuaEventManager.triggerEvent("SyncFaction", "");
    }
}
