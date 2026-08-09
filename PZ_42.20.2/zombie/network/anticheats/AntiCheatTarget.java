// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.packets.INetworkPacket;

public class AntiCheatTarget extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        if (packet instanceof AntiCheatTarget.IAntiCheat field) {
            return !connection.hasPlayer(field.getPlayerId()) ? "invalid target" : result;
        } else {
            DebugType.Multiplayer.error("Invalid packet-type=%s for anti-cheat=%s", packet.getClass().getSimpleName(), this.getClass().getSimpleName());
            return "";
        }
    }

    public interface IAntiCheat {
        short getPlayerId();
    }
}
