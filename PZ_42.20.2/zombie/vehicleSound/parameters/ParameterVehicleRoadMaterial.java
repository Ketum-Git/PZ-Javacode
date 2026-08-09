// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.vehicleSound.VehicleSoundOwner;

public final class ParameterVehicleRoadMaterial extends FMODLocalParameter {
    private final VehicleSoundOwner vehicle;

    public ParameterVehicleRoadMaterial(VehicleSoundOwner vehicle) {
        super("VehicleRoadMaterial");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        if (!this.vehicle.isEngineRunning()) {
            return Float.isNaN(this.getCurrentValue()) ? 0.0F : this.getCurrentValue();
        } else {
            return this.getMaterial().label;
        }
    }

    private zombie.audio.parameters.ParameterVehicleRoadMaterial.Material getMaterial() {
        return this.vehicle.getRoadMaterial();
    }
}
