// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import zombie.UsedFromLua;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;

@UsedFromLua
public class DebugCSVExportFirearms {
    public static void doCSV() throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter("FirearmsCSV.txt"))) {
            writer.println(
                "MODULE&TYPE,DISPLAY_NAME,MIN_DAMAGE,MAX_DAMAGE,MIN_RANGE,MAX_RANGE,MIN_SIGHT_RANGE,MAX_SIGHT_RANGE,WEIGHT,AIMING_PERK_CRIT_MODIFIER,AIMING_PERK_HIT_CHANCE_MODIFIER,AIMING_PERK_MIN_ANGLE_MODIFIER,AIMING_PERK_RANGE_MODIFER,AIMING_TIME,CONDITION_LOWER_CHANCE,CONDITION_MAX,CRIT_DAMAGE_MULTIPLIER,CRITICAL_CHANCE,CYCLIC_RATE_MODIFIER,HIT_CHANCE,JAM_GUN_CHANCE,KNOCK_BACK_ON_NO_DEATH,KNOCKDOWN_MOD,MAX_AMMO,MAX_HIT_COUNT,MINIMUM_SWING_TIME,PIERCING_BULLETS,PROJECTILE_COUNT,PROJECTILE_SPREAD,PROJECTILE_WEIGHT_CENTER,PUSH_BACK_MOD,RECOIL_DELAY,RELOAD_TIME,SOUND_GAIN,SOUND_RADIUS,SOUND_VOLUME,SPLAT_NUMBER,STOP_POWER,SWING_TIME"
            );

            for (Item item : ScriptManager.instance.getAllItems()) {
                if (item.isAimedFirearm) {
                    writer.println(
                        "%s.%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s"
                            .formatted(
                                item.getModuleName(),
                                item.getName(),
                                item.getDisplayName(),
                                item.minDamage,
                                item.maxDamage,
                                item.minRange,
                                item.maxRange,
                                item.minSightRange,
                                item.maxSightRange,
                                item.getActualWeight(),
                                item.aimingPerkCritModifier,
                                item.aimingPerkHitChanceModifier,
                                item.aimingPerkMinAngleModifier,
                                item.aimingPerkRangeModifier,
                                item.aimingTime,
                                item.conditionLowerChance,
                                item.conditionMax,
                                item.critDmgMultiplier,
                                item.criticalChance,
                                item.cyclicRateMultiplier,
                                item.hitChance,
                                item.jamGunChance,
                                item.knockBackOnNoDeath,
                                item.knockdownMod,
                                item.maxAmmo,
                                item.maxHitCount,
                                item.minimumSwingTime,
                                item.piercingBullets,
                                item.projectileCount,
                                item.projectileSpread,
                                item.projectileWeightCenter,
                                item.pushBackMod,
                                item.recoilDelay,
                                item.reloadTime,
                                item.soundGain,
                                item.soundRadius,
                                item.soundVolume,
                                item.splatNumber,
                                item.stopPower,
                                item.swingTime
                            )
                    );
                }
            }
        }
    }
}
