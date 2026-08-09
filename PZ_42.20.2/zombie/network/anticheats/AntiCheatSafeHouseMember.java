// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.areas.SafeHouse;
import zombie.network.packets.INetworkPacket;

public class AntiCheatSafeHouseMember extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        if (connection.getRole().hasCapability(Capability.CanSetupSafehouses)) {
            return result;
        } else if (packet instanceof AntiCheatSafeHouseMember.IAntiCheat field) {
            if (!connection.hasPlayer(field.getUsername()) && !connection.hasPlayer(field.getSafehouse().getOwner())) {
                return "player not found and sender is not owner";
            } else {
                SafeHouse safeHouse = SafeHouse.hasSafehouse(field.getUsername());
                return safeHouse != null && safeHouse.getOnlineID() == field.getSafehouse().getOnlineID()
                    ? result
                    : "player is not member or owner of safehouse";
            }
        } else {
            DebugType.Multiplayer.error("Invalid packet-type=%s for anti-cheat=%s", packet.getClass().getSimpleName(), this.getClass().getSimpleName());
            return "";
        }
    }

    public interface IAntiCheat {
        SafeHouse getSafehouse();

        String getUsername();
    }
}
