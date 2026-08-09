// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.network.fields.hit.Hit;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitDamage extends AbstractAntiCheat {
    private static final int MAX_DAMAGE = 100;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        if (packet instanceof AntiCheatHitDamage.IAntiCheat field) {
            float damage = field.getHit().getDamage();
            return damage > 100.0F ? String.format("damage=%f is too big", damage) : result;
        } else {
            DebugType.Multiplayer.error("Invalid packet-type=%s for anti-cheat=%s", packet.getClass().getSimpleName(), this.getClass().getSimpleName());
            return "";
        }
    }

    public interface IAntiCheat {
        Hit getHit();
    }
}
