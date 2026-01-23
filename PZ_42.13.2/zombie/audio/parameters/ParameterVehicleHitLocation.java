// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import org.joml.Vector3f;
import zombie.audio.FMODLocalParameter;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;

public class ParameterVehicleHitLocation extends FMODLocalParameter {
    private ParameterVehicleHitLocation.HitLocation location = ParameterVehicleHitLocation.HitLocation.Front;

    public ParameterVehicleHitLocation() {
        super("VehicleHitLocation");
    }

    @Override
    public float calculateCurrentValue() {
        return this.location.label;
    }

    public static ParameterVehicleHitLocation.HitLocation calculateLocation(BaseVehicle vehicle, float x, float y, float z) {
        VehicleScript script = vehicle.getScript();
        if (script == null) {
            return ParameterVehicleHitLocation.HitLocation.Front;
        } else {
            Vector3f chrPos = vehicle.getLocalPos(x, y, z, BaseVehicle.TL_vector3f_pool.get().alloc());
            Vector3f ext = script.getExtents();
            Vector3f com = script.getCenterOfMassOffset();
            float yMin = com.z - ext.z / 2.0F;
            float yMax = com.z + ext.z / 2.0F;
            yMin *= 0.9F;
            yMax *= 0.9F;
            ParameterVehicleHitLocation.HitLocation position;
            if (chrPos.z >= yMin && chrPos.z <= yMax) {
                position = ParameterVehicleHitLocation.HitLocation.Side;
            } else if (chrPos.z > 0.0F) {
                position = ParameterVehicleHitLocation.HitLocation.Front;
            } else {
                position = ParameterVehicleHitLocation.HitLocation.Rear;
            }

            BaseVehicle.TL_vector3f_pool.get().release(chrPos);
            return position;
        }
    }

    public void setLocation(ParameterVehicleHitLocation.HitLocation location) {
        this.location = location;
    }

    public static enum HitLocation {
        Front(0),
        Rear(1),
        Side(2);

        final int label;

        private HitLocation(final int label) {
            this.label = label;
        }

        public int getValue() {
            return this.label;
        }
    }
}
