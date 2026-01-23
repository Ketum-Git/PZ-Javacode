// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleLoad extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleLoad(BaseVehicle vehicle) {
        super("VehicleLoad");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        return this.vehicle.getController().isGasPedalPressed() ? 1.0F : 0.0F;
    }
}
