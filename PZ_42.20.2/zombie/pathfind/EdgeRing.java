// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.iso.Vector2;

final class EdgeRing extends ArrayList<Edge> {
    static final ArrayDeque<EdgeRing> pool = new ArrayDeque<>();

    public boolean add(Edge obj) {
        assert !this.contains(obj);

        return super.add(obj);
    }

    public boolean hasNode(Node node) {
        for (int i = 0; i < this.size(); i++) {
            Edge edge = this.get(i);
            if (edge.hasNode(node)) {
                return true;
            }
        }

        return false;
    }

    boolean hasAdjacentNodes(Node node1, Node node2) {
        for (int i = 0; i < this.size(); i++) {
            Edge edge = this.get(i);
            if (edge.hasNode(node1) && edge.hasNode(node2)) {
                return true;
            }
        }

        return false;
    }

    boolean isPointInPolygon_CrossingNumber(float x, float y) {
        int cn = 0;

        for (int i = 0; i < this.size(); i++) {
            Edge edge = this.get(i);
            if (edge.node1.y <= y && edge.node2.y > y || edge.node1.y > y && edge.node2.y <= y) {
                float vt = (y - edge.node1.y) / (edge.node2.y - edge.node1.y);
                if (x < edge.node1.x + vt * (edge.node2.x - edge.node1.x)) {
                    cn++;
                }
            }
        }

        return cn % 2 == 1;
    }

    float isLeft(float x0, float y0, float x1, float y1, float x2, float y2) {
        return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
    }

    EdgeRingHit isPointInPolygon_WindingNumber(float x, float y, int flags) {
        int wn = 0;

        for (int i = 0; i < this.size(); i++) {
            Edge edge = this.get(i);
            if ((flags & 16) != 0 && edge.isPointOn(x, y)) {
                return EdgeRingHit.OnEdge;
            }

            if (edge.node1.y <= y) {
                if (edge.node2.y > y && this.isLeft(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y, x, y) > 0.0F) {
                    wn++;
                }
            } else if (edge.node2.y <= y && this.isLeft(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y, x, y) < 0.0F) {
                wn--;
            }
        }

        return wn == 0 ? EdgeRingHit.Outside : EdgeRingHit.Inside;
    }

    boolean lineSegmentIntersects(float sx, float sy, float ex, float ey) {
        Vector2 move = L_lineSegmentIntersects.v1;
        move.set(ex - sx, ey - sy);
        float lineSegmentLength = move.getLength();
        move.normalize();
        float dirX = move.x;
        float dirY = move.y;

        for (int j = 0; j < this.size(); j++) {
            Edge edge = this.get(j);
            if (!edge.isPointOn(sx, sy) && !edge.isPointOn(ex, ey)) {
                float dot = edge.normal.dot(move);
                if (dot >= 0.01F) {
                }

                float aX = edge.node1.x;
                float aY = edge.node1.y;
                float bX = edge.node2.x;
                float bY = edge.node2.y;
                float doaX = sx - aX;
                float doaY = sy - aY;
                float dbaX = bX - aX;
                float dbaY = bY - aY;
                float invDbaDir = 1.0F / (dbaY * dirX - dbaX * dirY);
                float t = (dbaX * doaY - dbaY * doaX) * invDbaDir;
                if (t >= 0.0F && t <= lineSegmentLength) {
                    float t2 = (doaY * dirX - doaX * dirY) * invDbaDir;
                    if (t2 >= 0.0F && t2 <= 1.0F) {
                        return true;
                    }
                }
            }
        }

        return this.isPointInPolygon_WindingNumber((sx + ex) / 2.0F, (sy + ey) / 2.0F, 0) != EdgeRingHit.Outside;
    }

    void getClosestPointOnEdge(float x3, float y3, ClosestPointOnEdge out) {
        for (int i = 0; i < this.size(); i++) {
            Edge edge = this.get(i);
            edge.getClosestPointOnEdge(x3, y3, out);
        }
    }

    static EdgeRing alloc() {
        return pool.isEmpty() ? new EdgeRing() : pool.pop();
    }

    public void release() {
        Edge.releaseAll(this);
    }

    static void releaseAll(ArrayList<EdgeRing> objs) {
        for (int i = 0; i < objs.size(); i++) {
            objs.get(i).release();
        }
    }
}
