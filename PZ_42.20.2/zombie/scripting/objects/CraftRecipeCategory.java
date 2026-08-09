// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum CraftRecipeCategory {
    ARMOR("Armor"),
    ASSEMBLY("Assembly"),
    BLACKSMITHING("Blacksmithing"),
    BLADE("Blade"),
    CARPENTRY("Carpentry"),
    CARVING("Carving"),
    COOKING("Cooking"),
    COOKWARE("Cookware"),
    ELECTRICAL("Electrical"),
    FARMING("Farming"),
    FISHING("Fishing"),
    GLASSMAKING("Glassmaking"),
    KNAPPING("Knapping"),
    MASONRY("Masonry"),
    MEDICAL("Medical"),
    METALWORKING("Metalworking"),
    MISCELLANEOUS("Miscellaneous"),
    PACKING("Packing"),
    POTTERY("Pottery"),
    REPAIR("Repair"),
    TAILORING("Tailoring"),
    TOOLS("Tools"),
    WEAPONRY("Weaponry");

    private final String id;

    private CraftRecipeCategory(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
