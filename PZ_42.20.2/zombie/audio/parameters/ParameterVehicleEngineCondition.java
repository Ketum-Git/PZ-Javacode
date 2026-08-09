// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.core.math.PZMath;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class ParameterVehicleEngineCondition extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleEngineCondition(BaseVehicle vehicle) {
        super("VehicleEngineCondition");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        VehiclePart part = this.vehicle.getPartById("Engine");
        return part == null ? 100.0F : PZMath.clamp(part.getCondition(), 0, 100);
    }
}
