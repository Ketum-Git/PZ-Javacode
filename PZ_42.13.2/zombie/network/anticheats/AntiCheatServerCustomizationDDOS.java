// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatServerCustomizationDDOS extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatServerCustomizationDDOS.IAntiCheat field = (AntiCheatServerCustomizationDDOS.IAntiCheat)packet;
        return System.currentTimeMillis() / 1000L - field.getLastConnect() <= 3600L ? "invalid rate" : result;
    }

    public interface IAntiCheat {
        long getLastConnect();
    }
}
