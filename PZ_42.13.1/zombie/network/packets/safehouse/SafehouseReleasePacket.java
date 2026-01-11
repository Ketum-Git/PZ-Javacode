// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.safehouse;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatSafeHousePlayer;
import zombie.network.fields.SafehouseID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 1,
    anticheats = AntiCheat.SafeHousePlayer
)
public class SafehouseReleasePacket extends SafehouseID implements INetworkPacket, AntiCheatSafeHousePlayer.IAntiCheat {
    @Override
    public void setData(Object... values) {
        this.set((SafeHouse)values[0]);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        if (!super.isConsistent(connection)) {
            DebugLog.Multiplayer.error("safehouse is not found");
            return false;
        } else if (!connection.havePlayer(this.getSafehouse().getOwner())) {
            DebugLog.Multiplayer.error("player releasing safehouse is not the owner");
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        SafeHouse.removeSafeHouse(this.getSafehouse());

        for (UdpConnection c : GameServer.udpEngine.connections) {
            if (c.isFullyConnected()) {
                INetworkPacket.send(c, PacketTypes.PacketType.SafehouseSync, this.getSafehouse(), true);
            }
        }
    }

    @Override
    public String getPlayer() {
        return this.getSafehouse().getOwner();
    }
}
