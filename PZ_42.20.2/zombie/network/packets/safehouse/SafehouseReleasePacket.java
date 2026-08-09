// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseOwner;
import zombie.network.chat.ChatServer;
import zombie.network.fields.SafehouseID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = AntiCheat.SafeHouseOwner
)
public class SafehouseReleasePacket extends SafehouseID implements INetworkPacket, AntiCheatSafeHouseOwner.IAntiCheat {
    @Override
    public void setData(Object... values) {
        this.set((SafeHouse)values[0]);
    }

    @Override
    public void processClient(UdpConnection connection) {
        SafeHouse.removeSafeHouse(this.getSafehouse());
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        SafeHouse.removeSafeHouse(this.getSafehouse());
        this.sendToClients(PacketTypes.PacketType.SafehouseRelease, null);
        ChatServer.getInstance().removeSafehouseChat(this.getSafehouse().getId());
    }
}
