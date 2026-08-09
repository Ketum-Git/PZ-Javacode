// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.vehicleSound.VehicleSoundOwner;

public class ParameterVehicleLoad extends FMODLocalParameter {
    private final VehicleSoundOwner vehicle;

    public ParameterVehicleLoad(VehicleSoundOwner vehicle) {
        super("VehicleLoad");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        return this.vehicle.isGasPedalPressed() ? 1.0F : 0.0F;
    }
}
