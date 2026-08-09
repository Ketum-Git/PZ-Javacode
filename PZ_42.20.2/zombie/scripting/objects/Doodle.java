// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class Doodle {
    public static final Doodle A_BATTLE = registerBase("aBattle");
    public static final Doodle A_CARTOON = registerBase("aCartoon");
    public static final Doodle A_CARTOON_CHARACTER = registerBase("aCartoonCharacter");
    public static final Doodle A_COUPLE = registerBase("aCouple");
    public static final Doodle A_CUTE_ANIMAL = registerBase("aCuteAnimal");
    public static final Doodle A_DEAD_STICK_FIGURE = registerBase("aDeadStickFigure");
    public static final Doodle A_FACE = registerBase("aFace");
    public static final Doodle A_FAMOUS_PERSON = registerBase("aFamousPerson");
    public static final Doodle A_FUNNY_SCENE = registerBase("aFunnyScene");
    public static final Doodle A_GARDEN = registerBase("aGarden");
    public static final Doodle A_HANDSOME_MAN = registerBase("aHandsomeMan");
    public static final Doodle A_HOUSE = registerBase("aHouse");
    public static final Doodle A_LANDMARK = registerBase("aLandmark");
    public static final Doodle A_LOGO = registerBase("aLogo");
    public static final Doodle A_MAN = registerBase("aMan");
    public static final Doodle A_MANS_FACE = registerBase("aMan'sFace");
    public static final Doodle A_MONSTER = registerBase("aMonster");
    public static final Doodle A_MOVIE_CHARACTER = registerBase("aMovieCharacter");
    public static final Doodle A_NATURE_SCENE = registerBase("aNatureScene");
    public static final Doodle A_PATTERN = registerBase("aPattern");
    public static final Doodle A_PERSON = registerBase("aPerson");
    public static final Doodle A_PET = registerBase("aPet");
    public static final Doodle A_RELIGIOUS_SCENE = registerBase("aReligiousScene");
    public static final Doodle A_SPACE_SCENE = registerBase("aSpaceScene");
    public static final Doodle A_STICK_FIGURE = registerBase("aStickFigure");
    public static final Doodle A_SURREAL_SCENE = registerBase("aSurrealScene");
    public static final Doodle A_VIOLENT_SCENE = registerBase("aViolentScene");
    public static final Doodle A_WEIRD_FACE = registerBase("aWeirdFace");
    public static final Doodle A_WILD_ANIMAL = registerBase("aWildAnimal");
    public static final Doodle A_WOMAN = registerBase("aWoman");
    public static final Doodle A_WOMANS_FACE = registerBase("aWoman'sFace");
    public static final Doodle AN_ALIEN = registerBase("anAlien");
    public static final Doodle AN_ANGRY_MAN = registerBase("anAngryMan");
    public static final Doodle AN_ANGRY_PERSON = registerBase("anAngryPerson");
    public static final Doodle AN_ANGRY_WOMAN = registerBase("anAngryWoman");
    public static final Doodle AN_ATTRACTIVE_LADY = registerBase("anAttractiveLady");
    public static final Doodle AN_EXOTIC_ANIMAL = registerBase("anExoticAnimal");
    public static final Doodle BUILDINGS = registerBase("Buildings");
    public static final Doodle CLOUDS = registerBase("Clouds");
    public static final Doodle DEAD_STICK_FIGURES = registerBase("DeadStickFigures");
    public static final Doodle FLOWERS = registerBase("Flowers");
    public static final Doodle FOOD = registerBase("Food");
    public static final Doodle FUNNY_CHARACTERS = registerBase("FunnyCharacters");
    public static final Doodle HEARTS = registerBase("Hearts");
    public static final Doodle NOTHING_MUCH = registerBase("NothingMuch");
    public static final Doodle NUMBERS = registerBase("Numbers");
    public static final Doodle RANDOM_LINES = registerBase("RandomLines");
    public static final Doodle RANDOM_SHAPES = registerBase("RandomShapes");
    public static final Doodle SHAPES = registerBase("Shapes");
    public static final Doodle SOMETHING_CRUDE = registerBase("SomethingCrude");
    public static final Doodle SQUIGGLY_LINES = registerBase("SquigglyLines");
    public static final Doodle STICK_FIGURES = registerBase("StickFigures");
    public static final Doodle STICK_FIGURES_FIGHTING = registerBase("StickFiguresFighting");
    public static final Doodle SYMBOLS = registerBase("Symbols");
    public static final Doodle TEXT = registerBase("Text");
    public static final Doodle WEIRD_FACES = registerBase("WeirdFaces");
    private final String translationKey;

    private Doodle(String id) {
        this.translationKey = "IGUI_Photo_" + id;
    }

    public static Doodle get(ResourceLocation id) {
        return Registries.DOODLE.get(id);
    }

    @Override
    public String toString() {
        return Registries.DOODLE.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Doodle register(String id) {
        return register(false, id);
    }

    private static Doodle registerBase(String id) {
        return register(true, id);
    }

    private static Doodle register(boolean allowDefaultNamespace, String id) {
        return Registries.DOODLE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Doodle(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Doodle doodle : Registries.DOODLE) {
                TranslationKeyValidator.of(doodle.translationKey);
            }
        }
    }
}
