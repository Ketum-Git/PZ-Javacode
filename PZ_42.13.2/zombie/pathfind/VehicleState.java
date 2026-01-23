// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import org.joml.Vector3f;
import zombie.core.math.PZMath;
import zombie.vehicles.BaseVehicle;

final class VehicleState {
    BaseVehicle vehicle;
    float x;
    float y;
    float z;
    final Vector3f forward = new Vector3f();
    final VehiclePoly polyPlusRadius = new VehiclePoly();
    static final ArrayDeque<VehicleState> pool = new ArrayDeque<>();

    VehicleState init(BaseVehicle vehicle) {
        this.vehicle = vehicle;
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
        vehicle.getForwardVector(this.forward);
        this.polyPlusRadius.init(vehicle.getPolyPlusRadius());
        return this;
    }

    boolean check() {
        boolean changed = this.x != this.vehicle.getX() || this.y != this.vehicle.getY() || PZMath.fastfloor(this.z) != PZMath.fastfloor(this.vehicle.getZ());
        if (!changed) {
            BaseVehicle.Vector3fObjectPool pool = BaseVehicle.TL_vector3f_pool.get();
            Vector3f forward2 = this.vehicle.getForwardVector(pool.alloc());
            changed = this.forward.dot(forward2) < 0.999F;
            if (changed) {
                this.forward.set(forward2);
            }

            pool.release(forward2);
        }

        if (changed) {
            this.x = this.vehicle.getX();
            this.y = this.vehicle.getY();
            this.z = this.vehicle.getZ();
        }

        return changed;
    }

    static VehicleState alloc() {
        return pool.isEmpty() ? new VehicleState() : pool.pop();
    }

    void release() {
        assert !pool.contains(this);

        pool.push(this);
    }
}
