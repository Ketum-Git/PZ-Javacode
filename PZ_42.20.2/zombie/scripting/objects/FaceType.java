// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum FaceType {
    N("N"),
    E("E"),
    S("S"),
    W("W"),
    SINGLE("SINGLE"),
    W_OPEN("W_OPEN"),
    N_OPEN("N_OPEN");

    private final String id;

    private FaceType(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
