// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class OldNewspaper {
    public static final OldNewspaper BOWLING_GREEN_POST = registerBase("BowlingGreenPost");
    public static final OldNewspaper BRANDENBURG_BUGLE = registerBase("BrandenburgBugle");
    public static final OldNewspaper CHRISTIAN_BULLETIN = registerBase("ChristianBulletin");
    public static final OldNewspaper EVANSVILLE_POST = registerBase("EvansvillePost");
    public static final OldNewspaper KENTUCKY_HERALD = registerBase("KentuckyHerald");
    public static final OldNewspaper KENTUCKY_OBSERVER = registerBase("KentuckyObserver");
    public static final OldNewspaper KNOX_FRONTLINE = registerBase("KnoxFrontline");
    public static final OldNewspaper KNOX_KNEWS = registerBase("KnoxKnews");
    public static final OldNewspaper LOUISVILLE_STUDENT = registerBase("LouisvilleStudent");
    public static final OldNewspaper LOUISVILLE_SUN = registerBase("LouisvilleSun");
    public static final OldNewspaper LOUISVILLE_SUN_TIMES = registerBase("LouisvilleSunTimes");
    public static final OldNewspaper MULDRAUGH_MESSENGER = registerBase("MuldraughMessenger");
    public static final OldNewspaper NATIONAL_DISPATCH = registerBase("NationalDispatch");
    public static final OldNewspaper NATIONAL_FINANCE = registerBase("NationalFinance");
    public static final OldNewspaper OWENSBORO_OUTSIDER = registerBase("OwensboroOutsider");
    public static final OldNewspaper PADUCAH_POST = registerBase("PaducahPost");
    public static final OldNewspaper THE_CINCINNATI_TIMES = registerBase("TheCincinnatiTimes");
    public static final OldNewspaper THE_KENTUCKY_DEFENDER = registerBase("TheKentuckyDefender");
    public static final OldNewspaper THE_LEXINGTON_VOICE = registerBase("TheLexingtonVoice");
    public static final OldNewspaper THE_LONDON_POST = registerBase("TheLondonPost");
    public static final OldNewspaper WALL_STREET_INSIDER = registerBase("WallStreetInsider");
    public static final OldNewspaper WASHINGTON_HERALD = registerBase("WashingtonHerald");
    private final String translationKey;

    private OldNewspaper(String id) {
        this.translationKey = "IGUI_NewspaperTitle_" + id;
    }

    public static OldNewspaper get(ResourceLocation id) {
        return Registries.OLD_NEWSPAPER.get(id);
    }

    @Override
    public String toString() {
        return Registries.OLD_NEWSPAPER.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static OldNewspaper register(String id) {
        return register(false, id);
    }

    private static OldNewspaper registerBase(String id) {
        return register(true, id);
    }

    private static OldNewspaper register(boolean allowDefaultNamespace, String id) {
        return Registries.OLD_NEWSPAPER.register(RegistryReset.createLocation(id, allowDefaultNamespace), new OldNewspaper(id));
    }

    static {
        if (Core.IS_DEV) {
            for (OldNewspaper newspaper : Registries.OLD_NEWSPAPER) {
                TranslationKeyValidator.of(newspaper.getTranslationKey());
            }
        }
    }
}
