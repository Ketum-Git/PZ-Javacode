// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class Business {
    public static final Business MC_COY_LOGGING = registerBase("McCoyLogging");
    public static final Business VALU_TECH = registerBase("ValuTech");
    public static final Business EGENEREX = registerBase("Egenerex");
    public static final Business UNITED_SHIPPING_LOGISTICS = registerBase("UnitedShippingLogistics");
    public static final Business PERFICK_POTATO_CO = registerBase("PerfickPotatoCo");
    public static final Business HERR_FLICK_KNIVES = registerBase("HerrFlickKnives");
    public static final Business COBBER_METALS = registerBase("CobberMetals");
    public static final Business BANSHEE_HOLLOWAY = registerBase("BansheeHolloway");
    public static final Business BERING_COMPANY = registerBase("BeringCompany");
    public static final Business YURI_DESIGN = registerBase("YuriDesign");
    public static final Business NEWCASTLE_PAPERAND_INK = registerBase("NewcastlePaperandInk");
    public static final Business BUSAN_TELECOMMUNICATIONS = registerBase("BusanTelecommunications");
    public static final Business KITTEN_KNIVES = registerBase("KittenKnives");
    public static final Business BUTTERFLY_MACHINERY = registerBase("ButterflyMachinery");
    public static final Business WIRKLICHLANGESWORT_AG = registerBase("WirklichlangeswortAG");
    public static final Business SANCHEZ_GOLDBERG = registerBase("SanchezGoldberg");
    public static final Business BEANZ = registerBase("Beanz");
    public static final Business BRUCEY_SOUPS = registerBase("BruceySoups");
    public static final Business FELLOWS_INC = registerBase("FellowsInc");
    public static final Business INVISIBLE_SLEDGEHAMMER_CORP = registerBase("InvisibleSledgehammerCorp");
    public static final Business PANTHER_MOTORS = registerBase("PantherMotors");
    public static final Business KILLIAN_FOODSTUFFS = registerBase("KillianFoodstuffs");
    public static final Business GRENNADE_CHEMICALS = registerBase("GrennadeChemicals");
    public static final Business REALLY_HARD_STEEL = registerBase("ReallyHardSteel");
    public static final Business CHINESE_PETROLEUM = registerBase("ChinesePetroleum");
    public static final Business BANKOF_KENTUCKY = registerBase("BankofKentucky");
    public static final Business LOVEHEART_SHIPBUILDING = registerBase("LoveheartShipbuilding");
    public static final Business DOUBLE_ENTRY_ACCOUNTING = registerBase("DoubleEntryAccounting");
    public static final Business SWIFT_THOMPSON_AEROSPACE = registerBase("SwiftThompsonAerospace");
    public static final Business FUN_XTREME_INC = registerBase("FunXtremeInc");
    public static final Business IMEKAGI = registerBase("Imekagi");
    public static final Business WOLFRAM_WAFFEN = registerBase("WolframWaffen");
    public static final Business FOSSOIL = registerBase("Fossoil");
    public static final Business SPIFFO_CORP = registerBase("SpiffoCorp");
    public static final Business GIGA_MART = registerBase("GigaMart");
    public static final Business KIRRUS_INC = registerBase("KirrusInc");
    public static final Business FRANKLIN_MOTORS = registerBase("FranklinMotors");
    public static final Business GLOBAL_COMPUTER_SOLUTIONS = registerBase("GlobalComputerSolutions");
    public static final Business PARASOL_INC = registerBase("ParasolInc");
    public static final Business TISCONSTRUCTION = registerBase("TISConstruction");
    public static final Business PREMIUM_TECHNOLOGIES = registerBase("PremiumTechnologies");
    public static final Business MMM_INC = registerBase("MmmInc");
    public static final Business ALGOL_ELECTRONICS = registerBase("AlgolElectronics");
    public static final Business FIBROIL = registerBase("Fibroil");
    public static final Business SEAHORSE_COFFEE_CORP = registerBase("SeahorseCoffeeCorp");
    public static final Business HAWTHORN_OIL = registerBase("HawthornOil");
    public static final Business POP_CO = registerBase("PopCo");
    public static final Business CHRYSALIS = registerBase("Chrysalis");
    public static final Business NIKODA = registerBase("Nikoda");
    public static final Business VALU_INSURANCE = registerBase("ValuInsurance");
    public static final Business ZIPPEE = registerBase("Zippee");
    public static final Business PHARMAHUG = registerBase("Pharmahug");
    public static final Business SPECIFIC_ELECTRIC = registerBase("SpecificElectric");
    public static final Business HALLOWAY_FRAMER = registerBase("HallowayFramer");
    public static final Business REDMOND_REDMOND = registerBase("RedmondRedmond");
    public static final Business HAVISHAM_HOTELS = registerBase("HavishamHotels");
    public static final Business AMERICAN_TIRE = registerBase("AmericanTire");
    public static final Business AMERI_GLOBE_INC = registerBase("AmeriGlobeInc");
    public static final Business MASS_GENFAC_CO = registerBase("MassGenfacCo");
    public static final Business FINNEGAN_GROUP = registerBase("FinneganGroup");
    public static final Business PALM_TRAVEL = registerBase("PalmTravel");
    public static final Business GENERAL_BROADCAST_CORPORATION = registerBase("GeneralBroadcastCorporation");
    public static final Business SCITT_WILKER_FIREARMS = registerBase("ScittWilkerFirearms");
    private final String translation;

    private Business(String id) {
        this.translation = "IGUI_" + id;
    }

    public static Business get(ResourceLocation id) {
        return Registries.BUSINESS.get(id);
    }

    @Override
    public String toString() {
        return Registries.BUSINESS.getLocation(this).getPath();
    }

    public String getTranslation() {
        return this.translation;
    }

    public static Business register(String id) {
        return register(false, id);
    }

    private static Business registerBase(String id) {
        return register(true, id);
    }

    private static Business register(boolean allowDefaultNamespace, String id) {
        return Registries.BUSINESS.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Business(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Business business : Registries.BUSINESS) {
                TranslationKeyValidator.of(business.getTranslation());
            }
        }
    }
}
