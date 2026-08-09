// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;

public final class VehicleCluster {
    public int z;
    public final ArrayList<VehicleRect> rects = new ArrayList<>();
    static final ArrayDeque<VehicleCluster> pool = new ArrayDeque<>();

    VehicleCluster init() {
        this.rects.clear();
        return this;
    }

    void merge(VehicleCluster other) {
        for (int i = 0; i < other.rects.size(); i++) {
            VehicleRect rect = other.rects.get(i);
            rect.cluster = this;
        }

        this.rects.addAll(other.rects);
        other.rects.clear();
    }

    public VehicleRect bounds() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int i = 0; i < this.rects.size(); i++) {
            VehicleRect rect = this.rects.get(i);
            minX = Math.min(minX, rect.left());
            minY = Math.min(minY, rect.top());
            maxX = Math.max(maxX, rect.right());
            maxY = Math.max(maxY, rect.bottom());
        }

        return VehicleRect.alloc().init(minX, minY, maxX - minX, maxY - minY, this.z);
    }

    static VehicleCluster alloc() {
        return pool.isEmpty() ? new VehicleCluster() : pool.pop();
    }

    void release() {
        assert !pool.contains(this);

        pool.push(this);
    }
}
