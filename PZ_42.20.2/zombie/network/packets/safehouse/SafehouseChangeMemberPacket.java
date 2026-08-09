// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseMember;
import zombie.network.chat.ChatServer;
import zombie.network.fields.SafeHousePlayer;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 1,
    anticheats = AntiCheat.SafeHouseMember
)
public class SafehouseChangeMemberPacket extends SafeHousePlayer implements INetworkPacket, AntiCheatSafeHouseMember.IAntiCheat {
    @Override
    public void setData(Object... values) {
        this.set((SafeHouse)values[0], (String)values[1]);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            return false;
        } else if (this.getSafehouse().isOwner(this.getUsername())) {
            DebugType.Multiplayer.error("player is owner");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        SafeHouse.kickUserFromSafehouse(this.getSafehouse(), this.getUsername());
        ChatServer.getInstance().syncSafehouseChatMembers(this.getSafehouse().getId(), this.getSafehouse().getOwner(), this.getSafehouse().getPlayers());
    }
}
