// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;

public class AntiCheatSafeHouseSurvivor extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatSafeHouseSurvivor.IAntiCheat field = (AntiCheatSafeHouseSurvivor.IAntiCheat)packet;
        if (!connection.role.hasCapability(Capability.CanSetupSafehouses)) {
            int daysSurvived = ServerOptions.instance.safehouseDaySurvivedToClaim.getValue();
            if (daysSurvived > 0 && field.getSurvivor().getHoursSurvived() < daysSurvived * 24) {
                return String.format("player \"%s\" not survived enough", field.getSurvivor().getUsername());
            }
        }

        return result;
    }

    public interface IAntiCheat {
        IsoPlayer getSurvivor();
    }
}
