// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum DigType {
    HOE("Hoe"),
    PICK_AXE("PickAxe"),
    SHOVEL("Shovel"),
    TROWEL("Trowel");

    private final String id;

    private DigType(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
