// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatXPPlayer extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatXPPlayer.IAntiCheat field = (AntiCheatXPPlayer.IAntiCheat)packet;
        if (connection.role.hasCapability(Capability.AddXP)) {
            return result;
        } else {
            return !connection.havePlayer(field.getPlayer()) ? "invalid player" : result;
        }
    }

    public interface IAntiCheat {
        IsoPlayer getPlayer();

        float getAmount();
    }
}
