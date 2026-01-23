// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.UsedFromLua;

@UsedFromLua
public class MoodleType {
    public static final MoodleType ENDURANCE = registerBase("Endurance");
    public static final MoodleType TIRED = registerBase("Tired");
    public static final MoodleType HUNGRY = registerBase("Hungry");
    public static final MoodleType PANIC = registerBase("Panic");
    public static final MoodleType SICK = registerBase("Sick");
    public static final MoodleType BORED = registerBase("Bored");
    public static final MoodleType UNHAPPY = registerBase("Unhappy");
    public static final MoodleType BLEEDING = registerBase("Bleeding");
    public static final MoodleType WET = registerBase("Wet");
    public static final MoodleType HAS_A_COLD = registerBase("HasACold");
    public static final MoodleType ANGRY = registerBase("Angry");
    public static final MoodleType STRESS = registerBase("Stress");
    public static final MoodleType THIRST = registerBase("Thirst");
    public static final MoodleType INJURED = registerBase("Injured");
    public static final MoodleType PAIN = registerBase("Pain");
    public static final MoodleType HEAVY_LOAD = registerBase("HeavyLoad");
    public static final MoodleType DRUNK = registerBase("Drunk");
    public static final MoodleType DEAD = registerBase("Dead");
    public static final MoodleType ZOMBIE = registerBase("Zombie");
    public static final MoodleType HYPERTHERMIA = registerBase("Hyperthermia");
    public static final MoodleType HYPOTHERMIA = registerBase("Hypothermia");
    public static final MoodleType WINDCHILL = registerBase("Windchill");
    public static final MoodleType CANT_SPRINT = registerBase("CantSprint");
    public static final MoodleType UNCOMFORTABLE = registerBase("Uncomfortable");
    public static final MoodleType NOXIOUS_SMELL = registerBase("NoxiousSmell");
    public static final MoodleType FOOD_EATEN = registerBase("FoodEaten");
    private final String translationName;

    private MoodleType(String translationName) {
        this.translationName = translationName;
    }

    public static MoodleType get(ResourceLocation id) {
        return Registries.MOODLE_TYPE.get(id);
    }

    @Override
    public String toString() {
        return Registries.MOODLE_TYPE.getLocation(this).toString();
    }

    public String getTranslationName() {
        return this.translationName;
    }

    public static MoodleType register(String id) {
        return register(false, id);
    }

    private static MoodleType registerBase(String id) {
        return register(true, id);
    }

    private static MoodleType register(boolean allowDefaultNamespace, String id) {
        return Registries.MOODLE_TYPE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new MoodleType(id));
    }
}
