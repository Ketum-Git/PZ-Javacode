// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.nativeCode;

import zombie.pathfind.VehiclePoly;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;

class VehicleState {
    BaseVehicle vehicle;
    final VehiclePoly poly = new VehiclePoly();
    static final ObjectPool<VehicleState> pool = new ObjectPool<>(VehicleState::new);

    VehicleState init(BaseVehicle vehicle) {
        this.vehicle = vehicle;
        this.poly.init(vehicle.getPolyPlusRadius());
        return this;
    }

    boolean check() {
        if (!this.poly.isEqual(this.vehicle.getPolyPlusRadius())) {
            this.poly.init(this.vehicle.getPolyPlusRadius());
            return true;
        } else {
            return false;
        }
    }

    static VehicleState alloc() {
        return pool.alloc();
    }

    void release() {
        pool.release(this);
    }
}
