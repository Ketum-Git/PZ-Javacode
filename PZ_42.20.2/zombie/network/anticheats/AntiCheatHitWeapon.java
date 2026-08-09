// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.SandboxOptions;
import zombie.characters.IsoPlayer;
import zombie.characters.NetworkPlayerAI;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.inventory.types.HandWeapon;
import zombie.network.characters.AttackRateChecker;
import zombie.network.packets.INetworkPacket;

public class AntiCheatHitWeapon extends AbstractAntiCheat {
    private static final float PREDICTION_COMPENSATION_RANGE = 5.0F;
    private static final long SINGLE_FIREARM_RATE_MULTIPLIER = 150L;
    private static final long AUTO_FIREARM_RATE_MULTIPLIER = 15L;
    private static final long MELEE_RATE_MULTIPLIER = 400L;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        if (packet instanceof AntiCheatHitWeapon.IAntiCheat field) {
            IsoPlayer player = field.getWielder();
            if (player == null) {
                return "player not found";
            } else {
                HandWeapon weapon = field.getHandWeapon();
                if (weapon == null) {
                    return result;
                } else {
                    float distance = field.getDistance();
                    if (this.isDistanceExceeded(weapon, distance)) {
                        return String.format("distance=%f is greater than weapon range", distance);
                    } else if (weapon.isAimedFirearm()
                        && !field.isIgnoreDamage()
                        && !player.isUnlimitedAmmo()
                        && this.isAmmoExceeded(player, weapon, field.getShotID())) {
                        return "not enough ammo";
                    } else {
                        return this.isRateExceeded(player, weapon) ? "attack rate is too high" : result;
                    }
                }
            }
        } else {
            DebugType.Multiplayer.error("Invalid packet-type=%s for anti-cheat=%s", packet.getClass().getSimpleName(), this.getClass().getSimpleName());
            return "";
        }
    }

    private boolean isDistanceExceeded(HandWeapon weapon, float distance) {
        float range = PZMath.max(0.0F, distance - 5.0F);
        return range > weapon.getMaxRange();
    }

    private boolean isAmmoExceeded(IsoPlayer player, HandWeapon weapon, byte attackId) {
        NetworkPlayerAI networkAi = player.getNetworkCharacterAI();
        int ammoCount = networkAi.getShotID() == attackId ? networkAi.ammoBeforeShot : weapon.getCurrentAmmoCount() + (weapon.isRoundChambered() ? 1 : 0);
        return ammoCount <= 0;
    }

    private boolean isRateExceeded(IsoPlayer player, HandWeapon weapon) {
        AttackRateChecker attackRateChecker = player.getNetworkCharacterAI().attackRateChecker;
        int weaponHitCount = weapon.getProjectileCount() * weapon.getMaxHitCount();
        int hitCount = SandboxOptions.instance.multiHitZombies.getValue() ? PZMath.max(3, weaponHitCount) : weaponHitCount;
        if (!weapon.isAimedFirearm()) {
            return attackRateChecker.check(hitCount, 400L);
        } else {
            return "Auto".equals(weapon.getFireMode()) ? attackRateChecker.check(hitCount, 15L) : attackRateChecker.check(hitCount, 150L);
        }
    }

    public interface IAntiCheat {
        HandWeapon getHandWeapon();

        IsoPlayer getWielder();

        boolean isIgnoreDamage();

        byte getShotID();

        float getDistance();
    }
}
