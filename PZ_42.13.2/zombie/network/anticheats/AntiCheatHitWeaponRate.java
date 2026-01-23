// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.characters.AttackRateChecker;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitWeaponRate extends AbstractAntiCheat {
    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatHitWeaponRate.IAntiCheat field = (AntiCheatHitWeaponRate.IAntiCheat)packet;
        HandWeapon weapon = field.getHandWeapon();
        if (weapon == null) {
            return "weapon not found";
        } else {
            AttackRateChecker checker = field.getWielder().getNetworkCharacterAI().attackRateChecker;
            int combatSpeed = 0;
            if (weapon.isAimedFirearm()) {
                if ("Auto".equals(weapon.getFireMode())) {
                    combatSpeed = 100;
                } else if (!weapon.isTwoHandWeapon()) {
                    combatSpeed = 200;
                } else {
                    combatSpeed = 300;
                }
            }

            return !weapon.isAimedFirearm()
                    && !"Auto".equals(weapon.getFireMode())
                    && !weapon.isTwoHandWeapon()
                    && !weapon.isBareHands()
                    && !weapon.isMelee()
                    && checker.check(weapon.getProjectileCount(), combatSpeed)
                ? String.format("rate < speed=%d", combatSpeed)
                : result;
        }
    }

    public interface IAntiCheat {
        HandWeapon getHandWeapon();

        float getDistance();

        IsoPlayer getWielder();

        IsoGameCharacter getTarget();
    }
}
