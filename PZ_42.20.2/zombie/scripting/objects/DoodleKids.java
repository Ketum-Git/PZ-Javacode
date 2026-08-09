// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class DoodleKids {
    public static final DoodleKids A_CARTOON_CHARACTER = registerBase("aCartoonCharacter");
    public static final DoodleKids A_FAMILY_IN_A_CAR = registerBase("aFamilyinaCar");
    public static final DoodleKids A_FAMILY_IN_A_GARDEN = registerBase("aFamilyinaGarden");
    public static final DoodleKids A_FAMILY_ON_THE_BEACH = registerBase("aFamilyontheBeach");
    public static final DoodleKids A_FOREST = registerBase("aForest");
    public static final DoodleKids A_FRIENDLY_ALIEN = registerBase("aFriendlyAlien");
    public static final DoodleKids A_FRIENDLY_CREATURE = registerBase("aFriendlyCreature");
    public static final DoodleKids A_GARDEN = registerBase("aGarden");
    public static final DoodleKids A_HOUSE_WITH_A_FAMILY = registerBase("aHousewithaFamily");
    public static final DoodleKids A_KITTEN = registerBase("aKitten");
    public static final DoodleKids A_MAP = registerBase("aMap");
    public static final DoodleKids A_NICE_DAY = registerBase("aNiceDay");
    public static final DoodleKids A_NICE_TEACHER = registerBase("aNiceTeacher");
    public static final DoodleKids A_PATTERN = registerBase("aPattern");
    public static final DoodleKids A_PUPPY = registerBase("aPuppy");
    public static final DoodleKids A_RAINBOW = registerBase("aRainbow");
    public static final DoodleKids A_SCARY_ALIEN = registerBase("aScaryAlien");
    public static final DoodleKids A_SCARY_MONSTER = registerBase("aScaryMonster");
    public static final DoodleKids A_SMILING_FAMILY = registerBase("aSmilingFamily");
    public static final DoodleKids A_SMILING_SUN = registerBase("aSmilingSun");
    public static final DoodleKids A_STICK_FIGURE = registerBase("aStickFigure");
    public static final DoodleKids A_TREE = registerBase("aTree");
    public static final DoodleKids AN_ANGRY_TEACHER = registerBase("anAngryTeacher");
    public static final DoodleKids AN_ODDLY_COLORED_SCENE = registerBase("anOddlyColoredScene");
    public static final DoodleKids AN_ODDLY_PROPORTIONED_PERSON = registerBase("anOddlyProportionedPerson");
    public static final DoodleKids BUTTERFLIES = registerBase("Butterflies");
    public static final DoodleKids CHILDREN_PLAYING = registerBase("ChildrenPlaying");
    public static final DoodleKids CHRISTMAS = registerBase("Christmas");
    public static final DoodleKids DOODLE_OF_MAGICAL_WOODLAND = registerBase("DoodleofMagicalWoodland");
    public static final DoodleKids DOODLE_OF_SPIFFO = registerBase("DoodleofSpiffo");
    public static final DoodleKids FIREWORKS = registerBase("Fireworks");
    public static final DoodleKids FLOWERS = registerBase("Flowers");
    public static final DoodleKids GRANDPARENTS = registerBase("Grandparents");
    public static final DoodleKids HEARTS = registerBase("Hearts");
    public static final DoodleKids INSECTS = registerBase("Insects");
    public static final DoodleKids PARENTS = registerBase("Parents");
    public static final DoodleKids PEOPLE_CRYING = registerBase("PeopleCrying");
    public static final DoodleKids PEOPLE_SMILING = registerBase("PeopleSmiling");
    public static final DoodleKids PEOPLE_WALKING = registerBase("PeopleWalking");
    public static final DoodleKids RANDOM_COLORS = registerBase("RandomColors");
    public static final DoodleKids RANDOM_CRAYON_LINES = registerBase("RandomCrayonLines");
    public static final DoodleKids RANDOM_LINES = registerBase("RandomLines");
    public static final DoodleKids RANDOM_MARKER_LINES = registerBase("RandomMarkerLines");
    public static final DoodleKids SOMEONE_CRYING = registerBase("SomeoneCrying");
    public static final DoodleKids SOMETHING_INDISCERNIBLE = registerBase("SomethingIndiscernible");
    public static final DoodleKids SQUIGGLY_LINES = registerBase("SquigglyLines");
    public static final DoodleKids STICK_FIGURES = registerBase("StickFigures");
    private final String translationKey;

    private DoodleKids(String id) {
        this.translationKey = "IGUI_Photo_" + id;
    }

    public static DoodleKids get(ResourceLocation id) {
        return Registries.DOODLE_KIDS.get(id);
    }

    @Override
    public String toString() {
        return Registries.DOODLE_KIDS.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static DoodleKids register(String id) {
        return register(false, id);
    }

    private static DoodleKids registerBase(String id) {
        return register(true, id);
    }

    private static DoodleKids register(boolean allowDefaultNamespace, String id) {
        return Registries.DOODLE_KIDS.register(RegistryReset.createLocation(id, allowDefaultNamespace), new DoodleKids(id));
    }

    static {
        if (Core.IS_DEV) {
            for (DoodleKids doodleKids : Registries.DOODLE_KIDS) {
                TranslationKeyValidator.of(doodleKids.translationKey);
            }
        }
    }
}
