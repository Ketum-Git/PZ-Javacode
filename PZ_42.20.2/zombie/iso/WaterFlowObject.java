// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

public final class WaterFlowObject implements Comparable<WaterFlowObject> {
    public int x;
    public int y;
    public float radians;
    public float speed;
    public WaterFlowObject kdLeft;
    public WaterFlowObject kdRight;
    public double distSq;

    public WaterFlowObject() {
    }

    public WaterFlowObject(int x, int y, float radians, float speed) {
        this.x = x;
        this.y = y;
        this.radians = radians;
        this.speed = speed;
    }

    public float distSq(WaterFlowObject other) {
        return IsoUtils.DistanceToSquared(this.x, this.y, other.x, other.y);
    }

    public boolean isLeaf() {
        return this.kdLeft == null && this.kdRight == null;
    }

    public int compareTo(WaterFlowObject other) {
        return Double.compare(other.distSq, this.distSq);
    }
}
