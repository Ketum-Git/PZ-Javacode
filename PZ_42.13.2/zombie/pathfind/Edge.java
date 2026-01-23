// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.iso.Vector2;

public final class Edge {
    public Node node1;
    public Node node2;
    public Obstacle obstacle;
    public EdgeRing edgeRing;
    final ArrayList<Intersection> intersections = new ArrayList<>();
    final Vector2 normal = new Vector2();
    static final ArrayDeque<Edge> pool = new ArrayDeque<>();

    Edge init(Node node1, Node node2, Obstacle obstacle, EdgeRing edges) {
        if (node1 == null) {
            boolean var5 = true;
        }

        this.node1 = node1;
        this.node2 = node2;
        node1.edges.add(this);
        node2.edges.add(this);
        this.obstacle = obstacle;
        this.edgeRing = edges;
        this.intersections.clear();
        this.normal.set(node2.x - node1.x, node2.y - node1.y);
        this.normal.normalize();
        this.normal.rotate((float) (Math.PI / 2));
        return this;
    }

    boolean hasNode(Node node) {
        return node == this.node1 || node == this.node2;
    }

    void getClosestPointOnEdge(float x3, float y3, ClosestPointOnEdge out) {
        if (this.node1.isConnectedTo(this.node2)) {
            float x1 = this.node1.x;
            float y1 = this.node1.y;
            float x2 = this.node2.x;
            float y2 = this.node2.y;
            double u = ((x3 - x1) * (x2 - x1) + (y3 - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
            double xu = x1 + u * (x2 - x1);
            double yu = y1 + u * (y2 - y1);
            Node node = null;
            if (u <= 0.0) {
                xu = x1;
                yu = y1;
                node = this.node1;
            } else if (u >= 1.0) {
                xu = x2;
                yu = y2;
                node = this.node2;
            }

            double distSq = (x3 - xu) * (x3 - xu) + (y3 - yu) * (y3 - yu);
            if (distSq < out.distSq) {
                out.point.set((float)xu, (float)yu);
                out.distSq = distSq;
                out.edge = this;
                out.node = node;
            }
        }
    }

    boolean isPointOn(float x3, float y3) {
        if (!this.node1.isConnectedTo(this.node2)) {
            return false;
        } else {
            float x1 = this.node1.x;
            float y1 = this.node1.y;
            float x2 = this.node2.x;
            float y2 = this.node2.y;
            double u = ((x3 - x1) * (x2 - x1) + (y3 - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
            double xu = x1 + u * (x2 - x1);
            double yu = y1 + u * (y2 - y1);
            if (u <= 0.0) {
                xu = x1;
                yu = y1;
            } else if (u >= 1.0) {
                xu = x2;
                yu = y2;
            }

            double distSq = (x3 - xu) * (x3 - xu) + (y3 - yu) * (y3 - yu);
            return distSq < 1.0E-6;
        }
    }

    Edge split(Node nodeSplit) {
        Edge edgeNew = alloc().init(nodeSplit, this.node2, this.obstacle, this.edgeRing);
        this.edgeRing.add(this.edgeRing.indexOf(this) + 1, edgeNew);
        PolygonalMap2.instance.breakConnection(this.node1, this.node2);
        this.node2.edges.remove(this);
        this.node2 = nodeSplit;
        this.node2.edges.add(this);
        return edgeNew;
    }

    static Edge alloc() {
        return pool.isEmpty() ? new Edge() : pool.pop();
    }

    void release() {
        assert !pool.contains(this);

        this.node1 = null;
        this.node2 = null;
        this.obstacle = null;
        this.edgeRing = null;
        this.intersections.clear();
        pool.push(this);
    }

    static void releaseAll(ArrayList<Edge> objs) {
        for (int i = 0; i < objs.size(); i++) {
            objs.get(i).release();
        }
    }
}
