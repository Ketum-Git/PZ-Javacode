// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import zombie.core.math.PZMath;

public final class VehicleRect {
    VehicleCluster cluster;
    Vehicle vehicle;
    public int x;
    public int y;
    public int w;
    public int h;
    public int z;
    static final ArrayDeque<VehicleRect> pool = new ArrayDeque<>();

    VehicleRect init(Vehicle vehicle, int x, int y, int w, int h, int z) {
        this.cluster = null;
        this.vehicle = vehicle;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.z = z;
        return this;
    }

    VehicleRect init(int x, int y, int w, int h, int z) {
        this.cluster = null;
        this.vehicle = null;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.z = z;
        return this;
    }

    public int left() {
        return this.x;
    }

    public int top() {
        return this.y;
    }

    public int right() {
        return this.x + this.w;
    }

    public int bottom() {
        return this.y + this.h;
    }

    boolean containsPoint(float x, float y, float z) {
        return x >= this.left() && x < this.right() && y >= this.top() && y < this.bottom() && PZMath.fastfloor(z) == this.z;
    }

    boolean containsPoint(float x, float y, float z, int expand) {
        int left = this.x - expand;
        int top = this.y - expand;
        int right = this.right() + expand;
        int bottom = this.bottom() + expand;
        return x >= left && x < right && y >= top && y < bottom && PZMath.fastfloor(z) == this.z;
    }

    boolean intersects(VehicleRect other) {
        return this.left() < other.right() && this.right() > other.left() && this.top() < other.bottom() && this.bottom() > other.top();
    }

    boolean intersects(int x1, int y1, int x2, int y2, int expand) {
        return this.left() - expand < x2 && this.right() + expand > x1 && this.top() - expand < y2 && this.bottom() + expand > y1;
    }

    boolean isAdjacent(VehicleRect other) {
        this.x--;
        this.y--;
        this.w += 2;
        this.h += 2;
        boolean adjacent = this.intersects(other);
        this.x++;
        this.y++;
        this.w -= 2;
        this.h -= 2;
        return adjacent;
    }

    static VehicleRect alloc() {
        if (pool.isEmpty()) {
            boolean var0 = false;
        } else {
            boolean var1 = false;
        }

        return pool.isEmpty() ? new VehicleRect() : pool.pop();
    }

    public void release() {
        assert !pool.contains(this);

        pool.push(this);
    }
}
