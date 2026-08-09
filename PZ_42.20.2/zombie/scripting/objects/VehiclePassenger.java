// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum VehiclePassenger {
    ALL("*"),
    FRONT_LEFT("FrontLeft"),
    FRONT_RIGHT("FrontRight"),
    MIDDLE_LEFT("MiddleLeft"),
    MIDDLE_RIGHT("MiddleRight"),
    REAR_LEFT("RearLeft"),
    REAR_RIGHT("RearRight");

    private final String id;

    private VehiclePassenger(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
