// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum VehicleWheel {
    REAR_LEFT("RearLeft"),
    REAR_RIGHT("RearRight"),
    FRONT_RIGHT("FrontRight"),
    FRONT_LEFT("FrontLeft");

    private final String id;

    private VehicleWheel(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
