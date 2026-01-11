// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.iso.IsoGridSquare;
import zombie.popman.ObjectPool;
import zombie.vehicles.Clipper;

final class ChunkDataZ {
    public Chunk chunk;
    public final ArrayList<Obstacle> obstacles = new ArrayList<>();
    public final ArrayList<Node> nodes = new ArrayList<>();
    public int z;
    static short epochCount;
    short epoch;
    public static final ObjectPool<ChunkDataZ> pool = new ObjectPool<>(ChunkDataZ::new);

    public void init(Chunk chunk, int z) {
        this.chunk = chunk;
        this.z = z;
        this.epoch = epochCount;
        if (PolygonalMap2.instance.clipperThread == null) {
            PolygonalMap2.instance.clipperThread = new Clipper();
        }

        Clipper clipper = PolygonalMap2.instance.clipperThread;
        clipper.clear();
        int cx = chunk.wx * 8;
        int cy = chunk.wy * 8;

        for (int y = cy - 2; y < cy + 8 + 2; y++) {
            for (int x = cx - 2; x < cx + 8 + 2; x++) {
                Square square = PolygonalMap2.instance.getSquare(x, y, z);
                if (square != null && square.has(512)) {
                    if (square.isReallySolid() || square.has(128) || square.has(64) || square.has(16) || square.has(8)) {
                        clipper.addAABBBevel(x - 0.3F, y - 0.3F, x + 1.0F + 0.3F, y + 1.0F + 0.3F, 0.19800001F);
                    }

                    if (square.has(2) || square.has(256)) {
                        clipper.addAABBBevel(x - 0.3F, y - 0.3F, x + 0.3F, y + 1.0F + 0.3F, 0.19800001F);
                    }

                    if (square.has(4) || square.has(32)) {
                        clipper.addAABBBevel(x - 0.3F, y - 0.3F, x + 1.0F + 0.3F, y + 0.3F, 0.19800001F);
                    }

                    if (square.has(256)) {
                        Square square2 = PolygonalMap2.instance.getSquare(x + 1, y, z);
                        if (square2 != null) {
                            clipper.addAABBBevel(x + 1 - 0.3F, y - 0.3F, x + 1 + 0.3F, y + 1.0F + 0.3F, 0.19800001F);
                        }
                    }

                    if (square.has(32)) {
                        Square square2 = PolygonalMap2.instance.getSquare(x, y + 1, z);
                        if (square2 != null) {
                            clipper.addAABBBevel(x - 0.3F, y + 1 - 0.3F, x + 1.0F + 0.3F, y + 1 + 0.3F, 0.19800001F);
                        }
                    }
                } else {
                    clipper.addAABB(x, y, x + 1.0F, y + 1.0F);
                }
            }
        }

        ByteBuffer xyBuffer = PolygonalMap2.instance.xyBufferThread;
        int polyCount = clipper.generatePolygons();

        for (int i = 0; i < polyCount; i++) {
            xyBuffer.clear();
            clipper.getPolygon(i, xyBuffer);
            Obstacle obstacle = Obstacle.alloc().init((IsoGridSquare)null);
            this.getEdgesFromBuffer(xyBuffer, obstacle, true);
            short holeCount = xyBuffer.getShort();

            for (int j = 0; j < holeCount; j++) {
                this.getEdgesFromBuffer(xyBuffer, obstacle, false);
            }

            obstacle.calcBounds();
            this.obstacles.add(obstacle);
        }

        int x1 = chunk.wx * 8;
        int y1 = chunk.wy * 8;
        int x2 = x1 + 8;
        int y2 = y1 + 8;
        x1 -= 2;
        y1 -= 2;
        x2 += 2;
        y2 += 2;
        ImmutableRectF chunkBounds = ImmutableRectF.alloc();
        chunkBounds.init(x1, y1, x2 - x1, y2 - y1);
        ImmutableRectF vehicleBounds = ImmutableRectF.alloc();

        for (int i = 0; i < PolygonalMap2.instance.vehicles.size(); i++) {
            Vehicle vehicle = PolygonalMap2.instance.vehicles.get(i);
            VehiclePoly poly = vehicle.polyPlusRadius;
            float xMin = Math.min(poly.x1, Math.min(poly.x2, Math.min(poly.x3, poly.x4)));
            float yMin = Math.min(poly.y1, Math.min(poly.y2, Math.min(poly.y3, poly.y4)));
            float xMax = Math.max(poly.x1, Math.max(poly.x2, Math.max(poly.x3, poly.x4)));
            float yMax = Math.max(poly.y1, Math.max(poly.y2, Math.max(poly.y3, poly.y4)));
            vehicleBounds.init(xMin, yMin, xMax - xMin, yMax - yMin);
            if (chunkBounds.intersects(vehicleBounds)) {
                this.addEdgesForVehicle(vehicle);
            }
        }

        chunkBounds.release();
        vehicleBounds.release();
    }

    private void getEdgesFromBuffer(ByteBuffer xyBuffer, Obstacle obstacle, boolean outer) {
        int pointCount = xyBuffer.getShort();
        if (pointCount < 3) {
            xyBuffer.position(xyBuffer.position() + pointCount * 4 * 2);
        } else {
            EdgeRing edges = obstacle.outer;
            if (!outer) {
                edges = EdgeRing.alloc();
                edges.clear();
                obstacle.inner.add(edges);
            }

            int nodeFirst = this.nodes.size();

            for (int j = 0; j < pointCount; j++) {
                float x = xyBuffer.getFloat();
                float y = xyBuffer.getFloat();
                Node node1 = Node.alloc().init(x, y, this.z);
                node1.flags |= 4;
                this.nodes.add(nodeFirst, node1);
            }

            for (int j = nodeFirst; j < this.nodes.size() - 1; j++) {
                Node node1 = this.nodes.get(j);
                Node node2 = this.nodes.get(j + 1);
                Edge edge1 = Edge.alloc().init(node1, node2, obstacle, edges);
                edges.add(edge1);
            }

            Node node1 = this.nodes.get(this.nodes.size() - 1);
            Node node2 = this.nodes.get(nodeFirst);
            Edge edge = Edge.alloc().init(node1, node2, obstacle, edges);
            edges.add(edge);
        }
    }

    private void addEdgesForVehicle(Vehicle vehicle) {
        VehiclePoly poly = vehicle.polyPlusRadius;
        int z = PZMath.fastfloor(poly.z);
        Node nodeFrontRight = Node.alloc().init(poly.x1, poly.y1, z);
        Node nodeFrontLeft = Node.alloc().init(poly.x2, poly.y2, z);
        Node nodeRearLeft = Node.alloc().init(poly.x3, poly.y3, z);
        Node nodeRearRight = Node.alloc().init(poly.x4, poly.y4, z);
        nodeFrontRight.flags |= 4;
        nodeFrontLeft.flags |= 4;
        nodeRearLeft.flags |= 4;
        nodeRearRight.flags |= 4;
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
    }

    public void clear() {
        Node.releaseAll(this.nodes);
        this.nodes.clear();
        Obstacle.releaseAll(this.obstacles);
        this.obstacles.clear();
    }
}
