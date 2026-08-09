// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum VehicleTable {
    HEADLIGHT("headlight"),
    INSTALL("install"),
    UNINSTALL("uninstall");

    private final String id;

    private VehicleTable(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
