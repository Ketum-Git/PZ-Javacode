// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class Letter {
    public static final Letter ACCEPTANCE_LETTER = registerBase("AcceptanceLetter");
    public static final Letter APPLICATION_LETTER = registerBase("ApplicationLetter");
    public static final Letter BANK_LETTER = registerBase("BankLetter");
    public static final Letter BILL = registerBase("Bill");
    public static final Letter BUSINESS_LETTER = registerBase("BusinessLetter");
    public static final Letter CHARITY_LETTER = registerBase("CharityLetter");
    public static final Letter CHILDS_LETTER = registerBase("ChildsLetter");
    public static final Letter CONDOLENCE_LETTER = registerBase("CondolenceLetter");
    public static final Letter EMPLOYMENT_LETTER = registerBase("EmploymentLetter");
    public static final Letter FRIENDLY_LETTER = registerBase("FriendlyLetter");
    public static final Letter GOVERNMENT_LETTER = registerBase("GovernmentLetter");
    public static final Letter INVITATION_LETTER = registerBase("InvitationLetter");
    public static final Letter LEGAL_LETTER = registerBase("LegalLetter");
    public static final Letter LETTER = registerBase("Letter");
    public static final Letter OFFICIAL_LETTER = registerBase("OfficialLetter");
    public static final Letter OVERDUE_BILL = registerBase("OverdueBill");
    public static final Letter REJECTION_LETTER = registerBase("RejectionLetter");
    public static final Letter RESIGNATION_LETTER = registerBase("ResignationLetter");
    public static final Letter ROMANTIC_LETTER = registerBase("RomanticLetter");
    public static final Letter RUDE_LETTER = registerBase("RudeLetter");
    public static final Letter SAD_LETTER = registerBase("SadLetter");
    public static final Letter SCAM_LETTER = registerBase("ScamLetter");
    public static final Letter THANK_YOU_LETTER = registerBase("ThankYouLetter");
    public static final Letter THREATENING_LETTER = registerBase("ThreateningLetter");
    private final String translationKey;

    private Letter(String id) {
        this.translationKey = "IGUI_" + id;
    }

    public static Letter get(ResourceLocation id) {
        return Registries.LETTER.get(id);
    }

    @Override
    public String toString() {
        return Registries.LETTER.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Letter register(String id) {
        return register(false, id);
    }

    private static Letter registerBase(String id) {
        return register(true, id);
    }

    private static Letter register(boolean allowDefaultNamespace, String id) {
        return Registries.LETTER.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Letter(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Letter letter : Registries.LETTER) {
                TranslationKeyValidator.of(letter.translationKey);
            }
        }
    }
}
