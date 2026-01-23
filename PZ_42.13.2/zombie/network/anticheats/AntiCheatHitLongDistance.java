// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitLongDistance extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatHitLongDistance.IAntiCheat field = (AntiCheatHitLongDistance.IAntiCheat)packet;
        float distance = field.getDistance();
        int range = connection.releventRange * 8;
        return distance > range ? String.format("distance=%f > range=%d", distance, range) : result;
    }

    public interface IAntiCheat {
        float getDistance();
    }
}
