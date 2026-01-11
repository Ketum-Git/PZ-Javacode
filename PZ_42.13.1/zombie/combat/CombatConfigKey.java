// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.combat;

import zombie.UsedFromLua;

@UsedFromLua
public enum CombatConfigKey {
    BASE_WEAPON_DAMAGE_MULTIPLIER(CombatConfigCategory.GENERAL, 0.3F, 0.0F, 1.0F),
    WEAPON_LEVEL_DAMAGE_MULTIPLIER_INCREMENT(CombatConfigCategory.GENERAL, 0.1F, 0.0F, 1.0F),
    PLAYER_RECEIVED_DAMAGE_MULTIPLIER(CombatConfigCategory.GENERAL, 0.4F, 0.0F, 1.0F),
    NON_PLAYER_RECEIVED_DAMAGE_MULTIPLIER(CombatConfigCategory.GENERAL, 1.5F, 0.0F, 10.0F),
    HEAD_HIT_DAMAGE_SPLIT_MODIFIER(CombatConfigCategory.GENERAL, 3.0F, 0.0F, 10.0F),
    LEG_HIT_DAMAGE_SPLIT_MODIFIER(CombatConfigCategory.GENERAL, 0.05F, 0.0F, 1.0F),
    ADDITIONAL_CRITICAL_HIT_CHANCE_FROM_BEHIND(CombatConfigCategory.GENERAL, 30.0F, 0.0F, 100.0F),
    ADDITIONAL_CRITICAL_HIT_CHANCE_DEFAULT(CombatConfigCategory.GENERAL, 5.0F, 0.0F, 100.0F),
    RECOIL_DELAY(CombatConfigCategory.FIREARM, 10.0F, 0.0F, 100.0F),
    POINT_BLANK_DISTANCE(CombatConfigCategory.FIREARM, 3.5F, 0.0F, 10.0F),
    LOW_LIGHT_THRESHOLD(CombatConfigCategory.FIREARM, 0.75F, 0.0F, 1.0F),
    LOW_LIGHT_TO_HIT_MAXIMUM_PENALTY(CombatConfigCategory.FIREARM, 50.0F, 0.0F, 100.0F),
    POINT_BLANK_TO_HIT_MAXIMUM_BONUS(CombatConfigCategory.FIREARM, 40.0F, 0.0F, 100.0F),
    POINT_BLANK_DROP_OFF_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 0.7F, 0.0F, 1.0F),
    POST_SHOT_AIMING_DELAY_RECOIL_MODIFIER(CombatConfigCategory.FIREARM, 0.25F, 0.0F, 1.0F),
    POST_SHOT_AIMING_DELAY_AIMING_MODIFIER(CombatConfigCategory.FIREARM, 0.05F, 0.0F, 1.0F),
    OPTIMAL_RANGE_TO_HIT_MAXIMUM_BONUS(CombatConfigCategory.FIREARM, 15.0F, 0.0F, 100.0F),
    OPTIMAL_RANGE_DROP_OFF_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 4.0F, 0.0F, 10.0F),
    OPTIMAL_RANGE_DROP_OFF_TO_HIT_PENALTY_INCREMENT(CombatConfigCategory.FIREARM, 0.3F, 0.0F, 1.0F),
    MINIMUM_TO_HIT_CHANCE(CombatConfigCategory.FIREARM, 5.0F, 0.0F, 100.0F),
    MAXIMUM_START_TO_HIT_CHANCE(CombatConfigCategory.FIREARM, 95.0F, 0.0F, 100.0F),
    MAXIMUM_TO_HIT_CHANCE(CombatConfigCategory.FIREARM, 100.0F, 0.0F, 100.0F),
    MOVING_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 5.0F, 0.0F, 100.0F),
    RUNNING_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 15.0F, 0.0F, 100.0F),
    SPRINTING_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 25.0F, 0.0F, 100.0F),
    MARKSMAN_TRAIT_TO_HIT_BONUS(CombatConfigCategory.FIREARM, 20.0F, 0.0F, 100.0F),
    ARM_PAIN_TO_HIT_MODIFIER(CombatConfigCategory.FIREARM, 0.1F, 0.0F, 1.0F),
    PANIC_TO_HIT_BASE_PENALTY(CombatConfigCategory.FIREARM, 4.0F, 0.0F, 10.0F),
    PANIC_TO_HIT_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 0.5F, 0.0F, 1.0F),
    STRESS_TO_HIT_BASE_PENALTY(CombatConfigCategory.FIREARM, 4.0F, 0.0F, 10.0F),
    STRESS_TO_HIT_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 0.5F, 0.0F, 1.0F),
    TIRED_TO_HIT_BASE_PENALTY(CombatConfigCategory.FIREARM, 2.5F, 0.0F, 10.0F),
    ENDURANCE_TO_HIT_BASE_PENALTY(CombatConfigCategory.FIREARM, 2.5F, 0.0F, 10.0F),
    DRUNK_TO_HIT_BASE_PENALTY(CombatConfigCategory.FIREARM, 4.0F, 0.0F, 10.0F),
    DRUNK_TO_HIT_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 0.5F, 0.0F, 1.0F),
    WIND_INTENSITY_TO_HIT_PENALTY(CombatConfigCategory.FIREARM, 6.0F, 0.0F, 10.0F),
    WIND_INTENSITY_TO_HIT_AIMING_MODIFIER(CombatConfigCategory.FIREARM, 0.2F, 0.0F, 1.0F),
    WIND_INTENSITY_TO_HIT_MINIMUM_MARKSMAN_MODIFIER(CombatConfigCategory.FIREARM, 0.6F, 0.0F, 1.0F),
    WIND_INTENSITY_TO_HIT_MAXIMUM_MARKSMAN_MODIFIER(CombatConfigCategory.FIREARM, 1.0F, 0.0F, 10.0F),
    RAIN_INTENSITY_TO_HIT_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 0.5F, 0.0F, 1.0F),
    FOG_INTENSITY_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 10.0F, 0.0F, 100.0F),
    POINT_BLANK_MAXIMUM_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 1.0F, 0.0F, 10.0F),
    SIGHTLESS_TO_HIT_BASE_DISTANCE(CombatConfigCategory.FIREARM, 15.0F, 0.0F, 100.0F),
    SIGHTLESS_TO_HIT_PRONE_MODIFIER(CombatConfigCategory.FIREARM, 2.0F, 0.0F, 10.0F),
    SIGHTLESS_AIM_DELAY_TO_HIT_DISTANCE_MODIFIER(CombatConfigCategory.FIREARM, 0.1F, 0.0F, 1.0F),
    PIERCING_BULLET_DAMAGE_REDUCTION(CombatConfigCategory.FIREARM, 5.0F, 0.0F, 100.0F),
    FIREARM_RECOIL_MUSCLE_STRAIN_MODIFIER(CombatConfigCategory.FIREARM, 0.05F, 0.0F, 1.0F),
    DRIVEBY_DOT_OPTIMAL_ANGLE(CombatConfigCategory.FIREARM, -0.7F, -1.0F, 1.0F),
    DRIVEBY_DOT_MAXIMUM_ANGLE(CombatConfigCategory.FIREARM, -0.1F, -1.0F, 1.0F),
    DRIVEBY_DOT_TO_HIT_MAXIMUM_PENALTY(CombatConfigCategory.FIREARM, 40.0F, 0.0F, 100.0F),
    GLOBAL_MELEE_DAMAGE_REDUCTION_MULTIPLIER(CombatConfigCategory.MELEE, 0.15F, 0.0F, 1.0F),
    DAMAGE_PENALTY_ONE_HANDED_TWO_HANDED_WEAPON_MULTIPLIER(CombatConfigCategory.MELEE, 0.5F, 0.0F, 1.0F),
    ENDURANCE_LOSS_TWO_HANDED_PENALTY_DIVISOR(CombatConfigCategory.MELEE, 1.5F, 0.0F, 10.0F),
    ENDURANCE_LOSS_TWO_HANDED_PENALTY_SCALE(CombatConfigCategory.MELEE, 10.0F, 0.0F, 100.0F),
    ENDURANCE_LOSS_FLOOR_SHOVE_MULTIPLIER(CombatConfigCategory.MELEE, 2.0F, 0.0F, 10.0F),
    ENDURANCE_LOSS_CLOSE_KILL_MODIFIER(CombatConfigCategory.MELEE, 0.2F, 0.0F, 1.0F),
    ENDURANCE_LOSS_BASE_SCALE(CombatConfigCategory.MELEE, 0.28F, 0.0F, 1.0F),
    ENDURANCE_LOSS_WEIGHT_MODIFIER(CombatConfigCategory.MELEE, 0.3F, 0.0F, 1.0F),
    ENDURANCE_LOSS_FINAL_MULTIPLIER(CombatConfigCategory.MELEE, 0.04F, 0.0F, 1.0F),
    BALLISTICS_CONTROLLER_DISTANCE_THRESHOLD(CombatConfigCategory.BALLISTICS, 2.75F, 0.0F, 10.0F);

    private final CombatConfigCategory category;
    private final float defaultValue;
    private final float minimum;
    private final float maximum;

    private CombatConfigKey(final CombatConfigCategory category, final float defaultValue, final float minimum, final float maximum) {
        this.category = category;
        this.defaultValue = defaultValue;
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public CombatConfigCategory getCategory() {
        return this.category;
    }

    public float getDefaultValue() {
        return this.defaultValue;
    }

    public float getMinimum() {
        return this.minimum;
    }

    public float getMaximum() {
        return this.maximum;
    }
}
