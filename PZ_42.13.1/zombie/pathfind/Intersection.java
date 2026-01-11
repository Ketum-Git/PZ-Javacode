// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

final class Intersection {
    Edge edge1;
    Edge edge2;
    float dist1;
    float dist2;
    Node nodeSplit;

    Intersection(Edge edge1, Edge edge2, float dist1, float dist2, float x, float y) {
        this.edge1 = edge1;
        this.edge2 = edge2;
        this.dist1 = dist1;
        this.dist2 = dist2;
        this.nodeSplit = Node.alloc().init(x, y, edge1.node1.z);
    }

    Intersection(Edge edge1, Edge edge2, float dist1, float dist2, Node nodeSplit) {
        this.edge1 = edge1;
        this.edge2 = edge2;
        this.dist1 = dist1;
        this.dist2 = dist2;
        this.nodeSplit = nodeSplit;
    }

    Edge split(Edge edge) {
        return edge.split(this.nodeSplit);
    }
}
