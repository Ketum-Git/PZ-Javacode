// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatRecipe extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        connection.validator.checksumTimeoutReset();
        AntiCheatRecipe.IAntiCheat field = (AntiCheatRecipe.IAntiCheat)packet;
        return field.getClientChecksum() != field.getServerChecksum() ? "invalid checksum" : result;
    }

    public interface IAntiCheat {
        long getClientChecksum();

        long getServerChecksum();
    }
}
