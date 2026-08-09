// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.UsedFromLua;
import zombie.core.Core;

@UsedFromLua
public class ContainerType {
    public static final ContainerType BARBECUE = registerBase("barbecue");
    public static final ContainerType BARBECUE_PROPANE = registerBase("barbecuepropane");
    public static final ContainerType BIN = registerBase("bin");
    public static final ContainerType BRAZIER = registerBase("brazier");
    public static final ContainerType CAMPFIRE = registerBase("campfire");
    public static final ContainerType CARDBOARD_BOX = registerBase("cardboardbox");
    public static final ContainerType CASH_REGISTER = registerBase("cashregister");
    public static final ContainerType CLOTHING_DRYER = registerBase("clothingdryer");
    public static final ContainerType CLOTHING_DRYER_BASIC = registerBase("clothingdryerbasic");
    public static final ContainerType CLOTHING_RACK = registerBase("clothingrack");
    public static final ContainerType CLOTHING_WASHER = registerBase("clothingwasher");
    public static final ContainerType COFFEE_MAKER = registerBase("coffeemaker");
    public static final ContainerType COFFIN = registerBase("coffin");
    public static final ContainerType COMPOSTER = registerBase("composter");
    public static final ContainerType CORN = registerBase("corn");
    public static final ContainerType COUNTER = registerBase("counter");
    public static final ContainerType CRATE = registerBase("crate");
    public static final ContainerType CUPBOARD = registerBase("cupboard");
    public static final ContainerType DESK = registerBase("desk");
    public static final ContainerType DISHES_CABINET = registerBase("dishescabinet");
    public static final ContainerType DISHWASHER = registerBase("dishwasher");
    public static final ContainerType DISPLAY_CASE = registerBase("displaycase");
    public static final ContainerType DISPLAY_CASE_BUTCHER = registerBase("displaycasebutcher");
    public static final ContainerType DISPLAY_CASE_BAKERY = registerBase("displaycasebakery");
    public static final ContainerType DOG_HOUSE = registerBase("doghouse");
    public static final ContainerType DRESSER = registerBase("dresser");
    public static final ContainerType DUMPSTER = registerBase("dumpster");
    public static final ContainerType FILING_CABINET = registerBase("filingcabinet");
    public static final ContainerType FIREPLACE = registerBase("fireplace");
    public static final ContainerType FLOOR = registerBase("floor");
    public static final ContainerType FREEZER = registerBase("freezer");
    public static final ContainerType FRIDGE = registerBase("fridge");
    public static final ContainerType FRUIT_BUSH_A = registerBase("fruitbusha");
    public static final ContainerType FRUIT_BUSH_B = registerBase("fruitbushb");
    public static final ContainerType FRUIT_BUSH_C = registerBase("fruitbushc");
    public static final ContainerType FRUIT_BUSH_D = registerBase("fruitbushd");
    public static final ContainerType FRUIT_BUSH_E = registerBase("fruitbushe");
    public static final ContainerType GARAGE_STORAGE = registerBase("garagestorage");
    public static final ContainerType GROCER_STANDg = registerBase("grocerstand");
    public static final ContainerType GLOVE_BOX = registerBase("GloveBox");
    public static final ContainerType ICE_CREAM = registerBase("icecream");
    public static final ContainerType INVENTORY_MALE = registerBase("inventoryfemale");
    public static final ContainerType INVENTORY_FEMALE = registerBase("inventorymale");
    public static final ContainerType LOCKER = registerBase("locker");
    public static final ContainerType LOGS = registerBase("logs");
    public static final ContainerType MANNEQUIN = registerBase("Mannequin");
    public static final ContainerType MEDICINE = registerBase("medicine");
    public static final ContainerType METAL_SHELVES = registerBase("metal_shelves");
    public static final ContainerType MICROWAVE = registerBase("microwave");
    public static final ContainerType MILITARY_CRATE = registerBase("militarycrate");
    public static final ContainerType MILITARY_LOCKER = registerBase("militarylocker");
    public static final ContainerType NEWSPAPER_DISPATCH = registerBase("newspaper_dispatch");
    public static final ContainerType NEWSPAPER_HERALD = registerBase("newspaper_herald");
    public static final ContainerType NEWSPAPER_KNEWS = registerBase("newspaper_knews");
    public static final ContainerType NEWSPAPER_TIMES = registerBase("newspaper_times");
    public static final ContainerType OFFICE_DRAWERS = registerBase("officedrawers");
    public static final ContainerType OVERHEAD = registerBase("overhead");
    public static final ContainerType PLANK_STASH = registerBase("plankstash");
    public static final ContainerType POSTBOX = registerBase("postbox");
    public static final ContainerType RESTAURANT_DISPLAY = registerBase("restaurantdisplay");
    public static final ContainerType SEAT_FRONT_LEFT = registerBase("SeatFrontLeft");
    public static final ContainerType SEAT_FRONT_RIGHT = registerBase("SeatFrontRight");
    public static final ContainerType SEAT_MIDDLE_LEFT = registerBase("SeatMiddleLeft");
    public static final ContainerType SEAT_MIDDLE_RIGHT = registerBase("SeatMiddleRight");
    public static final ContainerType SEAT_REAR_LEFT = registerBase("SeatRearLeft");
    public static final ContainerType SEAT_REAR_RIGHT = registerBase("SeatRearRight");
    public static final ContainerType SHELTER = registerBase("shelter");
    public static final ContainerType SHELVES = registerBase("shelves");
    public static final ContainerType SHELVES_MAG = registerBase("shelvesmag");
    public static final ContainerType SIDETABLE = registerBase("sidetable");
    public static final ContainerType SMALL_BOX = registerBase("smallbox");
    public static final ContainerType SMALL_CRATE = registerBase("smallcrate");
    public static final ContainerType STONE_FURNACE = registerBase("stonefurnace");
    public static final ContainerType STOVE = registerBase("stove");
    public static final ContainerType SURVIVOR_CRATE = registerBase("SurvivorCrate");
    public static final ContainerType TENT = registerBase("tent");
    public static final ContainerType TOASTER = registerBase("toaster");
    public static final ContainerType TOOL_CABINET = registerBase("toolcabinet");
    public static final ContainerType TROUGH = registerBase("trough");
    public static final ContainerType TRUCK_BED = registerBase("TruckBed");
    public static final ContainerType TRUCK_BED_OPEN = registerBase("TruckBedOpen");
    public static final ContainerType VENDING_GT = registerBase("vendingGt");
    public static final ContainerType VENDING_SNACK = registerBase("vendingsnack");
    public static final ContainerType VENDING_POP = registerBase("vendingpop");
    public static final ContainerType WARDROVE = registerBase("wardrobe");
    public static final ContainerType WOOD_STOVE = registerBase("woodstove");
    private final String translationName;

    private ContainerType(String translationName) {
        this.translationName = "IGUI_ContainerTitle_" + translationName;
    }

    public static ContainerType get(ResourceLocation id) {
        return Registries.CONTAINER_TYPE.get(id);
    }

    @Override
    public String toString() {
        return Registries.CONTAINER_TYPE.getLocation(this).toString();
    }

    public String getTranslationName() {
        return this.translationName;
    }

    public static ContainerType register(String id) {
        return register(false, id);
    }

    private static ContainerType registerBase(String id) {
        return register(true, id);
    }

    private static ContainerType register(boolean allowDefaultNamespace, String id) {
        return Registries.CONTAINER_TYPE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new ContainerType(id));
    }

    static {
        if (Core.IS_DEV) {
            for (ContainerType containerType : Registries.CONTAINER_TYPE) {
                TranslationKeyValidator.of(containerType.translationName);
            }
        }
    }
}
