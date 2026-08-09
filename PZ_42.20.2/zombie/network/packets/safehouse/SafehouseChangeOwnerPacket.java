// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseNotMember;
import zombie.network.anticheats.AntiCheatSafeHouseOwner;
import zombie.network.chat.ChatServer;
import zombie.network.fields.SafeHousePlayer;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 1,
    anticheats = AntiCheat.SafeHouseOwner
)
public class SafehouseChangeOwnerPacket
    extends SafeHousePlayer
    implements INetworkPacket,
    AntiCheatSafeHouseOwner.IAntiCheat,
    AntiCheatSafeHouseNotMember.IAntiCheat {
    @Override
    public void setData(Object... values) {
        this.set((SafeHouse)values[0], (String)values[1]);
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
            } else if (SafeHouse.hasNotSurvivedEnoughToClaim(isoPlayer)) {
                DebugType.Multiplayer.error("player is not survived enough");
                return false;
            } else if (this.getSafehouse().isOwner(isoPlayer)) {
                DebugType.Multiplayer.error("player is already owner");
                return false;
            } else {
                SafeHouse newSafeHouse = SafeHouse.hasSafehouse(isoPlayer);
                if (newSafeHouse != null && newSafeHouse != this.getSafehouse()) {
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
        String member = this.getSafehouse().getOwner();
        this.getSafehouse().setOwner(this.getUsername());
        this.getSafehouse().addPlayer(member);
        INetworkPacket.sendToAll(PacketTypes.PacketType.SafehouseSync, this.getSafehouse());
        ChatServer.getInstance().syncSafehouseChatMembers(this.getSafehouse().getId(), this.getSafehouse().getOwner(), this.getSafehouse().getPlayers());
    }
}
