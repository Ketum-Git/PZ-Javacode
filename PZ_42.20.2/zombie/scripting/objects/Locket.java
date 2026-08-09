// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class Locket {
    public static final Locket A_BABY = registerBase("aBaby");
    public static final Locket A_BIRTHDAY_PARTY = registerBase("aBirthdayParty");
    public static final Locket A_BOY = registerBase("aBoy");
    public static final Locket A_CHILD = registerBase("aChild");
    public static final Locket A_COUPLE = registerBase("aCouple");
    public static final Locket A_DECEASED_LOVED_ONE = registerBase("aDeceasedLovedOne");
    public static final Locket A_FAMILY = registerBase("aFamily");
    public static final Locket A_FAMILY_WITH_A_BABY = registerBase("aFamilywithaBaby");
    public static final Locket A_FAMILY_WITH_A_PET = registerBase("aFamilywithaPet");
    public static final Locket A_FAMILY_WITH_CHILDREN = registerBase("aFamilywithChildren");
    public static final Locket A_FAMOUS_PERSON = registerBase("aFamousPerson");
    public static final Locket A_FATHER_AND_CHILDREN = registerBase("aFatherandChildren");
    public static final Locket A_FATHER_AND_DAUGHTER = registerBase("aFatherandDaughter");
    public static final Locket A_FATHER_AND_SON = registerBase("aFatherandSon");
    public static final Locket A_GIRL = registerBase("aGirl");
    public static final Locket A_HAPPY_FAMILY = registerBase("aHappyFamily");
    public static final Locket A_LARGE_FAMILY = registerBase("aLargeFamily");
    public static final Locket A_LOVED_ONE = registerBase("aLovedOne");
    public static final Locket A_MAN_WITH_A_BABY = registerBase("aManwithaBaby");
    public static final Locket A_MARRIED_COUPLE = registerBase("aMarriedCouple");
    public static final Locket A_MOTHER_AND_CHILDREN = registerBase("aMotherandChildren");
    public static final Locket A_MOTHER_AND_SON = registerBase("aMotherandSon");
    public static final Locket A_PET = registerBase("aPet");
    public static final Locket A_RELIGIOUS_FIGURE = registerBase("aReligiousFigure");
    public static final Locket A_SMILING_COUPLE = registerBase("aSmilingCouple");
    public static final Locket A_SMILING_MAN = registerBase("aSmilingMan");
    public static final Locket A_SMILING_WOMAN = registerBase("aSmilingWoman");
    public static final Locket A_SOLDIER = registerBase("aSoldier");
    public static final Locket A_WEDDING = registerBase("aWedding");
    public static final Locket A_WOMAN_WITH_A_BABY = registerBase("aWomanwithaBaby");
    public static final Locket A_YOUNG_COUPLE = registerBase("aYoungCouple");
    public static final Locket A_YOUNG_MAN = registerBase("aYoungMan");
    public static final Locket A_YOUNG_WOMAN = registerBase("aYoungWoman");
    public static final Locket AN_OLD_COUPLE = registerBase("anOldCouple");
    public static final Locket AN_OLD_MAN = registerBase("anOldMan");
    public static final Locket AN_OLD_WOMAN = registerBase("anOldWoman");
    public static final Locket CHILDREN = registerBase("Children");
    public static final Locket CHILDREN_AND_A_BABY = registerBase("ChildrenandaBaby");
    public static final Locket CHILDREN_AND_BABIES = registerBase("ChildrenandBabies");
    public static final Locket GRANDPARENTS_AND_GRANDCHILDREN = registerBase("GrandparentsandGrandchildren");
    public static final Locket PARENTS_WITH_A_DAUGHTER = registerBase("ParentswithaDaughter");
    public static final Locket PARENTS_WITH_A_SON = registerBase("ParentswithaSon");
    public static final Locket PARENTS_WITH_TEENAGERS = registerBase("ParentswithTeenagers");
    public static final Locket PARENTS_WITH_YOUNG_CHILDREN = registerBase("ParentswithYoungChildren");
    public static final Locket SOMETHING_TOO_FADED_TO_MAKE_OUT = registerBase("SomethingTooFadedtoMakeOut");
    private final String translationKey;

    private Locket(String id) {
        this.translationKey = "IGUI_Photo_" + id;
    }

    public static Locket get(ResourceLocation id) {
        return Registries.LOCKET.get(id);
    }

    @Override
    public String toString() {
        return Registries.LOCKET.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Locket register(String id) {
        return register(false, id);
    }

    private static Locket registerBase(String id) {
        return register(true, id);
    }

    private static Locket register(boolean allowDefaultNamespace, String id) {
        return Registries.LOCKET.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Locket(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Locket locket : Registries.LOCKET) {
                TranslationKeyValidator.of(locket.translationKey);
            }
        }
    }
}
