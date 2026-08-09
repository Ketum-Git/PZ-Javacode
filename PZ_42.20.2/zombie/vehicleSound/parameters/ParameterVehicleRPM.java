// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleSound.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.core.math.PZMath;
import zombie.vehicleSound.VehicleSoundOwner;

public class ParameterVehicleRPM extends FMODLocalParameter {
    private static final float MAX_IDLE_ENGINE_SPEED = 800.0F;
    private static final float MAX_ENGINE_SPEED = 7000.0F;
    private static final float MAX_IDLE_ENGINE_SPEED_MULTIPLIER = 1.1F;
    private final VehicleSoundOwner vehicle;

    public ParameterVehicleRPM(VehicleSoundOwner vehicle) {
        super("VehicleRPM");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        float rpm1 = PZMath.clamp((float)this.vehicle.getEngineSpeed(), 0.0F, 7000.0F);
        float rpmIdle = this.vehicle.getScript().getEngineIdleSpeed();
        float rpmIdleMax = rpmIdle * 1.1F;
        float rpm2;
        if (rpm1 < rpmIdleMax) {
            rpm2 = rpm1 / rpmIdleMax * 800.0F;
        } else {
            rpm2 = 800.0F + (rpm1 - rpmIdleMax) / (7000.0F - rpmIdleMax) * 6200.0F;
        }

        float INCREMENT = 50.0F;
        return PZMath.ceil(rpm2 / 50.0F) * 50.0F;
    }
}
