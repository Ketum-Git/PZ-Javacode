// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;

public class Catalogue {
    public static final Catalogue AA_RON_HUNTING_SUPPLIES = registerBase("AARonHuntingSupplies");
    public static final Catalogue AMERICAN_TIRE = registerBase("AmericanTire");
    public static final Catalogue AMERICAN_VACATIONS = registerBase("AmericanVacations");
    public static final Catalogue ART_GALLERY_OF_LOUISVILLE_ARTWORKS = registerBase("ArtGalleryofLouisvilleArtworks");
    public static final Catalogue BARG_N_CLOTHES = registerBase("BargNClothes");
    public static final Catalogue BETTER_FURNISHINGS = registerBase("BetterFurnishings");
    public static final Catalogue BICYCLES_93 = registerBase("Bicycles93");
    public static final Catalogue BLOCKO = registerBase("Blocko");
    public static final Catalogue BRAC_BRICK_FACTORY = registerBase("BracBrickFactory");
    public static final Catalogue BUTTERFLY_MACHINERY = registerBase("ButterflyMachinery");
    public static final Catalogue CALLYS_GIFTS = registerBase("CallysGifts");
    public static final Catalogue CROSSROADS_MALL = registerBase("CrossroadsMall");
    public static final Catalogue CRYSTALS_CHARMS = registerBase("CrystalsCharms");
    public static final Catalogue DASH_MOTORS = registerBase("DashMotors");
    public static final Catalogue DOLLIE_1993 = registerBase("Dollie1993");
    public static final Catalogue FAMOUS_ARTWORKS = registerBase("FamousArtworks");
    public static final Catalogue FASHIONA_BELLE = registerBase("FashionaBelle");
    public static final Catalogue FRANKLIN_MOTORS = registerBase("FranklinMotors");
    public static final Catalogue GUNS_AMMO = registerBase("GunsAmmo");
    public static final Catalogue HIT_VIDS = registerBase("HitVids");
    public static final Catalogue HOMEWARD_REAL_ESTATE = registerBase("HomewardRealEstate");
    public static final Catalogue HUGO_PLUSH = registerBase("HugoPlush");
    public static final Catalogue INKR_ALLEY = registerBase("InkrAlley");
    public static final Catalogue KENTUCKY_REAL_ESTATE_GUIDE = registerBase("KentuckyRealEstateGuide");
    public static final Catalogue KIRRUS_COMPUTERS = registerBase("KirrusComputers");
    public static final Catalogue KIRRUS_SOFTWARE = registerBase("KirrusSoftware");
    public static final Catalogue KNOX_KITCHENS = registerBase("KnoxKitchens");
    public static final Catalogue LOUISVILLE_BOAT_CLUB = registerBase("LouisvilleBoatClub");
    public static final Catalogue LOUISVILLE_BRUISER = registerBase("LouisvilleBruiser");
    public static final Catalogue LOUISVILLE_STATE_UNIVERSITY = registerBase("LouisvilleStateUniversity");
    public static final Catalogue LUTHEX_WATCHES = registerBase("LuthexWatches");
    public static final Catalogue MOTORBIKES_AND_MORE = registerBase("MotorbikesandMore");
    public static final Catalogue NOLANS_USED_CARS = registerBase("NolansUsedCars");
    public static final Catalogue OLYMPIA_DEPARTMENT_STORE = registerBase("OlympiaDepartmentStore");
    public static final Catalogue PALM_TRAVEL = registerBase("PalmTravel");
    public static final Catalogue PANTHER_MOTORS = registerBase("PantherMotors");
    public static final Catalogue PAWS = registerBase("Paws");
    public static final Catalogue PET_SHELTER = registerBase("PetShelter");
    public static final Catalogue PLANE_SAILING = registerBase("PlaneSailing");
    public static final Catalogue SCENIC_MILE_CAR_DEALERSHIP = registerBase("ScenicMileCarDealership");
    public static final Catalogue SEAT_YOURSELF_FURNITURE = registerBase("SeatYourselfFurniture");
    public static final Catalogue SHEBA_JEWELLERS = registerBase("ShebaJewellers");
    public static final Catalogue SHOED_FOR_THE_STARS = registerBase("ShoedFortheStars");
    public static final Catalogue SIGHTS_OF_KENTUCKY = registerBase("SightsofKentucky");
    public static final Catalogue TEEN_IDLEZ = registerBase("TeenIdlez");
    public static final Catalogue THE_GRAND_OHIO_MALL = registerBase("TheGrandOhioMall");
    public static final Catalogue THE_MODERATORS = registerBase("TheModerators");
    public static final Catalogue THE_TOP_HANGER = registerBase("TheTopHanger");
    public static final Catalogue TIGHT_END_SWIMWEAR = registerBase("TightEndSwimwear");
    public static final Catalogue TIME_4_SPORT = registerBase("Time4Sport");
    public static final Catalogue TOYZ = registerBase("Toyz");
    public static final Catalogue TTRPG = registerBase("TTRPG");
    public static final Catalogue WLW_MOTORS = registerBase("WLWMotors");
    public static final Catalogue WOOL_WEAR = registerBase("WoolWear");
    public static final Catalogue WORLDWIDE_VACATIONS = registerBase("WorldwideVacations");
    public static final Catalogue ZACS_HARDWARE = registerBase("ZacsHardware");
    private final String translationKey;

    private Catalogue(String id) {
        this.translationKey = "IGUI_" + id;
    }

    public static Catalogue get(ResourceLocation id) {
        return Registries.CATALOGUE.get(id);
    }

    @Override
    public String toString() {
        return Registries.CATALOGUE.getLocation(this).getPath();
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public static Catalogue register(String id) {
        return register(false, id);
    }

    private static Catalogue registerBase(String id) {
        return register(true, id);
    }

    private static Catalogue register(boolean allowDefaultNamespace, String id) {
        return Registries.CATALOGUE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new Catalogue(id));
    }

    static {
        if (Core.IS_DEV) {
            for (Catalogue catalogue : Registries.CATALOGUE) {
                TranslationKeyValidator.of(catalogue.translationKey);
            }
        }
    }
}
