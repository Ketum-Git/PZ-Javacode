// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import zombie.util.PooledObject;

public final class DoublePoint extends PooledObject {
    public double x;
    public double y;

    public DoublePoint() {
    }

    public DoublePoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public DoublePoint set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public DoublePoint translate(double dx, double dy) {
        return this.set(this.x + dx, this.y + dy);
    }
}
