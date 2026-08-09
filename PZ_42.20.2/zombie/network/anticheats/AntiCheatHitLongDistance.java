// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitLongDistance extends AbstractAntiCheat {
    private static final float PREDICTION_COMPENSATION_RANGE = 1.2F;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        if (packet instanceof AntiCheatHitLongDistance.IAntiCheat field) {
            float distance = field.getDistance();
            float range = connection.getRelevantRange() * 8 * 1.2F;
            return distance > range ? String.format("distance=%f > range=%f", distance, range) : result;
        } else {
            DebugType.Multiplayer.error("Invalid packet-type=%s for anti-cheat=%s", packet.getClass().getSimpleName(), this.getClass().getSimpleName());
            return "";
        }
    }

    public interface IAntiCheat {
        float getDistance();
    }
}
