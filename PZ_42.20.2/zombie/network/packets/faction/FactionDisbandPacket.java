// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.faction;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.chat.ChatServer;
import zombie.network.fields.FactionId;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class FactionDisbandPacket extends FactionId implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.set((Faction)values[0]);
    }

    @Override
    public void processClient(UdpConnection connection) {
        Faction.getFactions().remove(this.getFaction());
        LuaEventManager.triggerEvent("SyncFaction", this.getFaction().getName());
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.getRole().hasCapability(Capability.FactionCheat) && !connection.hasPlayer(this.getFaction().getOwner())) {
            DebugType.Multiplayer.error("owner not found");
        } else {
            Faction.getFactions().remove(this.getFaction());
            this.sendToClients(packetType, null);
            ChatServer.getInstance().removeFactionChat(this.getFaction().getName());
            DebugType.Multiplayer.debugln("faction: removed %s owner=%s", this.getFaction().getName(), this.getFaction().getOwner());
        }
    }
}
