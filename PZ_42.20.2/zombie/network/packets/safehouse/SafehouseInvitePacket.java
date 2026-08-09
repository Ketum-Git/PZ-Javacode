// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.areas.SafeHouse;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHouseOwner;
import zombie.network.fields.SafehouseInvite;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = AntiCheat.SafeHouseOwner
)
public class SafehouseInvitePacket extends SafehouseInvite implements INetworkPacket, AntiCheatSafeHouseOwner.IAntiCheat {
    @Override
    public void setData(Object... values) {
        this.set((SafeHouse)values[0], (String)values[1], (String)values[2]);
    }

    @Override
    public void processClient(UdpConnection connection) {
        LuaEventManager.triggerEvent("ReceiveSafehouseInvite", this.getSafehouse(), this.getOwner(), this.getUsername());
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (SafeHouse.hasSafehouse(this.getUsername()) != null) {
            DebugType.Multiplayer.warn("player is already member or owner of safehouse");
        } else {
            this.getSafehouse().addInvite(this.getUsername());
            this.sendToClient(packetType, this.getUsername());
        }
    }
}
