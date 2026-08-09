// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import org.joml.Vector3f;
import zombie.debug.LineDrawer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;

public final class Obstacle {
    Vehicle vehicle;
    final EdgeRing outer = new EdgeRing();
    final ArrayList<EdgeRing> inner = new ArrayList<>();
    ImmutableRectF bounds;
    Node nodeCrawlFront;
    Node nodeCrawlRear;
    final ArrayList<Node> crawlNodes = new ArrayList<>();
    static final ArrayDeque<Obstacle> pool = new ArrayDeque<>();

    Obstacle init(Vehicle vehicle) {
        this.vehicle = vehicle;
        this.outer.clear();
        this.inner.clear();
        this.nodeCrawlFront = this.nodeCrawlRear = null;
        this.crawlNodes.clear();
        return this;
    }

    Obstacle init(IsoGridSquare square) {
        this.vehicle = null;
        this.outer.clear();
        this.inner.clear();
        this.nodeCrawlFront = this.nodeCrawlRear = null;
        this.crawlNodes.clear();
        return this;
    }

    boolean hasNode(Node node) {
        if (this.outer.hasNode(node)) {
            return true;
        } else {
            for (int i = 0; i < this.inner.size(); i++) {
                EdgeRing edges = this.inner.get(i);
                if (edges.hasNode(node)) {
                    return true;
                }
            }

            return false;
        }
    }

    boolean hasAdjacentNodes(Node node1, Node node2) {
        if (this.outer.hasAdjacentNodes(node1, node2)) {
            return true;
        } else {
            for (int i = 0; i < this.inner.size(); i++) {
                EdgeRing edges = this.inner.get(i);
                if (edges.hasAdjacentNodes(node1, node2)) {
                    return true;
                }
            }

            return false;
        }
    }

    boolean isPointInside(float x, float y, int flags) {
        if (this.outer.isPointInPolygon_WindingNumber(x, y, flags) != EdgeRingHit.Inside) {
            return false;
        } else if (this.inner.isEmpty()) {
            return true;
        } else {
            for (int i = 0; i < this.inner.size(); i++) {
                EdgeRing edges = this.inner.get(i);
                if (edges.isPointInPolygon_WindingNumber(x, y, flags) != EdgeRingHit.Outside) {
                    return false;
                }
            }

            return true;
        }
    }

    boolean isPointInside(float x, float y) {
        int flags = 0;
        return this.isPointInside(x, y, 0);
    }

    boolean lineSegmentIntersects(float sx, float sy, float ex, float ey) {
        if (this.outer.lineSegmentIntersects(sx, sy, ex, ey)) {
            return true;
        } else {
            for (int i = 0; i < this.inner.size(); i++) {
                EdgeRing edges = this.inner.get(i);
                if (edges.lineSegmentIntersects(sx, sy, ex, ey)) {
                    return true;
                }
            }

            return false;
        }
    }

    boolean isNodeInsideOf(Node node) {
        if (this.hasNode(node)) {
            return false;
        } else {
            return !this.bounds.containsPoint(node.x, node.y) ? false : this.isPointInside(node.x, node.y);
        }
    }

    void getClosestPointOnEdge(float x3, float y3, ClosestPointOnEdge out) {
        out.edge = null;
        out.node = null;
        out.distSq = Double.MAX_VALUE;
        this.outer.getClosestPointOnEdge(x3, y3, out);

        for (int i = 0; i < this.inner.size(); i++) {
            EdgeRing edges = this.inner.get(i);
            edges.getClosestPointOnEdge(x3, y3, out);
        }
    }

    boolean splitEdgeAtNearestPoint(ClosestPointOnEdge closestPointOnEdge, int z, AdjustStartEndNodeData adjust) {
        if (closestPointOnEdge.edge == null) {
            return false;
        } else {
            adjust.obstacle = this;
            if (closestPointOnEdge.node == null) {
                adjust.node = Node.alloc().init(closestPointOnEdge.point.x, closestPointOnEdge.point.y, z);
                adjust.newEdge = closestPointOnEdge.edge.split(adjust.node);
                adjust.isNodeNew = true;
            } else {
                adjust.node = closestPointOnEdge.node;
                adjust.newEdge = null;
                adjust.isNodeNew = false;
            }

            return true;
        }
    }

    public void unsplit(Node nodeSplit, ArrayList<Edge> edges) {
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            if (edge.node1 == nodeSplit) {
                if (i > 0) {
                    Edge edgePrev = edges.get(i - 1);
                    edgePrev.node2 = edge.node2;

                    assert edge.node2.edges.contains(edge);

                    edge.node2.edges.remove(edge);

                    assert !edge.node2.edges.contains(edgePrev);

                    edge.node2.edges.add(edgePrev);
                    PolygonalMap2.instance.connectTwoNodes(edgePrev.node1, edgePrev.node2);
                } else {
                    edges.get(i + 1).node1 = edges.get(edges.size() - 1).node2;
                }

                edge.release();
                edges.remove(i);
                break;
            }
        }
    }

    void calcBounds() {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (int i = 0; i < this.outer.size(); i++) {
            Edge edge = this.outer.get(i);
            minX = Math.min(minX, edge.node1.x);
            minY = Math.min(minY, edge.node1.y);
            maxX = Math.max(maxX, edge.node1.x);
            maxY = Math.max(maxY, edge.node1.y);
        }

        if (this.bounds != null) {
            this.bounds.release();
        }

        float epsilon = 0.01F;
        this.bounds = ImmutableRectF.alloc().init(minX - 0.01F, minY - 0.01F, maxX - minX + 0.02F, maxY - minY + 0.02F);
    }

    void render(ArrayList<Edge> edges, boolean outer) {
        if (!edges.isEmpty()) {
            float r = 0.0F;
            float g = outer ? 1.0F : 0.5F;
            float b = outer ? 0.0F : 0.5F;

            for (Edge edge : edges) {
                Node n1 = edge.node1;
                Node n2 = edge.node2;
                LineDrawer.addLine(n1.x, n1.y, n1.z, n2.x, n2.y, n2.z, r, g, b, null, true);
                Vector3f edgeVec = new Vector3f(n2.x - n1.x, n2.y - n1.y, n2.z - n1.z).normalize();
                Vector3f perp = new Vector3f(edgeVec).cross(0.0F, 0.0F, 1.0F).normalize();
                edgeVec.mul(0.9F);
                LineDrawer.addLine(
                    n2.x - edgeVec.x * 0.1F - perp.x * 0.1F, n2.y - edgeVec.y * 0.1F - perp.y * 0.1F, n2.z, n2.x, n2.y, n2.z, r, g, b, null, true
                );
                LineDrawer.addLine(
                    n2.x - edgeVec.x * 0.1F + perp.x * 0.1F, n2.y - edgeVec.y * 0.1F + perp.y * 0.1F, n2.z, n2.x, n2.y, n2.z, r, g, b, null, true
                );
                r = 1.0F - r;
            }

            Node node1 = edges.get(0).node1;
            LineDrawer.addLine(node1.x - 0.1F, node1.y - 0.1F, node1.z, node1.x + 0.1F, node1.y + 0.1F, node1.z, 1.0F, 0.0F, 0.0F, null, false);
        }
    }

    void render() {
        this.render(this.outer, true);

        for (int i = 0; i < this.inner.size(); i++) {
            this.render(this.inner.get(i), false);
        }
    }

    void connectCrawlNodes(VisibilityGraph graph, Obstacle other) {
        this.connectCrawlNode(graph, other, this.nodeCrawlFront, other.nodeCrawlFront);
        this.connectCrawlNode(graph, other, this.nodeCrawlFront, other.nodeCrawlRear);
        this.connectCrawlNode(graph, other, this.nodeCrawlRear, other.nodeCrawlFront);
        this.connectCrawlNode(graph, other, this.nodeCrawlRear, other.nodeCrawlRear);

        for (int i = 0; i < this.crawlNodes.size(); i += 3) {
            Node nodeLeft = this.crawlNodes.get(i);
            Node nodeRight = this.crawlNodes.get(i + 2);

            for (int j = 0; j < other.crawlNodes.size(); j += 3) {
                Node nodeLeft2 = other.crawlNodes.get(j);
                Node nodeRight2 = other.crawlNodes.get(j + 2);
                this.connectCrawlNode(graph, other, nodeLeft, nodeLeft2);
                this.connectCrawlNode(graph, other, nodeLeft, nodeRight2);
                this.connectCrawlNode(graph, other, nodeRight, nodeLeft2);
                this.connectCrawlNode(graph, other, nodeRight, nodeRight2);
            }
        }
    }

    void connectCrawlNode(VisibilityGraph graph, Obstacle other, Node node1, Node node2) {
        if (this.isNodeInsideOf(node2)) {
            node2.flags |= 2;
            node1 = this.getClosestInteriorCrawlNode(node2.x, node2.y);
            if (node1 != null) {
                if (!node1.isConnectedTo(node2)) {
                    PolygonalMap2.instance.connectTwoNodes(node1, node2);
                }
            }
        } else if (!node1.ignore && !node2.ignore) {
            if (!node1.isConnectedTo(node2)) {
                if (graph.isVisible(node1, node2)) {
                    PolygonalMap2.instance.connectTwoNodes(node1, node2);
                }
            }
        }
    }

    Node getClosestInteriorCrawlNode(float x, float y) {
        Node closest = null;
        float closestDistSq = Float.MAX_VALUE;

        for (int i = 0; i < this.crawlNodes.size(); i += 3) {
            Node nodeMid = this.crawlNodes.get(i + 1);
            float distSq = IsoUtils.DistanceToSquared(nodeMid.x, nodeMid.y, x, y);
            if (distSq < closestDistSq) {
                closest = nodeMid;
                closestDistSq = distSq;
            }
        }

        return closest;
    }

    static Obstacle alloc() {
        return pool.isEmpty() ? new Obstacle() : pool.pop();
    }

    void release() {
        assert !pool.contains(this);

        this.outer.release();
        this.outer.clear();
        EdgeRing.releaseAll(this.inner);
        this.inner.clear();
        pool.push(this);
    }

    static void releaseAll(ArrayList<Obstacle> objs) {
        for (int i = 0; i < objs.size(); i++) {
            objs.get(i).release();
        }
    }
}
