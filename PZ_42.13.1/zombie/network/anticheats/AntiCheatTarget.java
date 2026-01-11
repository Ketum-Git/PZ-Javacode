// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import java.util.Arrays;
import zombie.core.raknet.UdpConnection;
import zombie.network.fields.hit.Character;
import zombie.network.packets.INetworkPacket;

public class AntiCheatTarget extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatTarget.IAntiCheat field = (AntiCheatTarget.IAntiCheat)packet;
        return Arrays.stream(connection.players).noneMatch(p -> p.getOnlineID() == field.getTargetCharacter().getCharacter().getOnlineID())
            ? "invalid target"
            : result;
    }

    public interface IAntiCheat {
        Character getTargetCharacter();
    }
}
