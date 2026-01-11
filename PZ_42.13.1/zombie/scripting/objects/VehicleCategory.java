// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum VehicleCategory {
    BODYWORK("bodywork"),
    BRAKES("brakes"),
    DOOR("door"),
    ENGINE("engine"),
    GASTANK("gastank"),
    LIGHTS("lights"),
    NODISPLAY("nodisplay"),
    SEAT("seat"),
    SUSPENSION("suspension"),
    TIRE("tire");

    private final String id;

    private VehicleCategory(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
