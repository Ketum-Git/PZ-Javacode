// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.faction;

import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.chat.ChatServer;
import zombie.network.fields.FactionPlayer;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class FactionChangeOwnerPacket extends FactionPlayer implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.set((Faction)values[0], (String)values[1]);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            return false;
        } else {
            IsoPlayer isoPlayer = GameServer.getPlayerByUserName(this.getUsername());
            if (isoPlayer == null) {
                DebugType.Multiplayer.error("player is not found");
                return false;
            } else if (!Faction.canCreateFaction(isoPlayer)) {
                DebugType.Multiplayer.error("player can't create faction");
                return false;
            } else if (this.getFaction().isOwner(this.getUsername())) {
                DebugType.Multiplayer.error("player is already owner");
                return false;
            } else {
                Faction faction = Faction.getPlayerFaction(this.getUsername());
                if (faction != null && faction != this.getFaction()) {
                    DebugType.Multiplayer.error("player is already member");
                    return false;
                } else {
                    return true;
                }
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.getRole().hasCapability(Capability.FactionCheat) && !connection.hasPlayer(this.getFaction().getOwner())) {
            DebugType.Multiplayer.error("owner not found");
        } else {
            String member = this.getFaction().getOwner();
            this.getFaction().setOwner(this.getUsername());
            this.getFaction().addPlayer(member);
            INetworkPacket.sendToAll(PacketTypes.PacketType.FactionSync, this.getFaction());
            ChatServer.getInstance().syncFactionChatMembers(this.getFaction().getName(), this.getFaction().getOwner(), this.getFaction().getPlayers());
        }
    }
}
