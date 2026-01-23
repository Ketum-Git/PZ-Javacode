// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum ItemFabricType {
    COTTON("Cotton"),
    DENIM("Denim"),
    LEATHER("Leather");

    private final String id;

    private ItemFabricType(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
