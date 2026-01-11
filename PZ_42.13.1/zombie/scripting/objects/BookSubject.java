// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public class BookSubject {
    public static final BookSubject ADVENTURE_NON_FICTION = registerBase("adventure_non_fiction");
    public static final BookSubject ART = registerBase("art");
    public static final BookSubject BASEBALL = registerBase("baseball");
    public static final BookSubject BIBLE = registerBase("bible");
    public static final BookSubject BIOGRAPHY = registerBase("biography");
    public static final BookSubject BUSINESS = registerBase("business");
    public static final BookSubject CHILDS = registerBase("childs");
    public static final BookSubject CHILDS_PICTURE_SPECIAL = registerBase("childs_picture_special");
    public static final BookSubject CINEMA = registerBase("cinema");
    public static final BookSubject CLASSIC = registerBase("classic");
    public static final BookSubject CLASSIC_FICTION = registerBase("classic_fiction");
    public static final BookSubject CLASSIC_NONFICTION = registerBase("classic_nonfiction");
    public static final BookSubject COMPUTER = registerBase("computer");
    public static final BookSubject CONSPIRACY = registerBase("conspiracy");
    public static final BookSubject CRIME_FICTION = registerBase("crime_fiction");
    public static final BookSubject DIET = registerBase("diet");
    public static final BookSubject FANTASY = registerBase("fantasy");
    public static final BookSubject FARMING = registerBase("farming");
    public static final BookSubject FASHION = registerBase("fashion");
    public static final BookSubject GENERAL_FICTION = registerBase("general_fiction");
    public static final BookSubject GENERAL_REFERENCE = registerBase("general_reference");
    public static final BookSubject GOLF = registerBase("golf");
    public static final BookSubject HASS = registerBase("hass");
    public static final BookSubject HISTORY = registerBase("history");
    public static final BookSubject HORROR = registerBase("horror");
    public static final BookSubject LEGAL = registerBase("legal");
    public static final BookSubject MEDICAL = registerBase("medical");
    public static final BookSubject MILITARY = registerBase("military");
    public static final BookSubject MILITARY_HISTORY = registerBase("military_history");
    public static final BookSubject MUSIC = registerBase("music");
    public static final BookSubject NATURE = registerBase("nature");
    public static final BookSubject NEW_AGE = registerBase("new_age");
    public static final BookSubject OCCULT = registerBase("occult");
    public static final BookSubject PHILOSOPHY = registerBase("philosophy");
    public static final BookSubject PHOTO_SPECIAL = registerBase("photo_special");
    public static final BookSubject PLAY = registerBase("play");
    public static final BookSubject POLICING = registerBase("policing");
    public static final BookSubject POLITICS = registerBase("politics");
    public static final BookSubject QUACKERY = registerBase("quackery");
    public static final BookSubject QUIGLEY = registerBase("quigley");
    public static final BookSubject RELATIONSHIP = registerBase("relationship");
    public static final BookSubject RELIGION = registerBase("religion");
    public static final BookSubject ROMANCE = registerBase("romance");
    public static final BookSubject SAD_NON_FICTION = registerBase("sad_non_fiction");
    public static final BookSubject SCHOOL_TEXTBOOK = registerBase("school_textbook");
    public static final BookSubject SCIENCE = registerBase("science");
    public static final BookSubject SCIFI = registerBase("scifi");
    public static final BookSubject SELF_HELP = registerBase("self_help");
    public static final BookSubject SEXY = registerBase("sexy");
    public static final BookSubject SPORTS = registerBase("sports");
    public static final BookSubject TEENS = registerBase("teens");
    public static final BookSubject THRILLER = registerBase("thriller");
    public static final BookSubject TRAVEL = registerBase("travel");
    public static final BookSubject TRUE_CRIME = registerBase("true_crime");
    public static final BookSubject WESTERN = registerBase("western");

    private BookSubject() {
    }

    public static BookSubject get(ResourceLocation id) {
        return Registries.BOOK_SUBJECT.get(id);
    }

    @Override
    public String toString() {
        return Registries.BOOK_SUBJECT.getLocation(this).toString();
    }

    public static BookSubject register(String id) {
        return register(false, id);
    }

    private static BookSubject registerBase(String id) {
        return register(true, id);
    }

    private static BookSubject register(boolean allowDefaultNamespace, String id) {
        return Registries.BOOK_SUBJECT.register(RegistryReset.createLocation(id, allowDefaultNamespace), new BookSubject());
    }
}
