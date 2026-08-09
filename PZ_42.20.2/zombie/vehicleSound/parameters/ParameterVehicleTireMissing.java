// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.vehicleSound.VehicleSoundOwner;

public class ParameterVehicleTireMissing extends FMODLocalParameter {
    private final VehicleSoundOwner vehicle;

    public ParameterVehicleTireMissing(VehicleSoundOwner vehicle) {
        super("VehicleTireMissing");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        return this.vehicle.isAnyTireMissing() ? 1.0F : 0.0F;
    }
}
