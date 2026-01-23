// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.utils.triangulation;

public class EdgeDistancePack implements Comparable<EdgeDistancePack> {
    public Edge2D edge;
    public double distance;

    public EdgeDistancePack(Edge2D edge, double distance) {
        this.edge = edge;
        this.distance = distance;
    }

    public int compareTo(EdgeDistancePack o) {
        return Double.compare(this.distance, o.distance);
    }
}
