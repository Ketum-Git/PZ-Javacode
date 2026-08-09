// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;

@UsedFromLua
public class CharacterStat {
    public static final Map<String, CharacterStat> REGISTRY = new HashMap<>();
    public static final CharacterStat ANGER = register("Anger", 0.0F, 1.0F, 0.0F);
    public static final CharacterStat BOREDOM = register("Boredom", 0.0F, 100.0F, 0.0F);
    public static final CharacterStat DISCOMFORT = register("Discomfort", 0.0F, 100.0F, 0.0F);
    public static final CharacterStat ENDURANCE = register("Endurance", 0.0F, 1.0F, 1.0F);
    public static final CharacterStat FATIGUE = register("Fatigue", 0.0F, 1.0F, 0.0F);
    public static final CharacterStat FITNESS = register("Fitness", -1.0F, 1.0F, 0.0F);
    public static final CharacterStat FOOD_SICKNESS = register("FoodSickness", 0.0F, 100.0F, 0.0F);
    public static final CharacterStat HUNGER = register("Hunger", 0.0F, 1.0F, 0.0F);
    public static final CharacterStat IDLENESS = register("Idleness", 0.0F, 1.0F, 0.0F);
    public static final CharacterStat INTOXICATION = register("Intoxication", 0.0F, 100.0F, 0.0F);
    public static final CharacterStat MORALE = register("Morale", 0.0F, 1.0F, 1.0F);
    public static final CharacterStat NICOTINE_WITHDRAWAL = register("NicotineWithdrawal", 0.0F, 0.51F, 0.0F);
    public static final CharacterStat PAIN = register("Pain", 0.0F, 100.0F, 0.0F);
    public static final CharacterStat PANIC = register("Panic", 0.0F, 100.0F, 0.0F);
    public static final CharacterStat POISON = register("Poison", 0.0F, 100.0F, 0.0F);
    public static final CharacterStat SANITY = register("Sanity", 0.0F, 1.0F, 1.0F);
    public static final CharacterStat SICKNESS = register("Sickness", 0.0F, 1.0F, 0.0F);
    public static final CharacterStat STRESS = register("Stress", 0.0F, 1.0F, 0.0F);
    public static final CharacterStat TEMPERATURE = register("Temperature", 20.0F, 40.0F, 37.0F);
    public static final CharacterStat THIRST = register("Thirst", 0.0F, 1.0F, 0.0F);
    public static final CharacterStat UNHAPPINESS = register("Unhappiness", 0.0F, 100.0F, 0.0F);
    public static final CharacterStat WETNESS = register("Wetness", 0.0F, 100.0F, 0.0F);
    public static final CharacterStat ZOMBIE_FEVER = register("ZombieFever", 0.0F, 100.0F, 0.0F);
    public static final CharacterStat ZOMBIE_INFECTION = register("ZombieInfection", 0.0F, 100.0F, 0.0F);
    public static final CharacterStat[] ORDERED_STATS = new CharacterStat[]{
        ANGER,
        BOREDOM,
        DISCOMFORT,
        ENDURANCE,
        FATIGUE,
        FITNESS,
        FOOD_SICKNESS,
        HUNGER,
        IDLENESS,
        INTOXICATION,
        MORALE,
        NICOTINE_WITHDRAWAL,
        PAIN,
        PANIC,
        POISON,
        SANITY,
        SICKNESS,
        STRESS,
        TEMPERATURE,
        THIRST,
        UNHAPPINESS,
        WETNESS,
        ZOMBIE_FEVER,
        ZOMBIE_INFECTION
    };
    private final String id;
    private final float minimumValue;
    private final float maximumValue;
    private final float defaultValue;

    private CharacterStat(String id, float minimumValue, float maximumValue, float defaultValue) {
        this.id = id;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
        this.defaultValue = defaultValue;
    }

    public static CharacterStat register(String id, float minimumValue, float maximumValue, float defaultValue) {
        return REGISTRY.computeIfAbsent(id, key -> new CharacterStat(key, minimumValue, maximumValue, defaultValue));
    }

    public static CharacterStat getById(String id) {
        return REGISTRY.get(id);
    }

    public String getId() {
        return this.id;
    }

    public float getMinimumValue() {
        return this.minimumValue;
    }

    public float getMaximumValue() {
        return this.maximumValue;
    }

    public float clamp(float value) {
        return PZMath.clamp(value, this.minimumValue, this.maximumValue);
    }

    public float getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isAtMinimum(float value) {
        return value <= this.minimumValue;
    }

    public boolean isAtMaximum(float value) {
        return value >= this.maximumValue;
    }

    @Override
    public String toString() {
        return "CharacterStat{id='" + this.id + "', min=" + this.minimumValue + ", max=" + this.maximumValue + ", default=" + this.defaultValue + "}";
    }
}
