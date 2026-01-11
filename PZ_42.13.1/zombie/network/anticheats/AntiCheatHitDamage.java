// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.fields.hit.Hit;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitDamage extends AbstractAntiCheat {
    private static final int MAX_DAMAGE = 100;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatHitDamage.IAntiCheat field = (AntiCheatHitDamage.IAntiCheat)packet;
        float damage = field.getHit().getDamage();
        return field.getHit().getDamage() > 100.0F ? String.format("damage=%f is too big", damage) : result;
    }

    public interface IAntiCheat {
        Hit getHit();
    }
}
