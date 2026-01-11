// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatPower extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatPower.IAntiCheat field = (AntiCheatPower.IAntiCheat)packet;
        if (connection.role.hasCapability(Capability.ToggleGodModHimself)) {
            return result;
        } else {
            short mask = 3072;
            return field.getPlayer().networkAi.doCheckAccessLevel() && (field.getBooleanVariables() & 3072) != 0 ? "invalid mode" : result;
        }
    }

    public interface IAntiCheat {
        short getBooleanVariables();

        IsoPlayer getPlayer();
    }
}
