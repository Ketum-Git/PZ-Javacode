// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleGear extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleGear(BaseVehicle vehicle) {
        super("VehicleGear");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        return this.vehicle.getTransmissionNumber() + 1;
    }
}
