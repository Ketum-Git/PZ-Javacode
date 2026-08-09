// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.vehicleSound.VehicleSoundOwner;

public class ParameterVehicleBrake extends FMODLocalParameter {
    private final VehicleSoundOwner vehicle;

    public ParameterVehicleBrake(VehicleSoundOwner vehicle) {
        super("VehicleBrake");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        return this.vehicle.isBrakePedalPressed() ? 1.0F : 0.0F;
    }
}
