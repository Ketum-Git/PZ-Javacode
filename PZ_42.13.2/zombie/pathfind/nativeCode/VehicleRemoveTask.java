// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.nativeCode;

import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;

class VehicleRemoveTask implements IPathfindTask {
    short vehicleId;
    static final ObjectPool<VehicleRemoveTask> pool = new ObjectPool<>(VehicleRemoveTask::new);

    public VehicleRemoveTask init(BaseVehicle vehicle) {
        this.vehicleId = vehicle.vehicleId;

        assert this.vehicleId != -1;

        return this;
    }

    @Override
    public void execute() {
        PathfindNative.removeVehicle(this.vehicleId);
    }

    static VehicleRemoveTask alloc() {
        return pool.alloc();
    }

    @Override
    public void release() {
        pool.release(this);
    }
}
