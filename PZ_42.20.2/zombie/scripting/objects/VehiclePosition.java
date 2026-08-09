// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum VehiclePosition {
    OUTSIDE("outside"),
    INSIDE("inside"),
    OUTSIDE2("outside2");

    private final String id;

    private VehiclePosition(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
