// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import gnu.trove.list.array.TIntArrayList;
import java.awt.geom.Line2D;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.joml.Vector2f;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;

public final class VisibilityGraph {
    boolean created;
    public VehicleCluster cluster;
    final ArrayList<Node> nodes = new ArrayList<>();
    public final ArrayList<Edge> edges = new ArrayList<>();
    final ArrayList<Obstacle> obstacles = new ArrayList<>();
    final ArrayList<Node> intersectNodes = new ArrayList<>();
    public final ArrayList<Node> perimeterNodes = new ArrayList<>();
    public final ArrayList<Edge> perimeterEdges = new ArrayList<>();
    final ArrayList<Node> obstacleTraceNodes = new ArrayList<>();
    final ArrayList<Chunk> overlappedChunks = new ArrayList<>();
    final TIntArrayList splitXy = new TIntArrayList();
    static final VisibilityGraph.CompareIntersection comparator = new VisibilityGraph.CompareIntersection();
    private static final ClusterOutlineGrid clusterOutlineGrid = new ClusterOutlineGrid();
    private static final ArrayDeque<VisibilityGraph> pool = new ArrayDeque<>();

    VisibilityGraph init(VehicleCluster cluster) {
        this.created = false;
        this.cluster = cluster;
        this.edges.clear();
        this.nodes.clear();
        this.obstacles.clear();
        this.intersectNodes.clear();
        this.perimeterEdges.clear();
        this.perimeterNodes.clear();
        return this;
    }

    public boolean isCreated() {
        return this.created;
    }

    void addEdgesForVehicle(Vehicle vehicle) {
        VehiclePoly poly = vehicle.polyPlusRadius;
        int z = PZMath.fastfloor(poly.z);
        Node nodeFrontRight = Node.alloc().init(poly.x1, poly.y1, z);
        Node nodeFrontLeft = Node.alloc().init(poly.x2, poly.y2, z);
        Node nodeRearLeft = Node.alloc().init(poly.x3, poly.y3, z);
        Node nodeRearRight = Node.alloc().init(poly.x4, poly.y4, z);
        Obstacle obstacle = Obstacle.alloc().init(vehicle);
        this.obstacles.add(obstacle);
        Edge edgeFront = Edge.alloc().init(nodeFrontRight, nodeFrontLeft, obstacle, obstacle.outer);
        Edge edgeLeft = Edge.alloc().init(nodeFrontLeft, nodeRearLeft, obstacle, obstacle.outer);
        Edge edgeRear = Edge.alloc().init(nodeRearLeft, nodeRearRight, obstacle, obstacle.outer);
        Edge edgeRight = Edge.alloc().init(nodeRearRight, nodeFrontRight, obstacle, obstacle.outer);
        obstacle.outer.add(edgeFront);
        obstacle.outer.add(edgeLeft);
        obstacle.outer.add(edgeRear);
        obstacle.outer.add(edgeRight);
        obstacle.calcBounds();
        this.nodes.add(nodeFrontRight);
        this.nodes.add(nodeFrontLeft);
        this.nodes.add(nodeRearLeft);
        this.nodes.add(nodeRearRight);
        this.edges.add(edgeFront);
        this.edges.add(edgeLeft);
        this.edges.add(edgeRear);
        this.edges.add(edgeRight);
        if (!(vehicle.upVectorDot < 0.95F)) {
            obstacle.nodeCrawlFront = Node.alloc().init((poly.x1 + poly.x2) / 2.0F, (poly.y1 + poly.y2) / 2.0F, z);
            obstacle.nodeCrawlRear = Node.alloc().init((poly.x3 + poly.x4) / 2.0F, (poly.y3 + poly.y4) / 2.0F, z);
            obstacle.nodeCrawlFront.flags |= 1;
            obstacle.nodeCrawlRear.flags |= 1;
            this.nodes.add(obstacle.nodeCrawlFront);
            this.nodes.add(obstacle.nodeCrawlRear);
            Edge edgeCrawl1 = edgeFront.split(obstacle.nodeCrawlFront);
            Edge edgeCrawl3 = edgeRear.split(obstacle.nodeCrawlRear);
            this.edges.add(edgeCrawl1);
            this.edges.add(edgeCrawl3);
            BaseVehicle.Vector2fObjectPool pool = BaseVehicle.TL_vector2f_pool.get();
            Vector2f v1 = pool.alloc();
            Vector2f v2 = pool.alloc();
            obstacle.crawlNodes.clear();

            for (int i = 0; i < vehicle.crawlOffsets.size(); i++) {
                float frac = vehicle.crawlOffsets.get(i);
                v1.set(nodeRearLeft.x, nodeRearLeft.y);
                v2.set(nodeFrontLeft.x, nodeFrontLeft.y);
                v2.sub(v1).mul(frac).add(v1);
                Node nodeLeft = Node.alloc().init(v2.x, v2.y, z);
                nodeLeft.flags |= 1;
                v1.set(nodeRearRight.x, nodeRearRight.y);
                v2.set(nodeFrontRight.x, nodeFrontRight.y);
                v2.sub(v1).mul(frac).add(v1);
                Node nodeRight = Node.alloc().init(v2.x, v2.y, z);
                nodeRight.flags |= 1;
                Node nodeMid = Node.alloc().init((nodeLeft.x + nodeRight.x) / 2.0F, (nodeLeft.y + nodeRight.y) / 2.0F, z);
                nodeMid.flags |= 3;
                obstacle.crawlNodes.add(nodeLeft);
                obstacle.crawlNodes.add(nodeMid);
                obstacle.crawlNodes.add(nodeRight);
                this.nodes.add(nodeLeft);
                this.nodes.add(nodeMid);
                this.nodes.add(nodeRight);
                Edge edgeLeft2 = edgeLeft.split(nodeLeft);
                edgeRight = edgeRight.split(nodeRight);
                this.edges.add(edgeLeft2);
                this.edges.add(edgeRight);
            }

            pool.release(v1);
            pool.release(v2);
        }
    }

    boolean isVisible(Node node1, Node node2) {
        if (node1.sharesEdge(node2)) {
            return !node1.onSameShapeButDoesNotShareAnEdge(node2);
        } else if (node1.sharesShape(node2)) {
            return false;
        } else {
            for (int i = 0; i < this.edges.size(); i++) {
                Edge edge = this.edges.get(i);
                if (this.intersects(node1, node2, edge)) {
                    return false;
                }
            }

            for (int ix = 0; ix < this.perimeterEdges.size(); ix++) {
                Edge edge = this.perimeterEdges.get(ix);
                if (this.intersects(node1, node2, edge)) {
                    return false;
                }
            }

            return true;
        }
    }

    boolean intersects(Node node1, Node node2, Edge edge) {
        return !edge.hasNode(node1) && !edge.hasNode(node2)
            ? Line2D.linesIntersect(node1.x, node1.y, node2.x, node2.y, edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y)
            : false;
    }

    public Intersection getIntersection(Edge edge1, Edge edge2) {
        float x1 = edge1.node1.x;
        float y1 = edge1.node1.y;
        float x2 = edge1.node2.x;
        float y2 = edge1.node2.y;
        float x3 = edge2.node1.x;
        float y3 = edge2.node1.y;
        float x4 = edge2.node2.x;
        float y4 = edge2.node2.y;
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom == 0.0) {
            return null;
        } else {
            double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
            double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
            if (ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0) {
                float intersectX = (float)(x1 + ua * (x2 - x1));
                float intersectY = (float)(y1 + ua * (y2 - y1));
                return new Intersection(edge1, edge2, (float)ua, (float)ub, intersectX, intersectY);
            } else {
                return null;
            }
        }
    }

    @Deprecated
    void addWorldObstacles() {
        VehicleRect bounds = this.cluster.bounds();
        bounds.x--;
        bounds.y--;
        bounds.w += 3;
        bounds.h += 3;
        ObjectOutline[][] oo = new ObjectOutline[bounds.w][bounds.h];
        int z = this.cluster.z;

        for (int y = bounds.top(); y < bounds.bottom() - 1; y++) {
            for (int x = bounds.left(); x < bounds.right() - 1; x++) {
                Square square = PolygonalMap2.instance.getSquare(x, y, z);
                if (square != null && this.contains(square, 1)) {
                    if (square.has(504) || square.isReallySolid()) {
                        ObjectOutline.setSolid(x - bounds.left(), y - bounds.top(), z, oo);
                    }

                    if (square.has(2)) {
                        ObjectOutline.setWest(x - bounds.left(), y - bounds.top(), z, oo);
                    }

                    if (square.has(4)) {
                        ObjectOutline.setNorth(x - bounds.left(), y - bounds.top(), z, oo);
                    }

                    if (square.has(262144)) {
                        ObjectOutline.setWest(x - bounds.left() + 1, y - bounds.top(), z, oo);
                    }

                    if (square.has(524288)) {
                        ObjectOutline.setNorth(x - bounds.left(), y - bounds.top() + 1, z, oo);
                    }
                }
            }
        }

        for (int y = 0; y < bounds.h; y++) {
            for (int xx = 0; xx < bounds.w; xx++) {
                ObjectOutline f = ObjectOutline.get(xx, y, z, oo);
                if (f != null && f.nw && f.nwW && f.nwN) {
                    f.trace(oo, this.obstacleTraceNodes);
                    if (!f.nodes.isEmpty()) {
                        Obstacle obstacle = Obstacle.alloc().init((IsoGridSquare)null);

                        for (int i = 0; i < f.nodes.size() - 1; i++) {
                            Node node1 = f.nodes.get(i);
                            Node node2 = f.nodes.get(i + 1);
                            node1.x = node1.x + bounds.left();
                            node1.y = node1.y + bounds.top();
                            if (!this.contains(node1.x, node1.y, node1.z)) {
                                node1.ignore = true;
                            }

                            Edge edge = Edge.alloc().init(node1, node2, obstacle, obstacle.outer);
                            obstacle.outer.add(edge);
                            this.nodes.add(node1);
                        }

                        obstacle.calcBounds();
                        this.obstacles.add(obstacle);
                        this.edges.addAll(obstacle.outer);
                    }
                }
            }
        }

        for (int y = 0; y < bounds.h; y++) {
            for (int xxx = 0; xxx < bounds.w; xxx++) {
                if (oo[xxx][y] != null) {
                    oo[xxx][y].release();
                }
            }
        }

        bounds.release();
    }

    void addWorldObstaclesClipper() {
        VehicleRect bounds = this.cluster.bounds();
        bounds.x--;
        bounds.y--;
        bounds.w += 2;
        bounds.h += 2;
        if (PolygonalMap2.instance.clipperThread == null) {
            PolygonalMap2.instance.clipperThread = new Clipper();
        }

        Clipper clipper = PolygonalMap2.instance.clipperThread;
        clipper.clear();
        int z = this.cluster.z;

        for (int y = bounds.top(); y < bounds.bottom(); y++) {
            for (int x = bounds.left(); x < bounds.right(); x++) {
                Square square = PolygonalMap2.instance.getSquare(x, y, z);
                if (square != null && this.contains(square, 1)) {
                    if (square.has(504) || square.isReallySolid()) {
                        clipper.addAABB(x - 0.3F, y - 0.3F, x + 1 + 0.3F, y + 1 + 0.3F);
                    }

                    boolean bCollideW = square.has(2);
                    bCollideW |= square.isSlopedSurfaceEdgeBlocked(IsoDirections.W);
                    Square squareW = square.getAdjacentSquare(IsoDirections.W);
                    if (squareW != null && squareW.isSlopedSurfaceEdgeBlocked(IsoDirections.E)) {
                        bCollideW = true;
                    }

                    if (bCollideW) {
                        clipper.addAABB(x - 0.3F, y - 0.3F, x + 0.3F, y + 1 + 0.3F);
                    }

                    boolean bCollideN = square.has(4);
                    bCollideN |= square.isSlopedSurfaceEdgeBlocked(IsoDirections.N);
                    Square squareN = square.getAdjacentSquare(IsoDirections.N);
                    if (squareN != null && squareN.isSlopedSurfaceEdgeBlocked(IsoDirections.S)) {
                        bCollideN = true;
                    }

                    if (bCollideN) {
                        clipper.addAABB(x - 0.3F, y - 0.3F, x + 1 + 0.3F, y + 0.3F);
                    }
                }
            }
        }

        bounds.release();
        ByteBuffer polyBuffer = PolygonalMap2.instance.xyBufferThread;
        int polyCount = clipper.generatePolygons();

        for (int i = 0; i < polyCount; i++) {
            polyBuffer.clear();
            clipper.getPolygon(i, polyBuffer);
            Obstacle obstacle = Obstacle.alloc().init((IsoGridSquare)null);
            this.getEdgesFromBuffer(polyBuffer, obstacle, true, z);
            short holeCount = polyBuffer.getShort();

            for (int j = 0; j < holeCount; j++) {
                this.getEdgesFromBuffer(polyBuffer, obstacle, false, z);
            }

            obstacle.calcBounds();
            this.obstacles.add(obstacle);
            this.edges.addAll(obstacle.outer);

            for (int j = 0; j < obstacle.inner.size(); j++) {
                this.edges.addAll(obstacle.inner.get(j));
            }
        }
    }

    void getEdgesFromBuffer(ByteBuffer polyBuffer, Obstacle obstacle, boolean outer, int z) {
        short pointCount = polyBuffer.getShort();
        if (pointCount < 3) {
            polyBuffer.position(polyBuffer.position() + pointCount * 4 * 2);
        } else {
            EdgeRing edges = obstacle.outer;
            if (!outer) {
                edges = EdgeRing.alloc();
                edges.clear();
                obstacle.inner.add(edges);
            }

            int nodeFirst = this.nodes.size();

            for (int j = pointCount - 1; j >= 0; j--) {
                float x = polyBuffer.getFloat();
                float y = polyBuffer.getFloat();
                Node node1 = Node.alloc().init(x, y, z);
                this.nodes.add(node1);
            }

            for (int j = nodeFirst; j < this.nodes.size() - 1; j++) {
                Node node1 = this.nodes.get(j);
                Node node2 = this.nodes.get(j + 1);
                if (!this.contains(node1.x, node1.y, node1.z)) {
                    node1.ignore = true;
                }

                Edge edge1 = Edge.alloc().init(node1, node2, obstacle, edges);
                edges.add(edge1);
            }

            Node node1 = this.nodes.get(this.nodes.size() - 1);
            Node node2 = this.nodes.get(nodeFirst);
            Edge edge1 = Edge.alloc().init(node1, node2, obstacle, edges);
            edges.add(edge1);
        }
    }

    void trySplit(Edge edge, VehicleRect rect, TIntArrayList splitXY) {
        if (Math.abs(edge.node1.x - edge.node2.x) > Math.abs(edge.node1.y - edge.node2.y)) {
            float edgeLeft = Math.min(edge.node1.x, edge.node2.x);
            float edgeRight = Math.max(edge.node1.x, edge.node2.x);
            float edgeY = edge.node1.y;
            if (rect.left() > edgeLeft
                && rect.left() < edgeRight
                && rect.top() < edgeY
                && rect.bottom() > edgeY
                && !splitXY.contains(rect.left())
                && !this.contains(rect.left() - 0.5F, edgeY, this.cluster.z)) {
                splitXY.add(rect.left());
            }

            if (rect.right() > edgeLeft
                && rect.right() < edgeRight
                && rect.top() < edgeY
                && rect.bottom() > edgeY
                && !splitXY.contains(rect.right())
                && !this.contains(rect.right() + 0.5F, edgeY, this.cluster.z)) {
                splitXY.add(rect.right());
            }
        } else {
            float edgeTop = Math.min(edge.node1.y, edge.node2.y);
            float edgeBottom = Math.max(edge.node1.y, edge.node2.y);
            float edgeX = edge.node1.x;
            if (rect.top() > edgeTop
                && rect.top() < edgeBottom
                && rect.left() < edgeX
                && rect.right() > edgeX
                && !splitXY.contains(rect.top())
                && !this.contains(edgeX, rect.top() - 0.5F, this.cluster.z)) {
                splitXY.add(rect.top());
            }

            if (rect.bottom() > edgeTop
                && rect.bottom() < edgeBottom
                && rect.left() < edgeX
                && rect.right() > edgeX
                && !splitXY.contains(rect.bottom())
                && !this.contains(edgeX, rect.bottom() + 0.5F, this.cluster.z)) {
                splitXY.add(rect.bottom());
            }
        }
    }

    void splitWorldObstacleEdges(EdgeRing edges) {
        for (int j = edges.size() - 1; j >= 0; j--) {
            Edge edge = edges.get(j);
            this.splitXy.clear();

            for (int m = 0; m < this.cluster.rects.size(); m++) {
                VehicleRect rect = this.cluster.rects.get(m);
                this.trySplit(edge, rect, this.splitXy);
            }

            if (!this.splitXy.isEmpty()) {
                this.splitXy.sort();
                if (Math.abs(edge.node1.x - edge.node2.x) > Math.abs(edge.node1.y - edge.node2.y)) {
                    if (edge.node1.x < edge.node2.x) {
                        for (int n = this.splitXy.size() - 1; n >= 0; n--) {
                            Node nodeSplit = Node.alloc().init(this.splitXy.get(n), edge.node1.y, this.cluster.z);
                            Edge edgeNew = edge.split(nodeSplit);
                            this.nodes.add(nodeSplit);
                            this.edges.add(edgeNew);
                        }
                    } else {
                        for (int n = 0; n < this.splitXy.size(); n++) {
                            Node nodeSplit = Node.alloc().init(this.splitXy.get(n), edge.node1.y, this.cluster.z);
                            Edge edgeNew = edge.split(nodeSplit);
                            this.nodes.add(nodeSplit);
                            this.edges.add(edgeNew);
                        }
                    }
                } else if (edge.node1.y < edge.node2.y) {
                    for (int n = this.splitXy.size() - 1; n >= 0; n--) {
                        Node nodeSplit = Node.alloc().init(edge.node1.x, this.splitXy.get(n), this.cluster.z);
                        Edge edgeNew = edge.split(nodeSplit);
                        this.nodes.add(nodeSplit);
                        this.edges.add(edgeNew);
                    }
                } else {
                    for (int n = 0; n < this.splitXy.size(); n++) {
                        Node nodeSplit = Node.alloc().init(edge.node1.x, this.splitXy.get(n), this.cluster.z);
                        Edge edgeNew = edge.split(nodeSplit);
                        this.nodes.add(nodeSplit);
                        this.edges.add(edgeNew);
                    }
                }
            }
        }
    }

    void getStairSquares(ArrayList<Square> squares) {
        VehicleRect bounds = this.cluster.bounds();
        bounds.x -= 4;
        bounds.w += 4;
        bounds.w++;
        bounds.y -= 4;
        bounds.h += 4;
        bounds.h++;

        for (int y = bounds.top(); y < bounds.bottom(); y++) {
            for (int x = bounds.left(); x < bounds.right(); x++) {
                Square square = PolygonalMap2.instance.getSquare(x, y, this.cluster.z);
                if (square != null && square.has(72) && !squares.contains(square)) {
                    squares.add(square);
                }
            }
        }

        bounds.release();
    }

    void getCanPathSquares(ArrayList<Square> squares) {
        VehicleRect bounds = this.cluster.bounds();
        bounds.x--;
        bounds.w += 2;
        bounds.y--;
        bounds.h += 2;

        for (int y = bounds.top(); y < bounds.bottom(); y++) {
            for (int x = bounds.left(); x < bounds.right(); x++) {
                Square square = PolygonalMap2.instance.getSquare(x, y, this.cluster.z);
                if (square != null && (square.isCanPathW() || square.isCanPathN()) && !squares.contains(square)) {
                    squares.add(square);
                }
            }
        }

        bounds.release();
    }

    void connectVehicleCrawlNodes() {
        for (int i = 0; i < this.obstacles.size(); i++) {
            Obstacle obstacle = this.obstacles.get(i);
            if (obstacle.vehicle != null && obstacle.nodeCrawlFront != null) {
                for (int j = 0; j < obstacle.crawlNodes.size(); j += 3) {
                    Node nodeLeft = obstacle.crawlNodes.get(j);
                    Node nodeMid = obstacle.crawlNodes.get(j + 1);
                    Node nodeRight = obstacle.crawlNodes.get(j + 2);
                    PolygonalMap2.instance.connectTwoNodes(nodeLeft, nodeMid);
                    PolygonalMap2.instance.connectTwoNodes(nodeRight, nodeMid);
                    if (j + 3 < obstacle.crawlNodes.size()) {
                        Node nodeMidNext = obstacle.crawlNodes.get(j + 3 + 1);
                        PolygonalMap2.instance.connectTwoNodes(nodeMid, nodeMidNext);
                    }
                }

                if (!obstacle.crawlNodes.isEmpty()) {
                    int n = obstacle.crawlNodes.size() - 2;
                    Node nodeMid = obstacle.crawlNodes.get(n);
                    PolygonalMap2.instance.connectTwoNodes(obstacle.nodeCrawlFront, nodeMid);
                    int var38 = 1;
                    nodeMid = obstacle.crawlNodes.get(var38);
                    PolygonalMap2.instance.connectTwoNodes(obstacle.nodeCrawlRear, nodeMid);
                }

                if (!obstacle.crawlNodes.isEmpty()) {
                    ImmutableRectF bounds = obstacle.bounds;
                    int minX = PZMath.fastfloor(bounds.left());
                    int minY = PZMath.fastfloor(bounds.top());
                    int maxX = (int)Math.ceil(bounds.right());
                    int maxY = (int)Math.ceil(bounds.bottom());

                    for (int y = minY; y < maxY; y++) {
                        for (int x = minX; x < maxX; x++) {
                            Square squareCanPathInside = PolygonalMap2.instance.getSquare(x, y, this.cluster.z);
                            if (squareCanPathInside != null && obstacle.isPointInside(x + 0.5F, y + 0.5F)) {
                                Node nodeInside = PolygonalMap2.instance.getNodeForSquare(squareCanPathInside);

                                for (int jx = nodeInside.visible.size() - 1; jx >= 0; jx--) {
                                    Connection cxn = nodeInside.visible.get(jx);
                                    if (cxn.has(1)) {
                                        Node nodeOutside = cxn.otherNode(nodeInside);
                                        Node nodeMid = obstacle.getClosestInteriorCrawlNode(nodeInside.x, nodeInside.y);

                                        for (int k = 0; k < obstacle.outer.size(); k++) {
                                            Edge edge = obstacle.outer.get(k);
                                            float x1 = edge.node1.x;
                                            float y1 = edge.node1.y;
                                            float x2 = edge.node2.x;
                                            float y2 = edge.node2.y;
                                            float x3 = cxn.node1.x;
                                            float y3 = cxn.node1.y;
                                            float x4 = cxn.node2.x;
                                            float y4 = cxn.node2.y;
                                            double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
                                            if (denom != 0.0) {
                                                double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
                                                double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
                                                if (ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0) {
                                                    float intersectX = (float)(x1 + ua * (x2 - x1));
                                                    float intersectY = (float)(y1 + ua * (y2 - y1));
                                                    Node nodeSplit = Node.alloc().init(intersectX, intersectY, this.cluster.z);
                                                    nodeSplit.flags |= 1;
                                                    boolean bConnected = edge.node1.isConnectedTo(edge.node2);
                                                    Edge edgeNew = edge.split(nodeSplit);
                                                    if (bConnected) {
                                                        PolygonalMap2.instance.connectTwoNodes(edge.node1, edge.node2);
                                                        PolygonalMap2.instance.connectTwoNodes(edgeNew.node1, edgeNew.node2);
                                                    }

                                                    this.edges.add(edgeNew);
                                                    this.nodes.add(nodeSplit);
                                                    PolygonalMap2.instance.connectTwoNodes(nodeOutside, nodeSplit, cxn.flags & 2 | 1);
                                                    PolygonalMap2.instance.connectTwoNodes(nodeSplit, nodeMid, 0);
                                                    break;
                                                }
                                            }
                                        }

                                        PolygonalMap2.instance.breakConnection(cxn);
                                    }
                                }
                            }
                        }
                    }
                }

                for (int jxx = i + 1; jxx < this.obstacles.size(); jxx++) {
                    Obstacle obstacle2 = this.obstacles.get(jxx);
                    if (obstacle2.vehicle != null && obstacle2.nodeCrawlFront != null) {
                        obstacle.connectCrawlNodes(this, obstacle2);
                        obstacle2.connectCrawlNodes(this, obstacle);
                    }
                }
            }
        }
    }

    void checkEdgeIntersection() {
        for (int i = 0; i < this.obstacles.size(); i++) {
            Obstacle obstacle1 = this.obstacles.get(i);

            for (int j = i + 1; j < this.obstacles.size(); j++) {
                Obstacle obstacle2 = this.obstacles.get(j);
                if (obstacle1.bounds.intersects(obstacle2.bounds)) {
                    this.checkEdgeIntersection(obstacle1.outer, obstacle2.outer);

                    for (int m = 0; m < obstacle2.inner.size(); m++) {
                        EdgeRing inner2 = obstacle2.inner.get(m);
                        this.checkEdgeIntersection(obstacle1.outer, inner2);
                    }

                    for (int k = 0; k < obstacle1.inner.size(); k++) {
                        EdgeRing inner1 = obstacle1.inner.get(k);
                        this.checkEdgeIntersection(inner1, obstacle2.outer);

                        for (int m = 0; m < obstacle2.inner.size(); m++) {
                            EdgeRing inner2 = obstacle2.inner.get(m);
                            this.checkEdgeIntersection(inner1, inner2);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < this.obstacles.size(); i++) {
            Obstacle obstacle = this.obstacles.get(i);
            this.checkEdgeIntersectionSplit(obstacle.outer);

            for (int jx = 0; jx < obstacle.inner.size(); jx++) {
                this.checkEdgeIntersectionSplit(obstacle.inner.get(jx));
            }
        }
    }

    void checkEdgeIntersection(EdgeRing edges1, EdgeRing edges2) {
        for (int i = 0; i < edges1.size(); i++) {
            Edge edge1 = edges1.get(i);

            for (int j = 0; j < edges2.size(); j++) {
                Edge edge2 = edges2.get(j);
                if (this.intersects(edge1.node1, edge1.node2, edge2)) {
                    Intersection intersection = this.getIntersection(edge1, edge2);
                    if (intersection != null) {
                        edge1.intersections.add(intersection);
                        edge2.intersections.add(intersection);
                        this.nodes.add(intersection.nodeSplit);
                        this.intersectNodes.add(intersection.nodeSplit);
                    }
                }
            }
        }
    }

    void checkEdgeIntersectionSplit(EdgeRing edges) {
        for (int i = edges.size() - 1; i >= 0; i--) {
            Edge edge = edges.get(i);
            if (!edge.intersections.isEmpty()) {
                comparator.edge = edge;
                Collections.sort(edge.intersections, comparator);

                for (int k = edge.intersections.size() - 1; k >= 0; k--) {
                    Intersection intersection = edge.intersections.get(k);
                    Edge edgeNew = intersection.split(edge);
                    this.edges.add(edgeNew);
                }
            }
        }
    }

    void checkNodesInObstacles() {
        for (int i = 0; i < this.nodes.size(); i++) {
            Node node = this.nodes.get(i);

            for (int j = 0; j < this.obstacles.size(); j++) {
                Obstacle obstacle = this.obstacles.get(j);
                if (obstacle.isNodeInsideOf(node)) {
                    node.ignore = true;
                    break;
                }
            }
        }

        for (int i = 0; i < this.perimeterNodes.size(); i++) {
            Node node = this.perimeterNodes.get(i);

            for (int jx = 0; jx < this.obstacles.size(); jx++) {
                Obstacle obstacle = this.obstacles.get(jx);
                if (obstacle.isNodeInsideOf(node)) {
                    node.ignore = true;
                    break;
                }
            }
        }
    }

    void addPerimeterEdges() {
        VehicleRect bounds = this.cluster.bounds();
        bounds.x--;
        bounds.y--;
        bounds.w += 2;
        bounds.h += 2;
        ClusterOutlineGrid cog = clusterOutlineGrid.setSize(bounds.w, bounds.h);
        int z = this.cluster.z;

        for (int i = 0; i < this.cluster.rects.size(); i++) {
            VehicleRect rect = this.cluster.rects.get(i);
            rect = VehicleRect.alloc().init(rect.x - 1, rect.y - 1, rect.w + 2, rect.h + 2, rect.z);

            for (int y = rect.top(); y < rect.bottom(); y++) {
                for (int x = rect.left(); x < rect.right(); x++) {
                    cog.setInner(x - bounds.left(), y - bounds.top(), z);
                }
            }

            rect.release();
        }

        for (int y = 0; y < bounds.h; y++) {
            for (int x = 0; x < bounds.w; x++) {
                ClusterOutline f = cog.get(x, y, z);
                if (f.inner) {
                    if (!cog.isInner(x - 1, y, z)) {
                        f.w = true;
                    }

                    if (!cog.isInner(x, y - 1, z)) {
                        f.n = true;
                    }

                    if (!cog.isInner(x + 1, y, z)) {
                        f.e = true;
                    }

                    if (!cog.isInner(x, y + 1, z)) {
                        f.s = true;
                    }
                }
            }
        }

        for (int y = 0; y < bounds.h; y++) {
            for (int xx = 0; xx < bounds.w; xx++) {
                ClusterOutline f = cog.get(xx, y, z);
                if (f != null && (f.w || f.n || f.e || f.s || f.innerCorner)) {
                    Square square = PolygonalMap2.instance.getSquare(bounds.x + xx, bounds.y + y, z);
                    if (square != null && !square.isNonThumpableSolid() && !square.has(504)) {
                        Node node = PolygonalMap2.instance.getNodeForSquare(square);
                        node.flags |= 8;
                        node.addGraph(this);
                        this.perimeterNodes.add(node);
                    }
                }

                if (f != null && f.n && f.w && f.inner && !(f.tw | f.tn | f.te | f.ts)) {
                    ArrayList<Node> nodes = cog.trace(f);
                    if (!nodes.isEmpty()) {
                        for (int i = 0; i < nodes.size() - 1; i++) {
                            Node node1 = nodes.get(i);
                            Node node2 = nodes.get(i + 1);
                            node1.x = node1.x + bounds.left();
                            node1.y = node1.y + bounds.top();
                            Edge edge = Edge.alloc().init(node1, node2, null, null);
                            this.perimeterEdges.add(edge);
                        }

                        if (nodes.get(nodes.size() - 1) != nodes.get(0)) {
                            Node var10000 = nodes.get(nodes.size() - 1);
                            var10000.x = var10000.x + bounds.left();
                            var10000 = nodes.get(nodes.size() - 1);
                            var10000.y = var10000.y + bounds.top();
                        }
                    }
                }
            }
        }

        cog.releaseElements();
        bounds.release();
    }

    void calculateNodeVisibility() {
        ArrayList<Node> nodes1 = new ArrayList<>();
        nodes1.addAll(this.nodes);
        nodes1.addAll(this.perimeterNodes);

        for (int i = 0; i < nodes1.size(); i++) {
            Node node1 = nodes1.get(i);
            if (!node1.ignore && (node1.square == null || !node1.square.has(504))) {
                for (int j = i + 1; j < nodes1.size(); j++) {
                    Node node2 = nodes1.get(j);
                    if (!node2.ignore && (node2.square == null || !node2.square.has(504)) && (!node1.hasFlag(8) || !node2.hasFlag(8))) {
                        if (node1.isConnectedTo(node2)) {
                            assert node1.square != null && (node1.square.isCanPathW() || node1.square.isCanPathN())
                                || node2.square != null && (node2.square.isCanPathW() || node2.square.isCanPathN());
                        } else if (this.isVisible(node1, node2)) {
                            PolygonalMap2.instance.connectTwoNodes(node1, node2);
                        }
                    }
                }
            }
        }
    }

    public void addNode(Node node) {
        if (this.created && !node.ignore) {
            ArrayList<Node> nodes1 = new ArrayList<>();
            nodes1.addAll(this.nodes);
            nodes1.addAll(this.perimeterNodes);

            for (int i = 0; i < nodes1.size(); i++) {
                Node node1 = nodes1.get(i);
                if (!node1.ignore && this.isVisible(node1, node)) {
                    PolygonalMap2.instance.connectTwoNodes(node, node1);
                }
            }
        }

        this.nodes.add(node);
    }

    public void removeNode(Node node) {
        this.nodes.remove(node);

        for (int i = node.visible.size() - 1; i >= 0; i--) {
            Connection visible = node.visible.get(i);
            PolygonalMap2.instance.breakConnection(visible);
        }
    }

    boolean contains(float x, float y, int z) {
        for (int i = 0; i < this.cluster.rects.size(); i++) {
            VehicleRect rect = this.cluster.rects.get(i);
            if (rect.containsPoint(x, y, z)) {
                return true;
            }
        }

        return false;
    }

    boolean contains(float x, float y, int z, int expand) {
        for (int i = 0; i < this.cluster.rects.size(); i++) {
            VehicleRect rect = this.cluster.rects.get(i);
            if (rect.containsPoint(x, y, z, expand)) {
                return true;
            }
        }

        return false;
    }

    public boolean contains(Square square) {
        for (int i = 0; i < this.cluster.rects.size(); i++) {
            VehicleRect rect = this.cluster.rects.get(i);
            if (rect.containsPoint(square.x + 0.5F, square.y + 0.5F, square.z)) {
                return true;
            }
        }

        return false;
    }

    public boolean contains(Square square, int expand) {
        for (int i = 0; i < this.cluster.rects.size(); i++) {
            VehicleRect rect = this.cluster.rects.get(i);
            if (rect.containsPoint(square.x + 0.5F, square.y + 0.5F, square.z, expand)) {
                return true;
            }
        }

        return false;
    }

    public boolean intersects(int squareMinX, int squareMinY, int squareMaxX, int squareMaxY, int expand) {
        for (int i = 0; i < this.cluster.rects.size(); i++) {
            VehicleRect rect = this.cluster.rects.get(i);
            if (rect.intersects(squareMinX, squareMinY, squareMaxX, squareMaxY, expand)) {
                return true;
            }
        }

        return false;
    }

    public int getPointOutsideObstacles(float x, float y, float z, AdjustStartEndNodeData adjust) {
        ClosestPointOnEdge closestPointOnEdge = PolygonalMap2.instance.closestPointOnEdge;
        double closestDistSq = Double.MAX_VALUE;
        Edge closestEdge = null;
        Node closestNode = null;
        float closestX = 0.0F;
        float closestY = 0.0F;

        for (int i = 0; i < this.obstacles.size(); i++) {
            Obstacle obstacle = this.obstacles.get(i);
            if (obstacle.bounds.containsPoint(x, y) && obstacle.isPointInside(x, y)) {
                obstacle.getClosestPointOnEdge(x, y, closestPointOnEdge);
                if (closestPointOnEdge.edge != null && closestPointOnEdge.distSq < closestDistSq) {
                    closestDistSq = closestPointOnEdge.distSq;
                    closestEdge = closestPointOnEdge.edge;
                    closestNode = closestPointOnEdge.node;
                    closestX = closestPointOnEdge.point.x;
                    closestY = closestPointOnEdge.point.y;
                }
            }
        }

        if (closestEdge != null) {
            closestPointOnEdge.edge = closestEdge;
            closestPointOnEdge.node = closestNode;
            closestPointOnEdge.point.set(closestX, closestY);
            closestPointOnEdge.distSq = closestDistSq;
            if (closestEdge.obstacle.splitEdgeAtNearestPoint(closestPointOnEdge, PZMath.fastfloor(z), adjust)) {
                adjust.graph = this;
                if (adjust.isNodeNew) {
                    this.edges.add(adjust.newEdge);
                    this.addNode(adjust.node);
                }

                return 1;
            } else {
                return -1;
            }
        } else {
            return 0;
        }
    }

    Node getClosestNodeTo(float x, float y) {
        Node closest = null;
        float closestDistSq = Float.MAX_VALUE;

        for (int i = 0; i < this.nodes.size(); i++) {
            Node node = this.nodes.get(i);
            float distSq = IsoUtils.DistanceToSquared(node.x, node.y, x, y);
            if (distSq < closestDistSq) {
                closest = node;
                closestDistSq = distSq;
            }
        }

        return closest;
    }

    public void create() {
        for (int i = 0; i < this.cluster.rects.size(); i++) {
            VehicleRect rect = this.cluster.rects.get(i);
            this.addEdgesForVehicle(rect.vehicle);
        }

        this.addWorldObstaclesClipper();

        for (int i = 0; i < this.obstacles.size(); i++) {
            Obstacle obstacle = this.obstacles.get(i);
            if (obstacle.vehicle == null) {
                this.splitWorldObstacleEdges(obstacle.outer);

                for (int j = 0; j < obstacle.inner.size(); j++) {
                    this.splitWorldObstacleEdges(obstacle.inner.get(j));
                }
            }
        }

        this.checkEdgeIntersection();
        this.checkNodesInObstacles();
        this.calculateNodeVisibility();
        this.connectVehicleCrawlNodes();
        this.created = true;
    }

    void initOverlappedChunks() {
        this.clearOverlappedChunks();
        VehicleRect bounds = this.cluster.bounds();
        int expand = 1;
        int chunkMinX = PZMath.coorddivision(bounds.left() - 1, 8);
        int chunkMinY = PZMath.coorddivision(bounds.top() - 1, 8);
        int chunkMaxX = (int)PZMath.ceil((bounds.right() + 1 - 1) / 8.0F);
        int chunkMaxY = (int)PZMath.ceil((bounds.bottom() + 1 - 1) / 8.0F);

        for (int y = chunkMinY; y < chunkMaxY; y++) {
            for (int x = chunkMinX; x < chunkMaxX; x++) {
                Chunk chunk = PolygonalMap2.instance.getChunkFromChunkPos(x, y);
                if (chunk != null && this.intersects(chunk.getMinX(), chunk.getMinY(), chunk.getMaxX() + 1, chunk.getMaxY() + 1, 1)) {
                    this.overlappedChunks.add(chunk);
                    chunk.addVisibilityGraph(this);
                }
            }
        }

        bounds.release();
    }

    void clearOverlappedChunks() {
        for (int i = 0; i < this.overlappedChunks.size(); i++) {
            Chunk chunk = this.overlappedChunks.get(i);
            chunk.removeVisibilityGraph(this);
        }

        this.overlappedChunks.clear();
    }

    static VisibilityGraph alloc() {
        return pool.isEmpty() ? new VisibilityGraph() : pool.pop();
    }

    void release() {
        for (int i = 0; i < this.nodes.size(); i++) {
            if (!PolygonalMap2.instance.squareToNode.containsValue(this.nodes.get(i))) {
                this.nodes.get(i).release();
            }
        }

        for (int ix = 0; ix < this.perimeterEdges.size(); ix++) {
            this.perimeterEdges.get(ix).node1.release();
            this.perimeterEdges.get(ix).release();
        }

        for (int ix = 0; ix < this.obstacles.size(); ix++) {
            Obstacle obstacle = this.obstacles.get(ix);
            obstacle.release();
        }

        for (int ix = 0; ix < this.cluster.rects.size(); ix++) {
            this.cluster.rects.get(ix).release();
        }

        this.cluster.release();
        this.clearOverlappedChunks();

        assert !pool.contains(this);

        pool.push(this);
    }

    void render() {
        int cluster_z = this.cluster.z - 32;
        float r = 1.0F;

        for (Edge edge : this.perimeterEdges) {
            LineDrawer.addLine(edge.node1.x, edge.node1.y, cluster_z, edge.node2.x, edge.node2.y, cluster_z, r, 0.5F, 0.5F, null, true);
            r = 1.0F - r;
        }

        for (Obstacle obstacle : this.obstacles) {
            r = 1.0F;

            for (Edge edge : obstacle.outer) {
                LineDrawer.addLine(edge.node1.x, edge.node1.y, cluster_z, edge.node2.x, edge.node2.y, cluster_z, r, 0.5F, 0.5F, null, true);
                r = 1.0F - r;
            }

            for (EdgeRing edges : obstacle.inner) {
                for (Edge edge : edges) {
                    LineDrawer.addLine(edge.node1.x, edge.node1.y, cluster_z, edge.node2.x, edge.node2.y, cluster_z, r, 0.5F, 0.5F, null, true);
                    r = 1.0F - r;
                }
            }

            if (DebugOptions.instance.polymapRenderCrawling.getValue()) {
                for (Node node : obstacle.crawlNodes) {
                    LineDrawer.addLine(node.x - 0.05F, node.y - 0.05F, cluster_z, node.x + 0.05F, node.y + 0.05F, cluster_z, 0.5F, 1.0F, 0.5F, null, false);

                    for (Connection cxn : node.visible) {
                        Node nodeVis = cxn.otherNode(node);
                        if (nodeVis.hasFlag(1)) {
                            LineDrawer.addLine(node.x, node.y, cluster_z, nodeVis.x, nodeVis.y, cluster_z, 0.5F, 1.0F, 0.5F, null, true);
                        }
                    }
                }
            }
        }

        for (Node node1 : this.perimeterNodes) {
            if (DebugOptions.instance.polymapRenderConnections.getValue()) {
                for (Connection cxnx : node1.visible) {
                    Node node2 = cxnx.otherNode(node1);
                    LineDrawer.addLine(node1.x, node1.y, cluster_z, node2.x, node2.y, cluster_z, 0.0F, 0.25F, 0.0F, null, true);
                }
            }

            if (DebugOptions.instance.polymapRenderNodes.getValue()) {
                float r1 = 1.0F;
                float g1 = 0.5F;
                float b1 = 0.0F;
                if (node1.ignore) {
                    g1 = 1.0F;
                }

                LineDrawer.addLine(node1.x - 0.05F, node1.y - 0.05F, cluster_z, node1.x + 0.05F, node1.y + 0.05F, cluster_z, 1.0F, g1, 0.0F, null, false);
            }
        }

        for (Node node1 : this.nodes) {
            if (DebugOptions.instance.polymapRenderConnections.getValue()) {
                for (Connection cxnx : node1.visible) {
                    Node node2 = cxnx.otherNode(node1);
                    if (this.nodes.contains(node2)) {
                        LineDrawer.addLine(node1.x, node1.y, cluster_z, node2.x, node2.y, cluster_z, 0.0F, 1.0F, 0.0F, null, true);
                    }
                }
            }

            if (DebugOptions.instance.polymapRenderNodes.getValue() || node1.ignore) {
                LineDrawer.addLine(node1.x - 0.05F, node1.y - 0.05F, cluster_z, node1.x + 0.05F, node1.y + 0.05F, cluster_z, 1.0F, 1.0F, 0.0F, null, false);
            }
        }

        for (Node node1 : this.intersectNodes) {
            LineDrawer.addLine(node1.x - 0.1F, node1.y - 0.1F, cluster_z, node1.x + 0.1F, node1.y + 0.1F, cluster_z, 1.0F, 0.0F, 0.0F, null, false);
        }
    }

    static final class CompareIntersection implements Comparator<Intersection> {
        Edge edge;

        public int compare(Intersection o1, Intersection o2) {
            float dist1 = this.edge == o1.edge1 ? o1.dist1 : o1.dist2;
            float dist2 = this.edge == o2.edge1 ? o2.dist1 : o2.dist2;
            if (dist1 < dist2) {
                return -1;
            } else {
                return dist1 > dist2 ? 1 : 0;
            }
        }
    }
}
