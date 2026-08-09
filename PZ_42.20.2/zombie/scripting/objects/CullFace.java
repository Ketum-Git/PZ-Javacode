// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum CullFace {
    BACK("Back"),
    FRONT("Front"),
    NONE("None");

    private final String id;

    private CullFace(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
