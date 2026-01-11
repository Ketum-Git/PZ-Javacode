// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitWeaponRange extends AbstractAntiCheat {
    private static final float predictedAdditionalRange = 1.0F;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatHitWeaponRange.IAntiCheat field = (AntiCheatHitWeaponRange.IAntiCheat)packet;
        HandWeapon weapon = field.getHandWeapon();
        if (weapon == null) {
            return "weapon not found";
        } else {
            return field.getDistance() - 1.0F > weapon.getMaxRange()
                ? String.format("distance=%f > range=%f", field.getDistance(), weapon.getMaxRange())
                : result;
        }
    }

    public interface IAntiCheat {
        HandWeapon getHandWeapon();

        float getDistance();
    }
}
