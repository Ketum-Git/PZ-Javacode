// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

public enum VehiclePart {
    BATTERY("Battery"),
    BRAKE("Brake"),
    BRAKE_FRONT_LEFT("BrakeFrontLeft"),
    BRAKE_FRONT_RIGHT("BrakeFrontRight"),
    BRAKE_REAR_LEFT("BrakeRearLeft"),
    BRAKE_REAR_RIGHT("BrakeRearRight"),
    DOOR("Door"),
    DOOR_FRONT_LEFT("DoorFrontLeft"),
    DOOR_FRONT_RIGHT("DoorFrontRight"),
    DOOR_MIDDLE_LEFT("DoorMiddleLeft"),
    DOOR_MIDDLE_RIGHT("DoorMiddleRight"),
    DOOR_REAR("DoorRear"),
    DOOR_REAR_LEFT("DoorRearLeft"),
    DOOR_REAR_RIGHT("DoorRearRight"),
    ENGINE("Engine"),
    ENGINE_DOOR("EngineDoor"),
    GAS_TANK("GasTank"),
    GLOVE_BOX("GloveBox"),
    HEADLIGHT("Headlight"),
    HEADLIGHT_LEFT("HeadlightLeft"),
    HEADLIGHT_REAR_LEFT("HeadlightRearLeft"),
    HEADLIGHT_REAR_RIGHT("HeadlightRearRight"),
    HEADLIGHT_RIGHT("HeadlightRight"),
    HEATER("Heater"),
    HOOD_ORNAMENT("HoodOrnament"),
    LIGHTBAR("lightbar"),
    MUFFLER("Muffler"),
    PASSENGER_COMPARTMENT("PassengerCompartment"),
    RADIO("Radio"),
    SEAT("Seat"),
    SEAT_FRONT_LEFT("SeatFrontLeft"),
    SEAT_FRONT_RIGHT("SeatFrontRight"),
    SEAT_MIDDLE_LEFT("SeatMiddleLeft"),
    SEAT_MIDDLE_RIGHT("SeatMiddleRight"),
    SEAT_REAR("SeatRear"),
    SEAT_REAR_LEFT("SeatRearLeft"),
    SEAT_REAR_RIGHT("SeatRearRight"),
    SUSPENSION("Suspension"),
    SUSPENSION_FRONT_LEFT("SuspensionFrontLeft"),
    SUSPENSION_FRONT_RIGHT("SuspensionFrontRight"),
    SUSPENSION_REAR_LEFT("SuspensionRearLeft"),
    SUSPENSION_REAR_RIGHT("SuspensionRearRight"),
    TEST_PART_1("TestPart1"),
    TIRE("Tire"),
    TIRE_FRONT_LEFT("TireFrontLeft"),
    TIRE_FRONT_RIGHT("TireFrontRight"),
    TIRE_REAR_LEFT("TireRearLeft"),
    TIRE_REAR_RIGHT("TireRearRight"),
    TRAILER_ANIMAL_EGGS("TrailerAnimalEggs"),
    TRAILER_ANIMAL_FOOD("TrailerAnimalFood"),
    TRAILER_TRUNK("TrailerTrunk"),
    TRUCK_BED("TruckBed"),
    TRUCK_BED_OPEN("TruckBedOpen"),
    TRUNK_DOOR("TrunkDoor"),
    TRUNK_DOOR_OPENED("TrunkDoorOpened"),
    WINDOW("Window"),
    WINDOW_FRONT_LEFT("WindowFrontLeft"),
    WINDOW_FRONT_RIGHT("WindowFrontRight"),
    WINDOW_MIDDLE_LEFT("WindowMiddleLeft"),
    WINDOW_MIDDLE_RIGHT("WindowMiddleRight"),
    WINDOW_REAR_LEFT("WindowRearLeft"),
    WINDOW_REAR_RIGHT("WindowRearRight"),
    WINDSHIELD("Windshield"),
    WINDSHIELD_REAR("WindshieldRear");

    private final String id;

    private VehiclePart(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
