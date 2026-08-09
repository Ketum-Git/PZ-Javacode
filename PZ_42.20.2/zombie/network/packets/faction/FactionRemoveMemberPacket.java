// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.faction;

import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.chat.ChatServer;
import zombie.network.fields.FactionPlayer;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class FactionRemoveMemberPacket extends FactionPlayer implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.set((Faction)values[0], (String)values[1]);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            return false;
        } else if (this.getFaction() != Faction.getPlayerFaction(this.getUsername())) {
            DebugType.Multiplayer.error("player is not member or owner of faction");
            return false;
        } else if (this.getFaction().isOwner(this.getUsername())) {
            DebugType.Multiplayer.error("player is owner");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.getRole().hasCapability(Capability.FactionCheat)
            && !connection.hasPlayer(this.getUsername())
            && !connection.hasPlayer(this.getFaction().getOwner())) {
            DebugType.Multiplayer.error("player not found and sender is not owner");
        } else {
            this.getFaction().removePlayer(this.getUsername());
            INetworkPacket.sendToAll(PacketTypes.PacketType.FactionSync, this.getFaction());
            ChatServer.getInstance().syncFactionChatMembers(this.getFaction().getName(), this.getFaction().getOwner(), this.getFaction().getPlayers());
        }
    }
}
