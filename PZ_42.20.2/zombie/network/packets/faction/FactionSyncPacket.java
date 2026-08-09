// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.faction;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.ColorInfo;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.FactionTag;
import zombie.network.fields.Group;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class FactionSyncPacket extends Group implements INetworkPacket {
    @JSONField
    private final FactionTag factionTag = new FactionTag();

    @Override
    public void set(Faction faction) {
        super.set(faction);
        this.factionTag.set(faction);
    }

    @Override
    public void setData(Object... values) {
        this.set((Faction)values[0]);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.factionTag.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.factionTag.write(b);
    }

    @Override
    public void processClient(UdpConnection connection) {
        Faction faction = Faction.getFaction(this.title);
        if (faction == null) {
            faction = new Faction(this.title, this.owner);
            Faction.getFactions().add(faction);
        }

        faction.setName(this.title);
        faction.setOwner(this.owner);
        faction.getPlayers().clear();
        faction.getPlayers().addAll(this.members);
        if (this.factionTag.getTag() != null) {
            faction.setTag(this.factionTag.getTag());
            faction.setTagColor(new ColorInfo().setABGR(this.factionTag.getColor()));
        }

        LuaEventManager.triggerEvent("SyncFaction", this.title);
    }
}
