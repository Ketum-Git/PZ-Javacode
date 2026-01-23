// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleSpeed extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleSpeed(BaseVehicle vehicle) {
        super("VehicleSpeed");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        return (float)Math.floor(Math.abs(this.vehicle.getCurrentSpeedKmHour()));
    }
}
