// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.vehicles.BaseVehicle;

final class VehicleUpdateTask implements IVehicleTask {
    PolygonalMap2 map;
    BaseVehicle vehicle;
    final VehiclePoly poly = new VehiclePoly();
    final VehiclePoly polyPlusRadius = new VehiclePoly();
    float upVectorDot;
    static final ArrayDeque<VehicleUpdateTask> pool = new ArrayDeque<>();

    @Override
    public void init(PolygonalMap2 map, BaseVehicle vehicle) {
        this.map = map;
        this.vehicle = vehicle;
        this.poly.init(vehicle.getPoly());
        this.poly.z += 32.0F;
        this.polyPlusRadius.init(vehicle.getPolyPlusRadius());
        this.polyPlusRadius.z += 32.0F;
        this.upVectorDot = vehicle.getUpVectorDot();
    }

    @Override
    public void execute() {
        Vehicle vehicle = this.map.vehicleMap.get(this.vehicle);
        vehicle.poly.init(this.poly);
        vehicle.polyPlusRadius.init(this.polyPlusRadius);
        vehicle.upVectorDot = this.upVectorDot;
        this.vehicle = null;
    }

    static VehicleUpdateTask alloc() {
        synchronized (pool) {
            return pool.isEmpty() ? new VehicleUpdateTask() : pool.pop();
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
