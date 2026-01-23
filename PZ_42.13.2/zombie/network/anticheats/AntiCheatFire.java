// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoFire;
import zombie.network.ServerOptions;
import zombie.network.fields.Square;
import zombie.network.packets.INetworkPacket;

public class AntiCheatFire extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatFire.IAntiCheat field = (AntiCheatFire.IAntiCheat)packet;
        if (!field.getSmoke()) {
            if (ServerOptions.instance.noFire.getValue()) {
                return "fire is disabled";
            }

            if (!IsoFire.CanAddFire(field.getSquare().getSquare(), field.getIgnition(), field.getSmoke())) {
                return "invalid square";
            }
        }

        return !connection.RelevantTo(field.getSquare().getX(), field.getSquare().getY()) ? "irrelevant square" : result;
    }

    public interface IAntiCheat {
        boolean getSmoke();

        boolean getIgnition();

        Square getSquare();
    }
}
