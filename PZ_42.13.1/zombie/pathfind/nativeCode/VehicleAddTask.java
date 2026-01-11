// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.nativeCode;

import gnu.trove.list.array.TFloatArrayList;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.system.MemoryUtil;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;

class VehicleAddTask implements IPathfindTask {
    ByteBuffer bb;
    static final ObjectPool<VehicleAddTask> pool = new ObjectPool<>(VehicleAddTask::new);

    public VehicleAddTask init(BaseVehicle vehicle) {
        this.bb = MemoryUtil.memAlloc(256).order(ByteOrder.BIG_ENDIAN);
        this.bb.clear();

        assert vehicle.vehicleId != -1;

        this.bb.putInt(vehicle.vehicleId);
        vehicle.getPoly().toByteBuffer(this.bb);
        vehicle.getPolyPlusRadius().toByteBuffer(this.bb);
        this.bb.putFloat(vehicle.getUpVectorDot());
        TFloatArrayList crawlOffsets = vehicle.getScript().getCrawlOffsets();
        this.bb.put((byte)crawlOffsets.size());

        for (int i = 0; i < crawlOffsets.size(); i++) {
            this.bb.putFloat(crawlOffsets.get(i));
        }

        return this;
    }

    @Override
    public void execute() {
        PathfindNative.addVehicle(this.bb);
    }

    static VehicleAddTask alloc() {
        return pool.alloc();
    }

    @Override
    public void release() {
        try {
            MemoryUtil.memFree(this.bb);
        } catch (IllegalArgumentException var2) {
        }

        this.bb = null;
        pool.release(this);
    }
}
