// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoFire;
import zombie.network.fields.Square;
import zombie.network.packets.INetworkPacket;

public class AntiCheatSmoke extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatSmoke.IAntiCheat field = (AntiCheatSmoke.IAntiCheat)packet;
        if (field.getSmoke() && !IsoFire.CanAddSmoke(field.getSquare().getSquare(), field.getIgnition())) {
            return "invalid square";
        } else {
            return !connection.RelevantTo(field.getSquare().getX(), field.getSquare().getY()) ? "irrelevant square" : result;
        }
    }

    public interface IAntiCheat {
        boolean getSmoke();

        boolean getIgnition();

        Square getSquare();
    }
}
