// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitShortDistance extends AbstractAntiCheat {
    private static final int MAX_RELEVANT_RANGE = 10;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        if (packet instanceof AntiCheatHitShortDistance.IAntiCheat field) {
            float distance = field.getFurthestHitDistance();
            return distance > 10.0F ? String.format("distance=%f > range=%d", distance, 10) : result;
        } else {
            DebugType.Multiplayer.error("Invalid packet-type=%s for anti-cheat=%s", packet.getClass().getSimpleName(), this.getClass().getSimpleName());
            return "";
        }
    }

    public interface IAntiCheat {
        float getFurthestHitDistance();
    }
}
