// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.UsedFromLua;

@UsedFromLua
public class CharacterProfession {
    public static final CharacterProfession BURGLAR = registerBase("burglar");
    public static final CharacterProfession BURGER_FLIPPER = registerBase("burgerflipper");
    public static final CharacterProfession CARPENTER = registerBase("carpenter");
    public static final CharacterProfession CHEF = registerBase("chef");
    public static final CharacterProfession CONSTRUCTION_WORKER = registerBase("constructionworker");
    public static final CharacterProfession DOCTOR = registerBase("doctor");
    public static final CharacterProfession ELECTRICIAN = registerBase("electrician");
    public static final CharacterProfession ENGINEER = registerBase("engineer");
    public static final CharacterProfession FARMER = registerBase("farmer");
    public static final CharacterProfession FIRE_OFFICER = registerBase("fireofficer");
    public static final CharacterProfession FISHERMAN = registerBase("fisherman");
    public static final CharacterProfession FITNESS_INSTRUCTOR = registerBase("fitnessInstructor");
    public static final CharacterProfession LUMBERJACK = registerBase("lumberjack");
    public static final CharacterProfession MECHANICS = registerBase("mechanics");
    public static final CharacterProfession METALWORKER = registerBase("metalworker");
    public static final CharacterProfession NURSE = registerBase("nurse");
    public static final CharacterProfession PARK_RANGER = registerBase("parkranger");
    public static final CharacterProfession POLICE_OFFICER = registerBase("policeofficer");
    public static final CharacterProfession RANCHER = registerBase("rancher");
    public static final CharacterProfession REPAIRMAN = registerBase("repairman");
    public static final CharacterProfession SECURITY_GUARD = registerBase("securityguard");
    public static final CharacterProfession SMITHER = registerBase("smither");
    public static final CharacterProfession TAILOR = registerBase("tailor");
    public static final CharacterProfession UNEMPLOYED = registerBase("unemployed");
    public static final CharacterProfession VETERAN = registerBase("veteran");

    private CharacterProfession() {
    }

    public static CharacterProfession get(ResourceLocation id) {
        return Registries.CHARACTER_PROFESSION.get(id);
    }

    public String getName() {
        return Registries.CHARACTER_PROFESSION.getLocation(this).getPath();
    }

    @Override
    public String toString() {
        return Registries.CHARACTER_PROFESSION.getLocation(this).toString();
    }

    public static CharacterProfession register(String id) {
        return register(false, id);
    }

    private static CharacterProfession registerBase(String id) {
        return register(true, id);
    }

    private static CharacterProfession register(boolean allowDefaultNamespace, String id) {
        return Registries.CHARACTER_PROFESSION.register(RegistryReset.createLocation(id, allowDefaultNamespace), new CharacterProfession());
    }
}
