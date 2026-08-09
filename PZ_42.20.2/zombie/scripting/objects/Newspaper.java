// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import java.util.List;
import zombie.UsedFromLua;
import zombie.core.Core;

@UsedFromLua
public class Newspaper {
    public static final Newspaper KENTUCKY_HERALD = registerBase(
        "KentuckyHerald", List.of("KentuckyHerald_July6", "KentuckyHerald_July13", "KentuckyHerald_July14", "KentuckyHerald_July15", "KentuckyHerald_July16")
    );
    public static final Newspaper KNOX_KNEWS = registerBase(
        "KnoxKnews", List.of("KnoxKnews_July1", "KnoxKnews_July2", "KnoxKnews_July3", "KnoxKnews_July4", "KnoxKnews_July5", "KnoxKnews_July6")
    );
    public static final Newspaper LOUISVILLE_SUN_TIMES = registerBase(
        "LouisvilleSunTimes",
        List.of("LouisvilleSunTimes_July6", "LouisvilleSunTimes_July13", "LouisvilleSunTimes_July14", "LouisvilleSunTimes_July15", "LouisvilleSunTimes_July16")
    );
    public static final Newspaper NATIONAL_DISPATCH = registerBase(
        "NationalDispatch",
        List.of(
            "NationalDispatch_July6",
            "NationalDispatch_July7",
            "NationalDispatch_July12",
            "NationalDispatch_July13",
            "NationalDispatch_July14",
            "NationalDispatch_July15"
        )
    );
    private final String translationKey;
    private final List<String> issues;

    private Newspaper(String id, List<String> issues) {
        this.translationKey = "IGUI_NewspaperTitle_" + id;
        this.issues = issues;
    }

    public static Newspaper get(ResourceLocation id) {
        return Registries.NEWSPAPER.get(id);
    }

    @Override
    public String toString() {
        return Registries.NEWSPAPER.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public List<String> getIssues() {
        return this.issues;
    }

    public String getTitle(String title) {
        return "Print_Media_" + title + "_title";
    }

    public String getTranslationInfoKey(String issue) {
        return "Print_Media_" + issue + "_info";
    }

    public String getTranslationTextKey(String issue) {
        return "Print_Text_" + issue + "_info";
    }

    public static Newspaper register(String id, List<String> issues) {
        return register(false, id, new Newspaper(id, issues));
    }

    private static Newspaper registerBase(String id, List<String> issues) {
        return register(true, id, new Newspaper(id, issues));
    }

    private static Newspaper register(boolean allowDefaultNamespace, String id, Newspaper t) {
        return Registries.NEWSPAPER.register(RegistryReset.createLocation(id, allowDefaultNamespace), t);
    }

    static {
        if (Core.IS_DEV) {
            for (Newspaper newspaper : Registries.NEWSPAPER) {
                TranslationKeyValidator.of(newspaper.getTranslationKey());

                for (String issue : newspaper.getIssues()) {
                    TranslationKeyValidator.of(newspaper.getTitle(issue));
                    TranslationKeyValidator.of(newspaper.getTranslationInfoKey(issue));
                    TranslationKeyValidator.of(newspaper.getTranslationTextKey(issue));
                }
            }
        }
    }
}
