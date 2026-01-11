// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitShortDistance extends AbstractAntiCheat {
    private static final int MAX_RELEVANT_RANGE = 10;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatHitShortDistance.IAntiCheat field = (AntiCheatHitShortDistance.IAntiCheat)packet;
        float distance = field.getDistance();
        return distance > 10.0F ? String.format("distance=%f > range=%d", distance, 10) : result;
    }

    public interface IAntiCheat {
        float getDistance();
    }
}
