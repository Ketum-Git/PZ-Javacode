// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleNetworkSound;

import zombie.vehicles.BaseVehicle;

public final class VehicleStateFlags {
    public static final short ALARM_ON = 1;
    public static final short BACKUP_BEEPER_ON = 2;
    public static final short BATTERY_OK = 4;
    public static final short DOOR_ALARM_ON = 8;
    public static final short HORN_ON = 16;
    public static final short SIREN_ON = 32;
    public static final short BRAKE_PEDAL_PRESSED = 64;
    public static final short GAS_PEDAL_PRESSED = 128;
    public static final short ANY_TIRE_MISSING = 256;
    public static final short ALARM_ACTIVE = 512;

    public static short fromVehicle(BaseVehicle vehicle) {
        short flags = 0;
        if (vehicle.isAlarmActive()) {
            flags = (short)(flags | 512);
        }

        if (vehicle.isAlarmSounding()) {
            flags = (short)(flags | 1);
        }

        if (vehicle.isBackupBeeperSounding()) {
            flags = (short)(flags | 2);
        }

        if (vehicle.getBatteryCharge() > 0.0F) {
            flags = (short)(flags | 4);
        }

        if (vehicle.isDoorAlarmSounding()) {
            flags = (short)(flags | 8);
        }

        if (vehicle.isHornSounding()) {
            flags = (short)(flags | 16);
        }

        if (vehicle.isSirenSounding()) {
            flags = (short)(flags | 32);
        }

        if (vehicle.isBrakePedalPressed()) {
            flags = (short)(flags | 64);
        }

        if (vehicle.isGasPedalPressed()) {
            flags = (short)(flags | 128);
        }

        if (vehicle.isAnyTireMissing()) {
            flags = (short)(flags | 256);
        }

        return flags;
    }
}
