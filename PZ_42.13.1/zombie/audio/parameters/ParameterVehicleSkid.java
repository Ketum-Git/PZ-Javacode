// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.core.math.PZMath;
import zombie.network.GameClient;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleSkid extends FMODLocalParameter {
    private final BaseVehicle vehicle;
    private final BaseVehicle.WheelInfo[] wheelInfo;

    public ParameterVehicleSkid(BaseVehicle vehicle) {
        super("VehicleSkid");
        this.vehicle = vehicle;
        this.wheelInfo = vehicle.wheelInfo;
    }

    @Override
    public float calculateCurrentValue() {
        float value = 1.0F;
        if (GameClient.client && !this.vehicle.isLocalPhysicSim()) {
            return value;
        } else {
            VehicleScript script = this.vehicle.getScript();
            if (script == null) {
                return value;
            } else {
                int i = 0;

                for (int count = script.getWheelCount(); i < count; i++) {
                    value = PZMath.min(value, this.wheelInfo[i].skidInfo);
                }

                return (int)(100.0F - PZMath.clamp(value, 0.0F, 1.0F) * 100.0F) / 100.0F;
            }
        }
    }
}
