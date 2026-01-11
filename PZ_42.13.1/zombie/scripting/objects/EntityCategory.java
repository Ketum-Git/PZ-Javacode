// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum EntityCategory {
    BARRICADES("Barricades"),
    BLACKSMITHING("Blacksmithing"),
    CARPENTRY("Carpentry"),
    DEBUG("Debug"),
    FARMING("Farming"),
    FURNITURE("Furniture"),
    MASONRY("Masonry"),
    MISCELLANEOUS("Miscellaneous"),
    OUTDOORS("Outdoors"),
    POTTERY("Pottery"),
    WELDING("Welding"),
    WALL_COVERING("Wall Coverings");

    private final String id;

    private EntityCategory(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
