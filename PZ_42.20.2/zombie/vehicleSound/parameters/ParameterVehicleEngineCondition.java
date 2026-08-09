// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.core.math.PZMath;
import zombie.vehicleSound.VehicleSoundOwner;

public class ParameterVehicleEngineCondition extends FMODLocalParameter {
    private final VehicleSoundOwner vehicle;

    public ParameterVehicleEngineCondition(VehicleSoundOwner vehicle) {
        super("VehicleEngineCondition");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        return PZMath.clamp(this.vehicle.getEngineCondition(), 0, 100);
    }
}
