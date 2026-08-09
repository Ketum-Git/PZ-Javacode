// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum CraftRecipeTag {
    ADVANCED_FORGE("AdvancedForge"),
    ADVANCED_FURNACE("AdvancedFurnace"),
    ANY_SURFACE_CRAFT("AnySurfaceCraft"),
    AUTO_ROTATE("AutoRotate"),
    BURN_WOOD("BurnWood"),
    CANNOT_BE_RESEARCHED("CannotBeResearched"),
    CAN_ALWAYS_BE_RESEARCHED("CanAlwaysBeResearched"),
    CAN_BE_DONE_FROM_FLOOR("CanBeDoneFromFloor"),
    CAN_BE_DONE_IN_DARK("CanBeDoneInDark"),
    CARPENTRY("Carpentry"),
    CHOPPING_BLOCK("ChoppingBlock"),
    CHURN_BUCKET("ChurnBucket"),
    COFFEE_MACHINE("CoffeeMachine"),
    COOKING("Cooking"),
    DOME_KILN("DomeKiln"),
    DRY_LEATHER_LARGE("DryLeatherLarge"),
    DRY_LEATHER_MEDIUM("DryLeatherMedium"),
    DRY_LEATHER_SMALL("DryLeatherSmall"),
    DRYING_RACK_GRAIN("DryingRackGrain"),
    DRYING_RACK_HERB("DryingRackHerb"),
    ELECTRICAL("Electrical"),
    ENGINEER("Engineer"),
    ENTITY_RECIPE("EntityRecipe"),
    FARMING("Farming"),
    FISHING("Fishing"),
    FORGE("Forge"),
    FUEL_CHARCOAL("FuelCharcoal"),
    FURNACE("Furnace"),
    FURNITURE("Furniture"),
    GLASSMAKING("Glassmaking"),
    GRINDSTONE("Grindstone"),
    HAND_PRESS("HandPress"),
    HEALTH("Health"),
    HECKLING("Heckling"),
    IN_HAND_CRAFT("InHandCraft"),
    KEY_DUPLICATOR("KeyDuplicator"),
    KILN_LARGE("KilnLarge"),
    KILN_SMALL("KilnSmall"),
    MASONRY("Masonry"),
    METAL_BANDSAW("MetalBandsaw"),
    OUTDOORS("Outdoors"),
    PACKING("Packing"),
    POTTERY("Pottery"),
    POTTERY_BENCH("PotteryBench"),
    POTTERY_WHEEL("PotteryWheel"),
    PRIMITIVE_FORGE("PrimitiveForge"),
    PRIMITIVE_FURNACE("PrimitiveFurnace"),
    REMOVE_FLESH("RemoveFlesh"),
    REMOVE_FUR("RemoveFur"),
    REMOVE_RESULT_ITEMS("RemoveResultItems"),
    RIGHT_CLICK_ONLY("RightClickOnly"),
    RIPPLING("Rippling"),
    SCUTCHING("Scutching"),
    SPINNING_WHEEL("SpinningWheel"),
    STANDING_DRILL_PRESS("StandingDrillPress"),
    STONE_MILL("Stone_Mill"),
    STONE_QUERN("Stone_Quern"),
    SURVIVAL("Survival"),
    SURVIVALIST("Survivalist"),
    TAN_LEATHER("TanLeather"),
    TOASTER("Toaster"),
    TRAPPER("Trapper"),
    WEAVING("Weaving"),
    WELDING("Welding"),
    WOOD_CHARCOAL("WoodCharcoal");

    private final String id;

    private CraftRecipeTag(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }

    public static CraftRecipeTag fromValue(String value) {
        for (CraftRecipeTag craftRecipeTag : values()) {
            if (craftRecipeTag.id.equals(value)) {
                return craftRecipeTag;
            }
        }

        throw new IllegalArgumentException("No enum constant for value: " + value);
    }
}
