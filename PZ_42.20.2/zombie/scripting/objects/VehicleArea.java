// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum VehicleArea {
    ANIMAL_ENTRY("AnimalEntry"),
    ENGINE("Engine"),
    GAS_TANK("GasTank"),
    LEFT_SIREN("LeftSiren"),
    RIGHT_SIREN("RightSiren"),
    SEAT_FRONT_LEFT("SeatFrontLeft"),
    SEAT_FRONT_RIGHT("SeatFrontRight"),
    SEAT_MIDDLE_LEFT("SeatMiddleLeft"),
    SEAT_MIDDLE_RIGHT("SeatMiddleRight"),
    SEAT_REAR_LEFT("SeatRearLeft"),
    SEAT_REAR_RIGHT("SeatRearRight"),
    TIRE_FRONT_LEFT("TireFrontLeft"),
    TIRE_FRONT_RIGHT("TireFrontRight"),
    TIRE_REAR_LEFT("TireRearLeft"),
    TIRE_REAR_RIGHT("TireRearRight"),
    TRAILER_ANIMAL_FOOD("TrailerAnimalFood"),
    TRUCK_BED("TruckBed");

    private final String id;

    private VehicleArea(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
