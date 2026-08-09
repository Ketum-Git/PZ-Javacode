// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class ParameterVehicleTireMissing extends FMODLocalParameter {
    private final BaseVehicle vehicle;

    public ParameterVehicleTireMissing(BaseVehicle vehicle) {
        super("VehicleTireMissing");
        this.vehicle = vehicle;
    }

    @Override
    public float calculateCurrentValue() {
        boolean bMissing = false;
        VehicleScript script = this.vehicle.getScript();
        if (script != null) {
            for (int i = 0; i < script.getWheelCount(); i++) {
                VehicleScript.Wheel scriptWheel = script.getWheel(i);
                VehiclePart part = this.vehicle.getPartById("Tire" + scriptWheel.getId());
                if (part == null || part.getInventoryItem() == null) {
                    bMissing = true;
                    break;
                }
            }
        }

        return bMissing ? 1.0F : 0.0F;
    }
}
