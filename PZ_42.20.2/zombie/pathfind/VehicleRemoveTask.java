// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.vehicles.BaseVehicle;

final class VehicleRemoveTask implements IVehicleTask {
    PolygonalMap2 map;
    BaseVehicle vehicle;
    static final ArrayDeque<VehicleRemoveTask> pool = new ArrayDeque<>();

    @Override
    public void init(PolygonalMap2 map, BaseVehicle vehicle) {
        this.map = map;
        this.vehicle = vehicle;
    }

    @Override
    public void execute() {
        Vehicle vehicle = this.map.vehicleMap.remove(this.vehicle);
        if (vehicle != null) {
            this.map.vehicles.remove(vehicle);
            vehicle.release();
        }

        this.vehicle = null;
    }

    static VehicleRemoveTask alloc() {
        synchronized (pool) {
            return pool.isEmpty() ? new VehicleRemoveTask() : pool.pop();
        }
    }

    @Override
    public void release() {
        synchronized (pool) {
            assert !pool.contains(this);

            pool.push(this);
        }
    }
}
