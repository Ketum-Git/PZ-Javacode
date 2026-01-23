// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.core.raknet.UdpConnection;
import zombie.iso.areas.SafeHouse;
import zombie.network.packets.INetworkPacket;

public class AntiCheatSafeHousePlayer extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatSafeHousePlayer.IAntiCheat field = (AntiCheatSafeHousePlayer.IAntiCheat)packet;
        return !connection.role.hasCapability(Capability.CanSetupSafehouses)
                && !connection.havePlayer(field.getSafehouse().getOwner())
                && !connection.havePlayer(field.getPlayer())
            ? "player not found"
            : result;
    }

    public interface IAntiCheat {
        String getPlayer();

        SafeHouse getSafehouse();
    }
}
