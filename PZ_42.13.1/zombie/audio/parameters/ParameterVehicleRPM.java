// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.core.math.PZMath;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleRPM extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleRPM(BaseVehicle vehicle) {
        super("VehicleRPM");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        float rpm1 = PZMath.clamp((float)this.vehicle.getEngineSpeed(), 0.0F, 7000.0F);
        float rpmIdle = this.vehicle.getScript().getEngineIdleSpeed();
        float rpmIdleMax = rpmIdle * 1.1F;
        float FMOD_IDLE_MAX = 800.0F;
        float FMOD_RPM_MAX = 7000.0F;
        float rpm2;
        if (rpm1 < rpmIdleMax) {
            rpm2 = rpm1 / rpmIdleMax * 800.0F;
        } else {
            rpm2 = 800.0F + (rpm1 - rpmIdleMax) / (7000.0F - rpmIdleMax) * 6200.0F;
        }

        return (int)((rpm2 + 50.0F - 1.0F) / 50.0F) * 50.0F;
    }
}
