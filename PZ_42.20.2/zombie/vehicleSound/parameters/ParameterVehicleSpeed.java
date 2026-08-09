// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.vehicleSound.VehicleSoundOwner;

public class ParameterVehicleSpeed extends FMODLocalParameter {
    private final VehicleSoundOwner vehicle;

    public ParameterVehicleSpeed(VehicleSoundOwner vehicle) {
        super("VehicleSpeed");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        return (float)Math.floor(Math.abs(this.vehicle.getCurrentSpeedKmHour()));
    }
}
