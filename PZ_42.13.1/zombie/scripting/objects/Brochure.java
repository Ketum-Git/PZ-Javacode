// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.UsedFromLua;
import zombie.core.Core;

@UsedFromLua
public class Brochure {
    public static final Brochure AIRPORT = registerBase("Airport");
    public static final Brochure ART_GALLERY_OF_LOUISVILLE = registerBase("ArtGalleryofLouisville");
    public static final Brochure CARDINAL_PLAZA = registerBase("CardinalPlaza");
    public static final Brochure COALFIELD = registerBase("Coalfield");
    public static final Brochure COLD_WAR_BUNKER = registerBase("ColdWarBunker");
    public static final Brochure CROSS_ROADS_MALL = registerBase("CrossRoadsMall");
    public static final Brochure DARKWALLOW_GUEST_HOUSE = registerBase("DarkwallowGuestHouse");
    public static final Brochure DINER_IN_THE_WOODS = registerBase("DinerInTheWoods");
    public static final Brochure FOSSOIL_FIELD = registerBase("FossoilField");
    public static final Brochure GRAND_OHIO_MALL = registerBase("GrandOhioMall");
    public static final Brochure HAVISHAM_SUITES = registerBase("HavishamSuites");
    public static final Brochure IRVINGTON_SPEEDWAY = registerBase("IrvingtonSpeedway");
    public static final Brochure LSU = registerBase("LSU");
    public static final Brochure PONDVIEW = registerBase("Pondview");
    public static final Brochure QUILL_MANOR = registerBase("QuillManor");
    public static final Brochure SANATORIUM = registerBase("Sanatorium");
    public static final Brochure SCARLET_OAK_DISTILLERY = registerBase("ScarletOakDistillery");
    public static final Brochure SLEEP_EAZZZE_INN = registerBase("SleepEazzzeInn");
    public static final Brochure SUNSTAR_MOTEL = registerBase("SunstarMotel");
    public static final Brochure WELLINGTON_HEIGHTS_GOLF_CLUB = registerBase("WellingtonHeightsGolfClub");
    public static final Brochure WEST_MAPLE_COUNTRY_CLUB = registerBase("WestMapleCountryClub");
    public static final Brochure DELILAH = registerBase("Delilah");
    private final String translationKey;
    private final String translationInfoKey;
    private final String translationTextKey;

    private Brochure(String id) {
        this.translationKey = "Print_Media_" + id + "_title";
        this.translationInfoKey = "Print_Media_" + id + "_info";
        this.translationTextKey = "Print_Text_" + id + "_info";
    }

    @Override
    public String toString() {
        return Registries.BROCHURE.getLocation(this).getPath();
    }

    public static Brochure get(ResourceLocation id) {
        return Registries.BROCHURE.get(id);
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public String getTranslationInfoKey() {
        return this.translationInfoKey;
    }

    public String getTranslationTextKey() {
        return this.translationTextKey;
    }

    public static Brochure register(String id) {
        return register(false, id);
    }

    private static Brochure registerBase(String id) {
        return register(true, id);
    }

    private static Brochure register(boolean allowDefaultNamespace, String id) {
        return Registries.BROCHURE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Brochure(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Brochure brochure : Registries.BROCHURE) {
                TranslationKeyValidator.of(brochure.getTranslationKey());
                TranslationKeyValidator.of(brochure.getTranslationInfoKey());
                TranslationKeyValidator.of(brochure.getTranslationTextKey());
            }
        }
    }
}
