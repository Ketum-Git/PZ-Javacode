// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.core.math.PZMath;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleSteer extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleSteer(BaseVehicle vehicle) {
        super("VehicleSteer");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        float value = 0.0F;
        if (!this.vehicle.isEngineRunning()) {
            return value;
        } else {
            VehicleScript script = this.vehicle.getScript();
            if (script == null) {
                return value;
            } else {
                BaseVehicle.WheelInfo[] wheelInfo = this.vehicle.wheelInfo;
                int i = 0;

                for (int count = script.getWheelCount(); i < count; i++) {
                    value = PZMath.max(value, Math.abs(wheelInfo[i].steering));
                }

                return (int)(PZMath.clamp(value, 0.0F, 1.0F) * 100.0F) / 100.0F;
            }
        }
    }
}
