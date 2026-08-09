// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.areas.SafeHouse;
import zombie.network.packets.INetworkPacket;

public class AntiCheatSafeHouseNotMember extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        if (connection.getRole().hasCapability(Capability.CanSetupSafehouses)) {
            return result;
        } else if (packet instanceof AntiCheatSafeHouseNotMember.IAntiCheat field) {
            if (!connection.hasPlayer(field.getUsername())) {
                return "player not found";
            } else {
                SafeHouse safeHouse = SafeHouse.hasSafehouse(field.getUsername());
                return safeHouse != null ? "player is already member or owner of safehouse" : result;
            }
        } else {
            DebugType.Multiplayer.error("Invalid packet-type=%s for anti-cheat=%s", packet.getClass().getSimpleName(), this.getClass().getSimpleName());
            return "";
        }
    }

    public interface IAntiCheat {
        String getUsername();
    }
}
