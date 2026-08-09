// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.faction;

import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugType;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.FactionId;
import zombie.network.fields.FactionTag;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class FactionChangeTagPacket extends FactionId implements INetworkPacket {
    private static final int MIN_FACTION_TAG_LENGTH = 1;
    private static final int MAX_FACTION_TAG_LENGTH = 4;
    @JSONField
    private final FactionTag factionTag = new FactionTag();

    @Override
    public void setData(Object... values) {
        this.set((Faction)values[0]);
        this.factionTag.set(this.getFaction());
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.factionTag.write(b);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.factionTag.parse(b, connection);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            return false;
        } else if (!this.factionTag.isConsistent(connection)) {
            DebugType.Multiplayer.error("tag is null");
            return false;
        } else if (Faction.tagExist(this.factionTag.getTag()) && !connection.hasPlayer(this.getFaction().getOwner())) {
            DebugType.Multiplayer.error("tag already exists");
            return false;
        } else {
            int tagLength = this.factionTag.getTag().length();
            if (tagLength < 1 || tagLength > 4) {
                DebugType.Multiplayer.error("tag length is incorrect");
                return false;
            } else if (!this.getFaction().canCreateTag()) {
                DebugType.Multiplayer.error("no enough players to create tag");
                return false;
            } else {
                return true;
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.getRole().hasCapability(Capability.FactionCheat) && !connection.hasPlayer(this.getFaction().getOwner())) {
            DebugType.Multiplayer.error("owner not found");
        } else {
            if (this.factionTag.getTag() != null) {
                this.getFaction().setTag(this.factionTag.getTag());
                this.getFaction().setTagColor(new ColorInfo().setABGR(this.factionTag.getColor()));
            }

            INetworkPacket.sendToAll(PacketTypes.PacketType.FactionSync, this.getFaction());
        }
    }
}
