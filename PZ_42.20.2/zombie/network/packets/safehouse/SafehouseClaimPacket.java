// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.IsoGridSquare;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseNotMember;
import zombie.network.chat.ChatServer;
import zombie.network.fields.SafeHouseTitle;
import zombie.network.fields.Square;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 1,
    anticheats = AntiCheat.SafeHousePlayer
)
public class SafehouseClaimPacket extends SafeHouseTitle implements INetworkPacket, AntiCheatSafeHouseNotMember.IAntiCheat {
    @JSONField
    private final Square square = new Square();

    @Override
    public void setData(Object... values) {
        super.set((IsoPlayer)values[1], (String)values[2]);
        this.square.set((IsoGridSquare)values[0]);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.square.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.square.write(b);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            return false;
        } else if (!this.square.isConsistent(connection)) {
            DebugType.Multiplayer.error("square is not found");
            return false;
        } else if (SafeHouse.getSafeHouse(this.square.getSquare()) != null) {
            DebugType.Multiplayer.error("safehouse is already claimed");
            return false;
        } else if (this.square.getSquare().getBuilding() == null) {
            DebugType.Multiplayer.error("building not found");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        String reason = SafeHouse.canBeSafehouse(this.square.getSquare(), this.getPlayer());
        if (!"".equals(reason)) {
            DebugType.Multiplayer.error("can't be safehouse: %s", reason);
        } else {
            SafeHouse safehouse = SafeHouse.addSafeHouse(this.square.getSquare(), this.getPlayer());
            safehouse.setTitle(this.getTitle());
            INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, safehouse);
            ChatServer.getInstance().createSafehouseChat(safehouse.getId());
            ChatServer.getInstance().syncSafehouseChatMembers(safehouse.getId(), safehouse.getOwner(), safehouse.getPlayers());
        }
    }
}
