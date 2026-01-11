// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatPlayer extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        connection.validator.playerUpdateTimeoutReset();
        AntiCheatPlayer.IAntiCheat field = (AntiCheatPlayer.IAntiCheat)packet;

        for (IsoPlayer player : connection.players) {
            if (player == field.getPlayer()) {
                return result;
            }
        }

        return "invalid player";
    }

    public interface IAntiCheat {
        IsoPlayer getPlayer();
    }
}
