// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayDeque;
import zombie.vehicles.BaseVehicle;

final class VehicleAddTask implements IVehicleTask {
    PolygonalMap2 map;
    BaseVehicle vehicle;
    final VehiclePoly poly = new VehiclePoly();
    final VehiclePoly polyPlusRadius = new VehiclePoly();
    final TFloatArrayList crawlOffsets = new TFloatArrayList();
    float upVectorDot;
    static final ArrayDeque<VehicleAddTask> pool = new ArrayDeque<>();

    @Override
    public void init(PolygonalMap2 map, BaseVehicle vehicle) {
        this.map = map;
        this.vehicle = vehicle;
        this.poly.init(vehicle.getPoly());
        this.poly.z += 32.0F;
        this.polyPlusRadius.init(vehicle.getPolyPlusRadius());
        this.polyPlusRadius.z += 32.0F;
        this.crawlOffsets.resetQuick();
        this.crawlOffsets.addAll(vehicle.getScript().getCrawlOffsets());
        this.upVectorDot = vehicle.getUpVectorDot();
    }

    @Override
    public void execute() {
        Vehicle vehicle = Vehicle.alloc();
        vehicle.poly.init(this.poly);
        vehicle.polyPlusRadius.init(this.polyPlusRadius);
        vehicle.crawlOffsets.resetQuick();
        vehicle.crawlOffsets.addAll(this.crawlOffsets);
        vehicle.upVectorDot = this.upVectorDot;
        this.map.vehicles.add(vehicle);
        this.map.vehicleMap.put(this.vehicle, vehicle);
        this.vehicle = null;
    }

    static VehicleAddTask alloc() {
        synchronized (pool) {
            return pool.isEmpty() ? new VehicleAddTask() : pool.pop();
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
