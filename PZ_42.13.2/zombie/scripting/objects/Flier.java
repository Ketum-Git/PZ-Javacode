// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.UsedFromLua;
import zombie.core.Core;

@UsedFromLua
public class Flier {
    public static final Flier A1_HAY = registerBase("A1Hay");
    public static final Flier AMZ_STEEL = registerBase("AMZSteel");
    public static final Flier ALS_AUTO_SHOP = registerBase("AlsAutoShop");
    public static final Flier AMERICAN_TIRE = registerBase("AmericanTire");
    public static final Flier BEEF_CHUNK = registerBase("BeefChunk");
    public static final Flier BENS_CABIN = registerBase("BensCabin");
    public static final Flier BRANDENBURG_FD = registerBase("BrandenburgFD");
    public static final Flier BROOKS_LIBRARY = registerBase("BrooksLibrary");
    public static final Flier BROTT_AUCTION = registerBase("BrottAuction");
    public static final Flier CABIN_FOR_RENT_DIXIE = registerBase("CabinforRentDixie");
    public static final Flier CAR_FIXATION = registerBase("CarFixation");
    public static final Flier CAT_ON_A_HOT_TIN_GRILL = registerBase("CatonaHotTinGrill");
    public static final Flier CIRCUITAL_HEALING = registerBase("CircuitalHealing");
    public static final Flier DRAG_RACING_TRACK = registerBase("DragRacingTrack");
    public static final Flier DU_CASE_APARTMENTS = registerBase("DuCaseApartments");
    public static final Flier EP_TOOLS_LV = registerBase("EPToolsLV");
    public static final Flier EKRON_COLLEGE = registerBase("EkronCollege");
    public static final Flier ELVEE_ARENA = registerBase("ElveeArena");
    public static final Flier FALLAS_LAKE_CHURCH = registerBase("FallasLakeChurch");
    public static final Flier FARMERS_MARKET = registerBase("FarmersMarket");
    public static final Flier FARMING_AND_RURAL_SUPPLY_DOE_VALLEY = registerBase("FarmingAndRuralSupplyDoeValley");
    public static final Flier FASHION_A_BELLE = registerBase("FashionaBelle");
    public static final Flier FIVE_ALARM_CHILI = registerBase("FiveAlarmChili");
    public static final Flier FOSSOIL_1 = registerBase("Fossoil1");
    public static final Flier FOSSOIL_2 = registerBase("Fossoil2");
    public static final Flier FOSSOIL_3 = registerBase("Fossoil3");
    public static final Flier FOSSOIL_4 = registerBase("Fossoil4");
    public static final Flier FOSSOIL_5 = registerBase("Fossoil5");
    public static final Flier FOSSOIL_6 = registerBase("Fossoil6");
    public static final Flier FOSSOIL_7 = registerBase("Fossoil7");
    public static final Flier FOSSOIL_8 = registerBase("Fossoil8");
    public static final Flier FOURTH_OF_JULY_CELEBRATION_DIXIE_MOBILE_PARK = registerBase("FourthofJulyCelebrationDixieMobilePark");
    public static final Flier GNOME_SWEET_GNOME = registerBase("GnomeSweetGnome");
    public static final Flier GOLDEN_SUNSET = registerBase("GoldenSunset");
    public static final Flier GREENES_JOB_AD_EKRON = registerBase("GreenesJobAdEkron");
    public static final Flier GUNS_UNLIMITED_ECHO_CREEK = registerBase("GunsUnlimitedEchoCreek");
    public static final Flier HIGH_STREET_APARTMENTS = registerBase("HighStreetApartments");
    public static final Flier HIT_VIDS_JOB_AD_MARCH_RIDGE = registerBase("HitVidsJobAdMarchRidge");
    public static final Flier HOBBS_AND_PERKINS_HARDWARE = registerBase("HobbsandPerkinsHardware");
    public static final Flier HOUSE_FOR_SALE_787 = registerBase("HouseforSale787");
    public static final Flier HOUSE_FOR_SALE_799 = registerBase("HouseforSale799");
    public static final Flier HOUSE_FOR_SALE_818 = registerBase("HouseforSale818");
    public static final Flier HOUSE_FOR_SALE_845 = registerBase("HouseforSale845");
    public static final Flier HOUSE_FOR_SALE_851 = registerBase("HouseforSale851");
    public static final Flier HOUSE_FOR_SALE_855 = registerBase("HouseforSale855");
    public static final Flier HOUSE_FOR_SALE_860 = registerBase("HouseforSale860");
    public static final Flier HOUSE_FOR_SALE_867 = registerBase("HouseforSale867");
    public static final Flier HOUSE_FOR_SALE_895 = registerBase("HouseforSale895");
    public static final Flier HOUSE_FOR_SALE_903 = registerBase("HouseforSale903");
    public static final Flier HOUSE_FOR_SALE_907 = registerBase("HouseforSale907");
    public static final Flier HOUSE_FOR_SALE_912 = registerBase("HouseforSale912");
    public static final Flier HOUSE_FOR_SALE_915 = registerBase("HouseforSale915");
    public static final Flier HOUSE_FOR_SALE_919 = registerBase("HouseforSale919");
    public static final Flier HOUSE_FOR_SALE_922 = registerBase("HouseforSale922");
    public static final Flier HOUSE_FOR_SALE_929 = registerBase("HouseforSale929");
    public static final Flier HOUSE_FOR_SALE_930 = registerBase("HouseforSale930");
    public static final Flier HOUSE_FOR_SALE_934 = registerBase("HouseforSale934");
    public static final Flier HOUSE_FOR_SALE_943 = registerBase("HouseforSale943");
    public static final Flier IRVINGTON_GUN_CLUB = registerBase("IrvingtonGunClub");
    public static final Flier KNOX_BANK_JOB_AD_ROSEWOOD = registerBase("KnoxBankJobAdRosewood");
    public static final Flier KNOX_GUN_OWNERS_CLUB_GET_TOGETHER = registerBase("KnoxGunOwnersClubGetTogether");
    public static final Flier KNOX_PACK_KITCHENS = registerBase("KnoxPackKitchens");
    public static final Flier LVFD = registerBase("LVFD");
    public static final Flier LVPD_HQ = registerBase("LVPDHQ");
    public static final Flier LEAFHILL_HEIGHTS = registerBase("LeafhillHeights");
    public static final Flier LECTROMAX_MANUFACTURING_JOB_AD = registerBase("LectromaxManufacturingJobAd");
    public static final Flier LENNYS_CAR_REPAIR = registerBase("LennysCarRepair");
    public static final Flier LOUISVILLE_BRUISER = registerBase("LouisvilleBruiser");
    public static final Flier LOVE_DUET = registerBase("LoveDuet");
    public static final Flier LOWRY_COURT = registerBase("LowryCourt");
    public static final Flier MAD_DANS_DEN = registerBase("MadDansDen");
    public static final Flier MAIL_CARRIER_AD_EKRON = registerBase("MailCarrierAdEkron");
    public static final Flier MARCH_RIDGE_SCHOOL_JOB_AD = registerBase("MarchRidgeSchoolJobAd");
    public static final Flier MCCOY_LOGGING_CORP = registerBase("McCoyLoggingCorp");
    public static final Flier MEADSHIRE_ESTATES = registerBase("MeadshireEstate");
    public static final Flier MULDRAUGH_BAKE_SALE = registerBase("MuldraughBakeSale");
    public static final Flier MULDRAUGH_PD = registerBase("MuldraughPD");
    public static final Flier MUSIC_FEST_93 = registerBase("MusicFest93");
    public static final Flier NAILS_AND_NUTS = registerBase("NailsAndNuts");
    public static final Flier NOLANS_USED_CARS = registerBase("NolansUsedCars");
    public static final Flier OLD_CGE_CORP_BUILDING = registerBase("OldCGECorpBuilding");
    public static final Flier ONYX_DRIVE_IN_THEATER = registerBase("OnyxDriveinTheater");
    public static final Flier OVO_FARMS = registerBase("OvoFarms");
    public static final Flier PILEO_CREPE_JOB_AD_CROSS_ROADS_MALL = registerBase("PileoCrepeJobAdCrossRoadsMall");
    public static final Flier PIZZA_WHIRLED_JOB_AD_ROSEWOOD = registerBase("PizzaWhirledJobAdRosewood");
    public static final Flier PREMISES_FOR_LEASE_863 = registerBase("PremisesforLease863");
    public static final Flier PREMISES_WITH_APARTMENTS_FOR_LEASE_LISTING_NO_891 = registerBase("PremiseswithApartmentsforLeaselistingno891");
    public static final Flier READY_PREP = registerBase("ReadyPrep");
    public static final Flier RED_OAK_APARTMENTS = registerBase("RedOakApartments");
    public static final Flier RIVERSIDE_INDEPENDENCE_DAY_PARTY_ALL_WELCOME = registerBase("RiversideIndependenceDayPartyAllWelcome");
    public static final Flier RIVERSIDE_PD = registerBase("RiversidePD");
    public static final Flier ROSEWOOD_FD = registerBase("RosewoodFD");
    public static final Flier ROXYS_ROLLER_RINK = registerBase("RoxysRollerRink");
    public static final Flier RUSTY_RIFLE = registerBase("RustyRifle");
    public static final Flier SAMMIES = registerBase("Sammies");
    public static final Flier SPIFFOS_HIRING_DIXIE = registerBase("SpiffosHiringDixie");
    public static final Flier SPIFFOS_HIRING_LOUISVILLE = registerBase("SpiffosHiringLouisville");
    public static final Flier SPIFFOS_HIRING_WEST_POINT = registerBase("SpiffosHiringWestPoint");
    public static final Flier STUART_AND_LOG_SCRAPYARD = registerBase("StuartandLogScrapyard");
    public static final Flier SUNSET_PINES_FUNERAL_HOME = registerBase("SunsetPinesFuneralHome");
    public static final Flier SURE_FITNESS_BOXING_CLUB = registerBase("SureFitnessBoxingClub");
    public static final Flier TACO_DEL_PANCHO = registerBase("TacodelPancho");
    public static final Flier THE_SEA_SHANTY = registerBase("TheSeaShanty");
    public static final Flier THE_WIZARDS_KEEP = registerBase("TheWizardsKeep");
    public static final Flier TWIGGYS = registerBase("Twiggys");
    public static final Flier U_STORE_IT_LOUISVILLE = registerBase("UStoreItLouisville");
    public static final Flier U_STORE_IT_MULDRAUGH = registerBase("UStoreItMuldraugh");
    public static final Flier U_STORE_IT_RIVERSIDE = registerBase("UStoreItRiverside");
    public static final Flier UPSCALE_MOBILITY = registerBase("UpscaleMobility");
    public static final Flier WPDIY = registerBase("WPDIY");
    public static final Flier WP_TOWN_HALL = registerBase("WPTownHall");
    public static final Flier YOUR_LOCAL_SHELTER_BRANDENBURG = registerBase("YourLocalShelterBrandenburg");
    private final String translationKey;
    private final String translationInfoKey;
    private final String translationTextKey;

    private Flier(String id) {
        this.translationKey = "Print_Media_" + id + "_title";
        this.translationInfoKey = "Print_Media_" + id + "_info";
        this.translationTextKey = "Print_Text_" + id + "_info";
    }

    public static Flier get(ResourceLocation id) {
        return Registries.FLIER.get(id);
    }

    @Override
    public String toString() {
        return Registries.FLIER.getLocation(this).getPath();
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

    public static Flier register(String id) {
        return register(false, id);
    }

    private static Flier registerBase(String id) {
        return register(true, id);
    }

    private static Flier register(boolean allowDefaultNamespace, String id) {
        return Registries.FLIER.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Flier(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Flier flier : Registries.FLIER) {
                TranslationKeyValidator.of(flier.translationKey);
                TranslationKeyValidator.of(flier.translationInfoKey);
                TranslationKeyValidator.of(flier.translationTextKey);
            }
        }
    }
}
