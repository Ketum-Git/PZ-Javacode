// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.network.GameClient;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleBrake extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleBrake(BaseVehicle vehicle) {
        super("VehicleBrake");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        if (GameClient.client && !this.vehicle.isLocalPhysicSim()) {
            return this.vehicle.getStoplightsOn() ? 1.0F : 0.0F;
        } else {
            return this.vehicle.getController().isBrakePedalPressed() ? 1.0F : 0.0F;
        }
    }
}
