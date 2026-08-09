// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.core.math.PZMath;

public final class Node {
    static int nextID = 1;
    final int id;
    public float x;
    public float y;
    public int z;
    boolean ignore;
    public Square square;
    public ArrayList<VisibilityGraph> graphs;
    public final ArrayList<Edge> edges = new ArrayList<>();
    public final ArrayList<Connection> visible = new ArrayList<>();
    int flags;
    static final ArrayList<Obstacle> tempObstacles = new ArrayList<>();
    static final ArrayDeque<Node> pool = new ArrayDeque<>();

    Node() {
        this.id = nextID++;
    }

    public Node init(float x, float y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.ignore = false;
        this.square = null;
        if (this.graphs != null) {
            this.graphs.clear();
        }

        this.edges.clear();
        this.visible.clear();
        this.flags = 0;
        return this;
    }

    Node init(Square square) {
        this.x = square.x + 0.5F;
        this.y = square.y + 0.5F;
        this.z = square.z;
        this.ignore = false;
        this.square = square;
        if (this.graphs != null) {
            this.graphs.clear();
        }

        this.edges.clear();
        this.visible.clear();
        this.flags = 0;
        return this;
    }

    Node setXY(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    void addGraph(VisibilityGraph graph) {
        if (this.graphs == null) {
            this.graphs = new ArrayList<>();
        }

        assert !this.graphs.contains(graph);

        this.graphs.add(graph);
    }

    boolean sharesEdge(Node other) {
        for (int i = 0; i < this.edges.size(); i++) {
            Edge edge = this.edges.get(i);
            if (edge.hasNode(other)) {
                return true;
            }
        }

        return false;
    }

    boolean sharesShape(Node other) {
        for (int i = 0; i < this.edges.size(); i++) {
            Edge edge = this.edges.get(i);

            for (int j = 0; j < other.edges.size(); j++) {
                Edge edgeOther = other.edges.get(j);
                if (edge.obstacle != null && edge.obstacle == edgeOther.obstacle) {
                    return true;
                }
            }
        }

        return false;
    }

    void createGraphsIfNeeded() {
        if (this.graphs != null) {
            for (int i = 0; i < this.graphs.size(); i++) {
                VisibilityGraph graph = this.graphs.get(i);
                if (!graph.created) {
                    graph.create();
                }
            }
        }
    }

    void getObstacles(ArrayList<Obstacle> obstacles) {
        for (int i = 0; i < this.edges.size(); i++) {
            Edge edge = this.edges.get(i);
            if (!obstacles.contains(edge.obstacle)) {
                obstacles.add(edge.obstacle);
            }
        }
    }

    boolean onSameShapeButDoesNotShareAnEdge(Node other) {
        tempObstacles.clear();
        this.getObstacles(tempObstacles);

        for (int i = 0; i < tempObstacles.size(); i++) {
            Obstacle obstacle = tempObstacles.get(i);
            if (obstacle.hasNode(other) && !obstacle.hasAdjacentNodes(this, other)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasFlag(int flag) {
        return (this.flags & flag) != 0;
    }

    boolean isConnectedTo(Node other) {
        if (this.hasFlag(4)) {
            return true;
        } else {
            for (int i = 0; i < this.visible.size(); i++) {
                Connection cxn = this.visible.get(i);
                if (cxn.node1 == other || cxn.node2 == other) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isOnEdgeOfLoadedArea() {
        int x = PZMath.fastfloor(this.x);
        int y = PZMath.fastfloor(this.y);
        if (PZMath.coordmodulo(x, 8) == 0 && PolygonalMap2.instance.getChunkFromSquarePos(x - 1, y) == null) {
            return true;
        } else if (PZMath.coordmodulo(x, 8) == 7 && PolygonalMap2.instance.getChunkFromSquarePos(x + 1, y) == null) {
            return true;
        } else {
            return PZMath.coordmodulo(y, 8) == 0 && PolygonalMap2.instance.getChunkFromSquarePos(x, y - 1) == null
                ? true
                : PZMath.coordmodulo(y, 8) == 7 && PolygonalMap2.instance.getChunkFromSquarePos(x, y + 1) == null;
        }
    }

    public static Node alloc() {
        if (pool.isEmpty()) {
            boolean var0 = false;
        } else {
            boolean var1 = false;
        }

        return pool.isEmpty() ? new Node() : pool.pop();
    }

    public void release() {
        assert !pool.contains(this);

        for (int i = this.visible.size() - 1; i >= 0; i--) {
            PolygonalMap2.instance.breakConnection(this.visible.get(i));
        }

        pool.push(this);
    }

    static void releaseAll(ArrayList<Node> objs) {
        for (int i = 0; i < objs.size(); i++) {
            objs.get(i).release();
        }
    }
}
