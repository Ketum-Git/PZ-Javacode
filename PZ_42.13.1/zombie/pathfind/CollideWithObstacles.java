// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.joml.Vector2f;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.vehicles.BaseVehicle;

public final class CollideWithObstacles {
    static final float RADIUS = 0.3F;
    private final ArrayList<CollideWithObstacles.CCObstacle> obstacles = new ArrayList<>();
    private final ArrayList<CollideWithObstacles.CCNode> nodes = new ArrayList<>();
    private final ArrayList<CollideWithObstacles.CCIntersection> intersections = new ArrayList<>();
    private final CollideWithObstacles.ImmutableRectF moveBounds = new CollideWithObstacles.ImmutableRectF();
    private final CollideWithObstacles.ImmutableRectF vehicleBounds = new CollideWithObstacles.ImmutableRectF();
    private final Vector2 move = new Vector2();
    private final Vector2 closest = new Vector2();
    private final Vector2 nodeNormal = new Vector2();
    private final Vector2 edgeVec = new Vector2();
    private final ArrayList<BaseVehicle> vehicles = new ArrayList<>();
    CollideWithObstacles.CCObjectOutline[][] oo = new CollideWithObstacles.CCObjectOutline[5][5];
    ArrayList<CollideWithObstacles.CCNode> obstacleTraceNodes = new ArrayList<>();
    CollideWithObstacles.CompareIntersection comparator = new CollideWithObstacles.CompareIntersection();

    void getVehiclesInRect(float x1, float y1, float x2, float y2, int z) {
        this.vehicles.clear();
        int chunkX1 = PZMath.fastfloor(x1 / 8.0F);
        int chunkY1 = PZMath.fastfloor(y1 / 8.0F);
        int chunkX2 = (int)Math.ceil(x2 / 8.0F);
        int chunkY2 = (int)Math.ceil(y2 / 8.0F);

        for (int y = chunkY1; y < chunkY2; y++) {
            for (int x = chunkX1; x < chunkX2; x++) {
                IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(x, y) : IsoWorld.instance.currentCell.getChunkForGridSquare(x * 8, y * 8, 0);
                if (chunk != null) {
                    for (int i = 0; i < chunk.vehicles.size(); i++) {
                        BaseVehicle vehicle = chunk.vehicles.get(i);
                        if (vehicle.getScript() != null && PZMath.fastfloor(vehicle.getZ()) == z) {
                            this.vehicles.add(vehicle);
                        }
                    }
                }
            }
        }
    }

    void getObstaclesInRect(float x1, float y1, float x2, float y2, int chrX, int chrY, int z) {
        this.nodes.clear();
        this.obstacles.clear();
        this.moveBounds.init(x1 - 1.0F, y1 - 1.0F, x2 - x1 + 2.0F, y2 - y1 + 2.0F);
        this.getVehiclesInRect(x1 - 1.0F - 4.0F, y1 - 1.0F - 4.0F, x2 + 2.0F + 8.0F, y2 + 2.0F + 8.0F, z);

        for (int i = 0; i < this.vehicles.size(); i++) {
            BaseVehicle vehicle = this.vehicles.get(i);
            VehiclePoly poly = vehicle.getPolyPlusRadius();
            float xMin = Math.min(poly.x1, Math.min(poly.x2, Math.min(poly.x3, poly.x4)));
            float yMin = Math.min(poly.y1, Math.min(poly.y2, Math.min(poly.y3, poly.y4)));
            float xMax = Math.max(poly.x1, Math.max(poly.x2, Math.max(poly.x3, poly.x4)));
            float yMax = Math.max(poly.y1, Math.max(poly.y2, Math.max(poly.y3, poly.y4)));
            this.vehicleBounds.init(xMin, yMin, xMax - xMin, yMax - yMin);
            if (this.moveBounds.intersects(this.vehicleBounds)) {
                int polyZ = PZMath.fastfloor(poly.z);
                CollideWithObstacles.CCNode node1 = CollideWithObstacles.CCNode.alloc().init(poly.x1, poly.y1, polyZ);
                CollideWithObstacles.CCNode node2 = CollideWithObstacles.CCNode.alloc().init(poly.x2, poly.y2, polyZ);
                CollideWithObstacles.CCNode node3 = CollideWithObstacles.CCNode.alloc().init(poly.x3, poly.y3, polyZ);
                CollideWithObstacles.CCNode node4 = CollideWithObstacles.CCNode.alloc().init(poly.x4, poly.y4, polyZ);
                CollideWithObstacles.CCObstacle obstacle = CollideWithObstacles.CCObstacle.alloc().init();
                CollideWithObstacles.CCEdge edge1 = CollideWithObstacles.CCEdge.alloc().init(node1, node2, obstacle);
                CollideWithObstacles.CCEdge edge2 = CollideWithObstacles.CCEdge.alloc().init(node2, node3, obstacle);
                CollideWithObstacles.CCEdge edge3 = CollideWithObstacles.CCEdge.alloc().init(node3, node4, obstacle);
                CollideWithObstacles.CCEdge edge4 = CollideWithObstacles.CCEdge.alloc().init(node4, node1, obstacle);
                obstacle.edges.add(edge1);
                obstacle.edges.add(edge2);
                obstacle.edges.add(edge3);
                obstacle.edges.add(edge4);
                obstacle.calcBounds();
                this.obstacles.add(obstacle);
                this.nodes.add(node1);
                this.nodes.add(node2);
                this.nodes.add(node3);
                this.nodes.add(node4);
            }
        }

        if (!this.obstacles.isEmpty()) {
            int boundsLeft = chrX - 2;
            int boundsTop = chrY - 2;
            int boundsRight = chrX + 2 + 1;
            int boundsBottom = chrY + 2 + 1;

            for (int y = boundsTop; y < boundsBottom; y++) {
                for (int x = boundsLeft; x < boundsRight; x++) {
                    CollideWithObstacles.CCObjectOutline.get(x - boundsLeft, y - boundsTop, z, this.oo).init(x - boundsLeft, y - boundsTop, z);
                }
            }

            for (int y = boundsTop; y < boundsBottom - 1; y++) {
                for (int x = boundsLeft; x < boundsRight - 1; x++) {
                    IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                    if (square != null) {
                        if (square.isSolid()
                            || square.isSolidTrans() && !square.isAdjacentToWindow() && !square.isAdjacentToHoppable()
                            || square.has(IsoObjectType.stairsMN)
                            || square.has(IsoObjectType.stairsTN)
                            || square.has(IsoObjectType.stairsMW)
                            || square.has(IsoObjectType.stairsTW)) {
                            CollideWithObstacles.CCObjectOutline.setSolid(x - boundsLeft, y - boundsTop, z, this.oo);
                        }

                        boolean collideW = square.has(IsoFlagType.collideW);
                        if (square.has(IsoFlagType.windowW) || square.has(IsoFlagType.WindowW)) {
                            collideW = true;
                        }

                        if (collideW && square.has(IsoFlagType.doorW)) {
                            collideW = false;
                        }

                        boolean collideN = square.has(IsoFlagType.collideN);
                        if (square.has(IsoFlagType.windowN) || square.has(IsoFlagType.WindowN)) {
                            collideN = true;
                        }

                        if (collideN && square.has(IsoFlagType.doorN)) {
                            collideN = false;
                        }

                        if (collideW || square.hasBlockedDoor(false) || square.has(IsoObjectType.stairsBN)) {
                            CollideWithObstacles.CCObjectOutline.setWest(x - boundsLeft, y - boundsTop, z, this.oo);
                        }

                        if (collideN || square.hasBlockedDoor(true) || square.has(IsoObjectType.stairsBW)) {
                            CollideWithObstacles.CCObjectOutline.setNorth(x - boundsLeft, y - boundsTop, z, this.oo);
                        }

                        if (square.has(IsoObjectType.stairsBN) && x != boundsRight - 2) {
                            square = IsoWorld.instance.currentCell.getGridSquare(x + 1, y, z);
                            if (square != null) {
                                CollideWithObstacles.CCObjectOutline.setWest(x + 1 - boundsLeft, y - boundsTop, z, this.oo);
                            }
                        } else if (square.has(IsoObjectType.stairsBW) && y != boundsBottom - 2) {
                            square = IsoWorld.instance.currentCell.getGridSquare(x, y + 1, z);
                            if (square != null) {
                                CollideWithObstacles.CCObjectOutline.setNorth(x - boundsLeft, y + 1 - boundsTop, z, this.oo);
                            }
                        }
                    }
                }
            }

            for (int y = 0; y < boundsBottom - boundsTop; y++) {
                for (int xx = 0; xx < boundsRight - boundsLeft; xx++) {
                    CollideWithObstacles.CCObjectOutline f = CollideWithObstacles.CCObjectOutline.get(xx, y, z, this.oo);
                    if (f != null && f.nw && f.nwW && f.nwN) {
                        f.trace(this.oo, this.obstacleTraceNodes);
                        if (!f.nodes.isEmpty()) {
                            CollideWithObstacles.CCObstacle obstacle = CollideWithObstacles.CCObstacle.alloc().init();
                            CollideWithObstacles.CCNode node0 = f.nodes.get(f.nodes.size() - 1);

                            for (int ix = f.nodes.size() - 1; ix > 0; ix--) {
                                CollideWithObstacles.CCNode node1 = f.nodes.get(ix);
                                CollideWithObstacles.CCNode node2 = f.nodes.get(ix - 1);
                                node1.x += boundsLeft;
                                node1.y += boundsTop;
                                CollideWithObstacles.CCEdge edge = CollideWithObstacles.CCEdge.alloc().init(node1, node2, obstacle);
                                float n2x = node2.x + (node2 != node0 ? boundsLeft : 0.0F);
                                float n2y = node2.y + (node2 != node0 ? boundsTop : 0.0F);
                                edge.normal.set(n2x - node1.x, n2y - node1.y);
                                edge.normal.normalize();
                                edge.normal.rotate((float)Math.toRadians(90.0));
                                obstacle.edges.add(edge);
                                this.nodes.add(node1);
                            }

                            obstacle.calcBounds();
                            this.obstacles.add(obstacle);
                        }
                    }
                }
            }
        }
    }

    void checkEdgeIntersection() {
        boolean render = Core.debug && DebugOptions.instance.collideWithObstacles.render.obstacles.getValue();

        for (int i = 0; i < this.obstacles.size(); i++) {
            CollideWithObstacles.CCObstacle obstacle1 = this.obstacles.get(i);

            for (int j = i + 1; j < this.obstacles.size(); j++) {
                CollideWithObstacles.CCObstacle obstacle2 = this.obstacles.get(j);
                if (obstacle1.bounds.intersects(obstacle2.bounds)) {
                    for (int k = 0; k < obstacle1.edges.size(); k++) {
                        CollideWithObstacles.CCEdge edge1 = obstacle1.edges.get(k);

                        for (int m = 0; m < obstacle2.edges.size(); m++) {
                            CollideWithObstacles.CCEdge edge2 = obstacle2.edges.get(m);
                            CollideWithObstacles.CCIntersection intersection = this.getIntersection(edge1, edge2);
                            if (intersection != null) {
                                edge1.intersections.add(intersection);
                                edge2.intersections.add(intersection);
                                if (render) {
                                    LineDrawer.addLine(
                                        intersection.nodeSplit.x - 0.1F,
                                        intersection.nodeSplit.y - 0.1F,
                                        edge1.node1.z,
                                        intersection.nodeSplit.x + 0.1F,
                                        intersection.nodeSplit.y + 0.1F,
                                        edge1.node1.z,
                                        1.0F,
                                        0.0F,
                                        0.0F,
                                        null,
                                        false
                                    );
                                }

                                if (!edge1.hasNode(intersection.nodeSplit) && !edge2.hasNode(intersection.nodeSplit)) {
                                    this.nodes.add(intersection.nodeSplit);
                                }

                                this.intersections.add(intersection);
                            }
                        }
                    }
                }
            }
        }

        for (int i = 0; i < this.obstacles.size(); i++) {
            CollideWithObstacles.CCObstacle obstacle1 = this.obstacles.get(i);

            for (int jx = obstacle1.edges.size() - 1; jx >= 0; jx--) {
                CollideWithObstacles.CCEdge edge1 = obstacle1.edges.get(jx);
                if (!edge1.intersections.isEmpty()) {
                    this.comparator.edge = edge1;
                    Collections.sort(edge1.intersections, this.comparator);

                    for (int k = edge1.intersections.size() - 1; k >= 0; k--) {
                        CollideWithObstacles.CCIntersection intersection = edge1.intersections.get(k);
                        CollideWithObstacles.CCEdge var17 = intersection.split(edge1);
                    }
                }
            }
        }
    }

    boolean collinear(float ax, float ay, float bx, float by, float cx, float cy) {
        float f = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay);
        return f >= -0.05F && f < 0.05F;
    }

    boolean within(float p, float q, float r) {
        return p <= q && q <= r || r <= q && q <= p;
    }

    boolean is_on(float ax, float ay, float bx, float by, float cx, float cy) {
        return this.collinear(ax, ay, bx, by, cx, cy) && (ax != bx ? this.within(ax, cx, bx) : this.within(ay, cy, by));
    }

    public CollideWithObstacles.CCIntersection getIntersection(CollideWithObstacles.CCEdge edge1, CollideWithObstacles.CCEdge edge2) {
        float x1 = edge1.node1.x;
        float y1 = edge1.node1.y;
        float x2 = edge1.node2.x;
        float y2 = edge1.node2.y;
        float x3 = edge2.node1.x;
        float y3 = edge2.node1.y;
        float x4 = edge2.node2.x;
        float y4 = edge2.node2.y;
        double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (denom > -0.01 && denom < 0.01) {
            return null;
        } else {
            double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
            double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
            if (ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0) {
                float intersectX = (float)(x1 + ua * (x2 - x1));
                float intersectY = (float)(y1 + ua * (y2 - y1));
                CollideWithObstacles.CCNode node1 = null;
                CollideWithObstacles.CCNode node2 = null;
                if (ua < 0.01F) {
                    node1 = edge1.node1;
                } else if (ua > 0.99F) {
                    node1 = edge1.node2;
                }

                if (ub < 0.01F) {
                    node2 = edge2.node1;
                } else if (ub > 0.99F) {
                    node2 = edge2.node2;
                }

                if (node1 != null && node2 != null) {
                    CollideWithObstacles.CCIntersection intersection = CollideWithObstacles.CCIntersection.alloc()
                        .init(edge1, edge2, (float)ua, (float)ub, node1);
                    edge1.intersections.add(intersection);
                    this.intersections.add(intersection);
                    intersection = CollideWithObstacles.CCIntersection.alloc().init(edge1, edge2, (float)ua, (float)ub, node2);
                    edge2.intersections.add(intersection);
                    this.intersections.add(intersection);
                    LineDrawer.addLine(
                        intersection.nodeSplit.x - 0.1F,
                        intersection.nodeSplit.y - 0.1F,
                        edge1.node1.z,
                        intersection.nodeSplit.x + 0.1F,
                        intersection.nodeSplit.y + 0.1F,
                        edge1.node1.z,
                        1.0F,
                        0.0F,
                        0.0F,
                        null,
                        false
                    );
                    return null;
                } else {
                    return node1 == null && node2 == null
                        ? CollideWithObstacles.CCIntersection.alloc().init(edge1, edge2, (float)ua, (float)ub, intersectX, intersectY)
                        : CollideWithObstacles.CCIntersection.alloc().init(edge1, edge2, (float)ua, (float)ub, node1 == null ? node2 : node1);
                }
            } else {
                return null;
            }
        }
    }

    void checkNodesInObstacles() {
        for (int i = 0; i < this.nodes.size(); i++) {
            CollideWithObstacles.CCNode node = this.nodes.get(i);

            for (int j = 0; j < this.obstacles.size(); j++) {
                CollideWithObstacles.CCObstacle obstacle = this.obstacles.get(j);
                boolean isIntersect = false;

                for (int k = 0; k < this.intersections.size(); k++) {
                    CollideWithObstacles.CCIntersection intersection = this.intersections.get(k);
                    if (intersection.nodeSplit == node) {
                        if (intersection.edge1.obstacle == obstacle || intersection.edge2.obstacle == obstacle) {
                            isIntersect = true;
                        }
                        break;
                    }
                }

                if (!isIntersect && obstacle.isNodeInsideOf(node)) {
                    node.ignore = true;
                    break;
                }
            }
        }
    }

    boolean isVisible(CollideWithObstacles.CCNode node1, CollideWithObstacles.CCNode node2) {
        return node1.sharesEdge(node2) ? !node1.onSameShapeButDoesNotShareAnEdge(node2) : !node1.sharesShape(node2);
    }

    void calculateNodeVisibility() {
        for (int i = 0; i < this.obstacles.size(); i++) {
            CollideWithObstacles.CCObstacle obstacle = this.obstacles.get(i);

            for (int j = 0; j < obstacle.edges.size(); j++) {
                CollideWithObstacles.CCEdge edge = obstacle.edges.get(j);
                if (!edge.node1.ignore && !edge.node2.ignore && this.isVisible(edge.node1, edge.node2)) {
                    edge.node1.visible.add(edge.node2);
                    edge.node2.visible.add(edge.node1);
                }
            }
        }
    }

    Vector2f resolveCollision(IsoGameCharacter chr, float nx, float ny, Vector2f finalPos) {
        if (chr.getCurrentSquare() != null && chr.getCurrentSquare().HasStairs()) {
            finalPos.set(nx, ny);
            return finalPos;
        } else {
            float x1 = chr.getX();
            float y1 = chr.getY();
            float z1 = chr.getZ();
            return this.resolveCollision(x1, y1, z1, nx, ny, finalPos);
        }
    }

    boolean resolveCollision(IsoGameCharacter chr, float radius, Vector2f out_finalPos) {
        if (chr.getCurrentSquare() != null && chr.getCurrentSquare().HasStairs()) {
            out_finalPos.set(chr.getX(), chr.getY());
            return false;
        } else {
            float x1 = chr.getX();
            float y1 = chr.getY();
            float z1 = chr.getZ();
            return this.resolveCollision(x1, y1, z1, radius, out_finalPos);
        }
    }

    private Vector2f resolveCollision(float x1, float y1, float z1, float x2, float y2, Vector2f finalPos) {
        finalPos.set(x2, y2);
        boolean render = Core.debug && DebugOptions.instance.collideWithObstacles.render.obstacles.getValue();
        if (render) {
            LineDrawer.addLine(x1, y1, PZMath.fastfloor(z1), x2, y2, PZMath.fastfloor(z1), 1.0F, 1.0F, 1.0F, null, true);
        }

        if (x1 == x2 && y1 == y2) {
            return finalPos;
        } else {
            Vector2 move = this.move;
            move.set(x2 - x1, y2 - y1);
            move.normalize();
            this.cleanupLastCall();
            this.getObstaclesInRect(
                Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), PZMath.fastfloor(x1), PZMath.fastfloor(y1), PZMath.fastfloor(z1)
            );
            this.checkEdgeIntersection();
            this.checkNodesInObstacles();
            this.calculateNodeVisibility();
            if (render) {
                for (CollideWithObstacles.CCNode node : this.nodes) {
                    for (CollideWithObstacles.CCNode visible : node.visible) {
                        LineDrawer.addLine(node.x, node.y, node.z, visible.x, visible.y, visible.z, 0.0F, 1.0F, 0.0F, null, true);
                    }

                    if (DebugOptions.instance.collideWithObstacles.render.normals.getValue() && node.getNormalAndEdgeVectors(this.nodeNormal, this.edgeVec)) {
                        LineDrawer.addLine(node.x, node.y, node.z, node.x + this.nodeNormal.x, node.y + this.nodeNormal.y, node.z, 0.0F, 0.0F, 1.0F, null, true);
                    }

                    if (node.ignore) {
                        LineDrawer.addLine(node.x - 0.05F, node.y - 0.05F, node.z, node.x + 0.05F, node.y + 0.05F, node.z, 1.0F, 1.0F, 0.0F, null, false);
                    }
                }
            }

            CollideWithObstacles.CCEdge closestEdge = null;
            CollideWithObstacles.CCNode nodeAtXY = null;
            double closestDist = Double.MAX_VALUE;

            for (int i = 0; i < this.obstacles.size(); i++) {
                CollideWithObstacles.CCObstacle obstacle = this.obstacles.get(i);
                int flags = 0;
                if (obstacle.isPointInside(x1, y1, 0)) {
                    for (int j = 0; j < obstacle.edges.size(); j++) {
                        CollideWithObstacles.CCEdge edge = obstacle.edges.get(j);
                        if (edge.node1.visible.contains(edge.node2)) {
                            CollideWithObstacles.CCNode node = edge.closestPoint(x1, y1, this.closest);
                            double distSq = (x1 - this.closest.x) * (x1 - this.closest.x) + (y1 - this.closest.y) * (y1 - this.closest.y);
                            if (distSq < closestDist) {
                                closestDist = distSq;
                                closestEdge = edge;
                                nodeAtXY = node;
                            }
                        }
                    }
                }
            }

            if (closestEdge != null) {
                float dot = closestEdge.normal.dot(move);
                if (dot >= 0.01F) {
                    closestEdge = null;
                }
            }

            if (nodeAtXY != null
                && nodeAtXY.getNormalAndEdgeVectors(this.nodeNormal, this.edgeVec)
                && this.nodeNormal.dot(move) + 0.05F >= this.nodeNormal.dot(this.edgeVec)) {
                nodeAtXY = null;
                closestEdge = null;
            }

            if (closestEdge == null) {
                double closestDistSq = Double.MAX_VALUE;
                closestEdge = null;
                nodeAtXY = null;

                for (int ix = 0; ix < this.obstacles.size(); ix++) {
                    CollideWithObstacles.CCObstacle obstacle1 = this.obstacles.get(ix);

                    for (int jx = 0; jx < obstacle1.edges.size(); jx++) {
                        CollideWithObstacles.CCEdge edge = obstacle1.edges.get(jx);
                        if (edge.node1.visible.contains(edge.node2)) {
                            float x3 = edge.node1.x;
                            float y3 = edge.node1.y;
                            float x4 = edge.node2.x;
                            float y4 = edge.node2.y;
                            float cx = x3 + 0.5F * (x4 - x3);
                            float cy = y3 + 0.5F * (y4 - y3);
                            if (render && DebugOptions.instance.collideWithObstacles.render.normals.getValue()) {
                                LineDrawer.addLine(cx, cy, edge.node1.z, cx + edge.normal.x, cy + edge.normal.y, edge.node1.z, 0.0F, 0.0F, 1.0F, null, true);
                            }

                            double denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
                            if (denom != 0.0) {
                                double ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom;
                                double ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom;
                                float dot = edge.normal.dot(move);
                                if (!(dot >= 0.0F) && ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0) {
                                    if (ub < 0.01 || ub > 0.99) {
                                        CollideWithObstacles.CCNode node = ub < 0.01 ? edge.node1 : edge.node2;
                                        if (node.getNormalAndEdgeVectors(this.nodeNormal, this.edgeVec)) {
                                            if (!(this.nodeNormal.dot(move) + 0.05F >= this.nodeNormal.dot(this.edgeVec))) {
                                                closestEdge = edge;
                                                nodeAtXY = node;
                                                break;
                                            }
                                            continue;
                                        }
                                    }

                                    float intersectX = (float)(x1 + ua * (x2 - x1));
                                    float intersectY = (float)(y1 + ua * (y2 - y1));
                                    double distSq = IsoUtils.DistanceToSquared(x1, y1, intersectX, intersectY);
                                    if (distSq < closestDistSq) {
                                        closestDistSq = distSq;
                                        closestEdge = edge;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (nodeAtXY != null) {
                CollideWithObstacles.CCEdge edge1 = closestEdge;
                CollideWithObstacles.CCEdge edge2 = null;

                for (int ix = 0; ix < nodeAtXY.edges.size(); ix++) {
                    CollideWithObstacles.CCEdge edge = nodeAtXY.edges.get(ix);
                    if (edge.node1.visible.contains(edge.node2)
                        && edge != closestEdge
                        && (edge1.node1.x != edge.node1.x || edge1.node1.y != edge.node1.y || edge1.node2.x != edge.node2.x || edge1.node2.y != edge.node2.y)
                        && (edge1.node1.x != edge.node2.x || edge1.node1.y != edge.node2.y || edge1.node2.x != edge.node1.x || edge1.node2.y != edge.node1.y)
                        && (!edge1.hasNode(edge.node1) || !edge1.hasNode(edge.node2))) {
                        edge2 = edge;
                    }
                }

                if (edge1 != null && edge2 != null) {
                    if (closestEdge == edge1) {
                        CollideWithObstacles.CCNode nodeOther = nodeAtXY == edge2.node1 ? edge2.node2 : edge2.node1;
                        this.edgeVec.set(nodeOther.x - nodeAtXY.x, nodeOther.y - nodeAtXY.y);
                        this.edgeVec.normalize();
                        if (move.dot(this.edgeVec) >= 0.0F) {
                            closestEdge = edge2;
                        }
                    } else if (closestEdge == edge2) {
                        CollideWithObstacles.CCNode nodeOther = nodeAtXY == edge1.node1 ? edge1.node2 : edge1.node1;
                        this.edgeVec.set(nodeOther.x - nodeAtXY.x, nodeOther.y - nodeAtXY.y);
                        this.edgeVec.normalize();
                        if (move.dot(this.edgeVec) >= 0.0F) {
                            closestEdge = edge1;
                        }
                    }
                }
            }

            if (closestEdge != null) {
                float _x1 = closestEdge.node1.x;
                float _y1 = closestEdge.node1.y;
                float _x2 = closestEdge.node2.x;
                float _y2 = closestEdge.node2.y;
                if (render) {
                    LineDrawer.addLine(_x1, _y1, closestEdge.node1.z, _x2, _y2, closestEdge.node1.z, 0.0F, 1.0F, 1.0F, null, true);
                }

                closestEdge.closestPoint(x2, y2, this.closest);
                finalPos.set(this.closest.x, this.closest.y);
            }

            return finalPos;
        }
    }

    private boolean resolveCollision(float x1, float y1, float z1, float in_radius, Vector2f out_finalPos) {
        out_finalPos.set(x1, y1);
        if (in_radius <= 0.0) {
            return false;
        } else {
            boolean render = Core.debug && DebugOptions.instance.collideWithObstacles.render.obstacles.getValue();
            if (render) {
                LineDrawer.DrawIsoCircle(x1, y1, z1, in_radius, 16, 1.0F, 0.0F, 1.0F, 1.0F);
            }

            this.cleanupLastCall();
            this.getObstaclesInRect(
                x1 - in_radius, y1 - in_radius, x1 + in_radius, y1 + in_radius, PZMath.fastfloor(x1), PZMath.fastfloor(y1), PZMath.fastfloor(z1)
            );
            this.checkEdgeIntersection();
            this.checkNodesInObstacles();
            this.calculateNodeVisibility();
            if (render) {
                for (CollideWithObstacles.CCNode node : this.nodes) {
                    for (CollideWithObstacles.CCNode visible : node.visible) {
                        LineDrawer.addLine(node.x, node.y, node.z, visible.x, visible.y, visible.z, 0.0F, 1.0F, 0.0F, null, true);
                    }

                    if (DebugOptions.instance.collideWithObstacles.render.normals.getValue() && node.getNormalAndEdgeVectors(this.nodeNormal, this.edgeVec)) {
                        LineDrawer.addLine(node.x, node.y, node.z, node.x + this.nodeNormal.x, node.y + this.nodeNormal.y, node.z, 0.0F, 0.0F, 1.0F, null, true);
                    }

                    if (node.ignore) {
                        LineDrawer.addLine(node.x - 0.05F, node.y - 0.05F, node.z, node.x + 0.05F, node.y + 0.05F, node.z, 1.0F, 1.0F, 0.0F, null, false);
                    }
                }
            }

            CollideWithObstacles.CCEdge closestEdge = null;
            CollideWithObstacles.CCNode nodeAtXY = null;
            double closestDist = Double.MAX_VALUE;

            for (int i = 0; i < this.obstacles.size(); i++) {
                CollideWithObstacles.CCObstacle obstacle = this.obstacles.get(i);
                int flags = 0;
                if (obstacle.isPointInside(x1, y1, 0)) {
                    for (int j = 0; j < obstacle.edges.size(); j++) {
                        CollideWithObstacles.CCEdge edge = obstacle.edges.get(j);
                        if (edge.node1.visible.contains(edge.node2)) {
                            CollideWithObstacles.CCNode node = edge.closestPoint(x1, y1, this.closest);
                            double distSq = (x1 - this.closest.x) * (x1 - this.closest.x) + (y1 - this.closest.y) * (y1 - this.closest.y);
                            if (distSq < closestDist) {
                                closestDist = distSq;
                                closestEdge = edge;
                                nodeAtXY = node;
                            }
                        }
                    }
                }
            }

            if (nodeAtXY != null
                && nodeAtXY.getNormalAndEdgeVectors(this.nodeNormal, this.edgeVec)
                && this.nodeNormal.dot(this.move) + 0.05F >= this.nodeNormal.dot(this.edgeVec)) {
                nodeAtXY = null;
                closestEdge = null;
            }

            if (closestEdge == null) {
                return false;
            } else if (closestDist > in_radius) {
                return false;
            } else {
                Vector2 Pc = this.closest;
                closestEdge.closestPoint(x1, y1, Pc);
                Vector2 Vc;
                if (nodeAtXY == null) {
                    Vc = closestEdge.normal;
                } else {
                    Vc = this.nodeNormal;
                    nodeAtXY.getNormalAndEdgeVectors(Vc, this.edgeVec);
                }

                float VpcX = Pc.x - x1;
                float VpcY = Pc.y - y1;
                float VpcDotN = VpcX * Vc.x + VpcY * Vc.y;
                if (VpcDotN < 0.0) {
                    return false;
                } else {
                    out_finalPos.x = Pc.x + Vc.x * in_radius;
                    out_finalPos.y = Pc.y + Vc.y * in_radius;
                    return true;
                }
            }
        }
    }

    private void cleanupLastCall() {
        for (int i = 0; i < this.nodes.size(); i++) {
            this.nodes.get(i).release();
        }

        for (int i = 0; i < this.obstacles.size(); i++) {
            CollideWithObstacles.CCObstacle obstacle = this.obstacles.get(i);

            for (int j = 0; j < obstacle.edges.size(); j++) {
                obstacle.edges.get(j).release();
            }

            obstacle.release();
        }

        for (int i = 0; i < this.intersections.size(); i++) {
            this.intersections.get(i).release();
        }

        this.intersections.clear();
    }

    private static final class CCEdge {
        CollideWithObstacles.CCNode node1;
        CollideWithObstacles.CCNode node2;
        CollideWithObstacles.CCObstacle obstacle;
        final ArrayList<CollideWithObstacles.CCIntersection> intersections = new ArrayList<>();
        final Vector2 normal = new Vector2();
        static ArrayDeque<CollideWithObstacles.CCEdge> pool = new ArrayDeque<>();

        CollideWithObstacles.CCEdge init(CollideWithObstacles.CCNode node1, CollideWithObstacles.CCNode node2, CollideWithObstacles.CCObstacle obstacle) {
            if (node1.x == node2.x && node1.y == node2.y) {
                boolean var4 = false;
            }

            this.node1 = node1;
            this.node2 = node2;
            node1.edges.add(this);
            node2.edges.add(this);
            this.obstacle = obstacle;
            this.intersections.clear();
            this.normal.set(node2.x - node1.x, node2.y - node1.y);
            this.normal.normalize();
            this.normal.rotate((float)Math.toRadians(90.0));
            return this;
        }

        boolean hasNode(CollideWithObstacles.CCNode node) {
            return node == this.node1 || node == this.node2;
        }

        CollideWithObstacles.CCEdge split(CollideWithObstacles.CCNode nodeSplit) {
            CollideWithObstacles.CCEdge edgeNew = alloc().init(nodeSplit, this.node2, this.obstacle);
            this.obstacle.edges.add(this.obstacle.edges.indexOf(this) + 1, edgeNew);
            this.node2.edges.remove(this);
            this.node2 = nodeSplit;
            this.node2.edges.add(this);
            return edgeNew;
        }

        CollideWithObstacles.CCNode closestPoint(float x3, float y3, Vector2 closest) {
            float x1 = this.node1.x;
            float y1 = this.node1.y;
            float x2 = this.node2.x;
            float y2 = this.node2.y;
            double u = ((x3 - x1) * (x2 - x1) + (y3 - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
            double epsilon = 0.001;
            if (u <= 0.001) {
                closest.set(x1, y1);
                return this.node1;
            } else if (u >= 0.999) {
                closest.set(x2, y2);
                return this.node2;
            } else {
                double xu = x1 + u * (x2 - x1);
                double yu = y1 + u * (y2 - y1);
                closest.set((float)xu, (float)yu);
                return null;
            }
        }

        boolean isPointOn(float x3, float y3) {
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

        static CollideWithObstacles.CCEdge alloc() {
            return pool.isEmpty() ? new CollideWithObstacles.CCEdge() : pool.pop();
        }

        void release() {
            assert !pool.contains(this);

            pool.push(this);
        }
    }

    private static final class CCIntersection {
        CollideWithObstacles.CCEdge edge1;
        CollideWithObstacles.CCEdge edge2;
        float dist1;
        float dist2;
        CollideWithObstacles.CCNode nodeSplit;
        static ArrayDeque<CollideWithObstacles.CCIntersection> pool = new ArrayDeque<>();

        CollideWithObstacles.CCIntersection init(
            CollideWithObstacles.CCEdge edge1, CollideWithObstacles.CCEdge edge2, float dist1, float dist2, float x, float y
        ) {
            this.edge1 = edge1;
            this.edge2 = edge2;
            this.dist1 = dist1;
            this.dist2 = dist2;
            this.nodeSplit = CollideWithObstacles.CCNode.alloc().init(x, y, edge1.node1.z);
            return this;
        }

        CollideWithObstacles.CCIntersection init(
            CollideWithObstacles.CCEdge edge1, CollideWithObstacles.CCEdge edge2, float dist1, float dist2, CollideWithObstacles.CCNode nodeSplit
        ) {
            this.edge1 = edge1;
            this.edge2 = edge2;
            this.dist1 = dist1;
            this.dist2 = dist2;
            this.nodeSplit = nodeSplit;
            return this;
        }

        CollideWithObstacles.CCEdge split(CollideWithObstacles.CCEdge edge) {
            if (edge.hasNode(this.nodeSplit)) {
                return null;
            } else if (edge.node1.x == this.nodeSplit.x && edge.node1.y == this.nodeSplit.y) {
                return null;
            } else {
                return edge.node2.x == this.nodeSplit.x && edge.node2.y == this.nodeSplit.y ? null : edge.split(this.nodeSplit);
            }
        }

        static CollideWithObstacles.CCIntersection alloc() {
            return pool.isEmpty() ? new CollideWithObstacles.CCIntersection() : pool.pop();
        }

        void release() {
            assert !pool.contains(this);

            pool.push(this);
        }
    }

    private static final class CCNode {
        float x;
        float y;
        int z;
        boolean ignore;
        final ArrayList<CollideWithObstacles.CCEdge> edges = new ArrayList<>();
        final ArrayList<CollideWithObstacles.CCNode> visible = new ArrayList<>();
        static ArrayList<CollideWithObstacles.CCObstacle> tempObstacles = new ArrayList<>();
        static ArrayDeque<CollideWithObstacles.CCNode> pool = new ArrayDeque<>();

        CollideWithObstacles.CCNode init(float x, float y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.ignore = false;
            this.edges.clear();
            this.visible.clear();
            return this;
        }

        CollideWithObstacles.CCNode setXY(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }

        boolean sharesEdge(CollideWithObstacles.CCNode other) {
            for (int i = 0; i < this.edges.size(); i++) {
                CollideWithObstacles.CCEdge edge = this.edges.get(i);
                if (edge.hasNode(other)) {
                    return true;
                }
            }

            return false;
        }

        boolean sharesShape(CollideWithObstacles.CCNode other) {
            for (int i = 0; i < this.edges.size(); i++) {
                CollideWithObstacles.CCEdge edge = this.edges.get(i);

                for (int j = 0; j < other.edges.size(); j++) {
                    CollideWithObstacles.CCEdge edgeOther = other.edges.get(j);
                    if (edge.obstacle != null && edge.obstacle == edgeOther.obstacle) {
                        return true;
                    }
                }
            }

            return false;
        }

        void getObstacles(ArrayList<CollideWithObstacles.CCObstacle> obstacles) {
            for (int i = 0; i < this.edges.size(); i++) {
                CollideWithObstacles.CCEdge edge = this.edges.get(i);
                if (!obstacles.contains(edge.obstacle)) {
                    obstacles.add(edge.obstacle);
                }
            }
        }

        boolean onSameShapeButDoesNotShareAnEdge(CollideWithObstacles.CCNode other) {
            tempObstacles.clear();
            this.getObstacles(tempObstacles);

            for (int i = 0; i < tempObstacles.size(); i++) {
                CollideWithObstacles.CCObstacle obstacle = tempObstacles.get(i);
                if (obstacle.hasNode(other) && !obstacle.hasAdjacentNodes(this, other)) {
                    return true;
                }
            }

            return false;
        }

        boolean getNormalAndEdgeVectors(Vector2 out_normal, Vector2 out_edgeVec) {
            CollideWithObstacles.CCEdge edge1 = null;
            CollideWithObstacles.CCEdge edge2 = null;

            for (int i = 0; i < this.edges.size(); i++) {
                CollideWithObstacles.CCEdge edge = this.edges.get(i);
                if (edge.node1.visible.contains(edge.node2)) {
                    if (edge1 == null) {
                        edge1 = edge;
                    } else if (!edge1.hasNode(edge.node1) || !edge1.hasNode(edge.node2)) {
                        edge2 = edge;
                    }
                }
            }

            if (edge1 != null && edge2 != null) {
                float vx = edge1.normal.x + edge2.normal.x;
                float vy = edge1.normal.y + edge2.normal.y;
                out_normal.set(vx, vy);
                out_normal.normalize();
                if (edge1.node1 == this) {
                    out_edgeVec.set(edge1.node2.x - edge1.node1.x, edge1.node2.y - edge1.node1.y);
                } else {
                    out_edgeVec.set(edge1.node1.x - edge1.node2.x, edge1.node1.y - edge1.node2.y);
                }

                out_edgeVec.normalize();
                return true;
            } else {
                return false;
            }
        }

        static CollideWithObstacles.CCNode alloc() {
            if (pool.isEmpty()) {
                boolean var0 = false;
            } else {
                boolean var1 = false;
            }

            return pool.isEmpty() ? new CollideWithObstacles.CCNode() : pool.pop();
        }

        void release() {
            assert !pool.contains(this);

            pool.push(this);
        }
    }

    private static final class CCObjectOutline {
        int x;
        int y;
        int z;
        boolean nw;
        boolean nwW;
        boolean nwN;
        boolean nwE;
        boolean nwS;
        boolean wW;
        boolean wE;
        boolean wCutoff;
        boolean nN;
        boolean nS;
        boolean nCutoff;
        ArrayList<CollideWithObstacles.CCNode> nodes;
        static ArrayDeque<CollideWithObstacles.CCObjectOutline> pool = new ArrayDeque<>();

        CollideWithObstacles.CCObjectOutline init(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.nw = this.nwW = this.nwN = this.nwE = false;
            this.wW = this.wE = this.wCutoff = false;
            this.nN = this.nS = this.nCutoff = false;
            return this;
        }

        static void setSolid(int x, int y, int z, CollideWithObstacles.CCObjectOutline[][] oo) {
            setWest(x, y, z, oo);
            setNorth(x, y, z, oo);
            setWest(x + 1, y, z, oo);
            setNorth(x, y + 1, z, oo);
        }

        static void setWest(int x, int y, int z, CollideWithObstacles.CCObjectOutline[][] oo) {
            CollideWithObstacles.CCObjectOutline f1 = get(x, y, z, oo);
            if (f1 != null) {
                if (f1.nw) {
                    f1.nwS = false;
                } else {
                    f1.nw = true;
                    f1.nwW = true;
                    f1.nwN = true;
                    f1.nwE = true;
                    f1.nwS = false;
                }

                f1.wW = true;
                f1.wE = true;
            }

            f1 = get(x, y + 1, z, oo);
            if (f1 == null) {
                if (f1 != null) {
                    f1.wCutoff = true;
                }
            } else if (f1.nw) {
                f1.nwN = false;
            } else {
                f1.nw = true;
                f1.nwN = false;
                f1.nwW = true;
                f1.nwE = true;
                f1.nwS = true;
            }
        }

        static void setNorth(int x, int y, int z, CollideWithObstacles.CCObjectOutline[][] oo) {
            CollideWithObstacles.CCObjectOutline f1 = get(x, y, z, oo);
            if (f1 != null) {
                if (f1.nw) {
                    f1.nwE = false;
                } else {
                    f1.nw = true;
                    f1.nwW = true;
                    f1.nwN = true;
                    f1.nwE = false;
                    f1.nwS = true;
                }

                f1.nN = true;
                f1.nS = true;
            }

            f1 = get(x + 1, y, z, oo);
            if (f1 == null) {
                if (f1 != null) {
                    f1.nCutoff = true;
                }
            } else if (f1.nw) {
                f1.nwW = false;
            } else {
                f1.nw = true;
                f1.nwN = true;
                f1.nwW = false;
                f1.nwE = true;
                f1.nwS = true;
            }
        }

        static CollideWithObstacles.CCObjectOutline get(int x, int y, int z, CollideWithObstacles.CCObjectOutline[][] oo) {
            if (x < 0 || x >= oo.length) {
                return null;
            } else if (y >= 0 && y < oo[0].length) {
                if (oo[x][y] == null) {
                    oo[x][y] = alloc().init(x, y, z);
                }

                return oo[x][y];
            } else {
                return null;
            }
        }

        void trace_NW_N(CollideWithObstacles.CCObjectOutline[][] oo, CollideWithObstacles.CCNode extend) {
            if (extend != null) {
                extend.setXY(this.x + 0.3F, this.y - 0.3F);
            } else {
                CollideWithObstacles.CCNode node2 = CollideWithObstacles.CCNode.alloc().init(this.x + 0.3F, this.y - 0.3F, this.z);
                this.nodes.add(node2);
            }

            this.nwN = false;
            if (this.nwE) {
                this.trace_NW_E(oo, null);
            } else if (this.nN) {
                this.trace_N_N(oo, this.nodes.get(this.nodes.size() - 1));
            }
        }

        void trace_NW_S(CollideWithObstacles.CCObjectOutline[][] oo, CollideWithObstacles.CCNode extend) {
            if (extend != null) {
                extend.setXY(this.x - 0.3F, this.y + 0.3F);
            } else {
                CollideWithObstacles.CCNode node2 = CollideWithObstacles.CCNode.alloc().init(this.x - 0.3F, this.y + 0.3F, this.z);
                this.nodes.add(node2);
            }

            this.nwS = false;
            if (this.nwW) {
                this.trace_NW_W(oo, null);
            } else {
                CollideWithObstacles.CCObjectOutline f1 = get(this.x - 1, this.y, this.z, oo);
                if (f1 == null) {
                    return;
                }

                if (f1.nS) {
                    f1.nodes = this.nodes;
                    f1.trace_N_S(oo, this.nodes.get(this.nodes.size() - 1));
                }
            }
        }

        void trace_NW_W(CollideWithObstacles.CCObjectOutline[][] oo, CollideWithObstacles.CCNode extend) {
            if (extend != null) {
                extend.setXY(this.x - 0.3F, this.y - 0.3F);
            } else {
                CollideWithObstacles.CCNode node2 = CollideWithObstacles.CCNode.alloc().init(this.x - 0.3F, this.y - 0.3F, this.z);
                this.nodes.add(node2);
            }

            this.nwW = false;
            if (this.nwN) {
                this.trace_NW_N(oo, null);
            } else {
                CollideWithObstacles.CCObjectOutline f1 = get(this.x, this.y - 1, this.z, oo);
                if (f1 == null) {
                    return;
                }

                if (f1.wW) {
                    f1.nodes = this.nodes;
                    f1.trace_W_W(oo, this.nodes.get(this.nodes.size() - 1));
                }
            }
        }

        void trace_NW_E(CollideWithObstacles.CCObjectOutline[][] oo, CollideWithObstacles.CCNode extend) {
            if (extend != null) {
                extend.setXY(this.x + 0.3F, this.y + 0.3F);
            } else {
                CollideWithObstacles.CCNode node2 = CollideWithObstacles.CCNode.alloc().init(this.x + 0.3F, this.y + 0.3F, this.z);
                this.nodes.add(node2);
            }

            this.nwE = false;
            if (this.nwS) {
                this.trace_NW_S(oo, null);
            } else if (this.wE) {
                this.trace_W_E(oo, this.nodes.get(this.nodes.size() - 1));
            }
        }

        void trace_W_E(CollideWithObstacles.CCObjectOutline[][] oo, CollideWithObstacles.CCNode extend) {
            if (extend != null) {
                extend.setXY(this.x + 0.3F, this.y + 1 - 0.3F);
            } else {
                CollideWithObstacles.CCNode node2 = CollideWithObstacles.CCNode.alloc().init(this.x + 0.3F, this.y + 1 - 0.3F, this.z);
                this.nodes.add(node2);
            }

            this.wE = false;
            if (this.wCutoff) {
                CollideWithObstacles.CCNode node2 = this.nodes.get(this.nodes.size() - 1);
                node2.setXY(this.x + 0.3F, this.y + 1 + 0.3F);
                node2 = CollideWithObstacles.CCNode.alloc().init(this.x - 0.3F, this.y + 1 + 0.3F, this.z);
                this.nodes.add(node2);
                node2 = CollideWithObstacles.CCNode.alloc().init(this.x - 0.3F, this.y + 1 - 0.3F, this.z);
                this.nodes.add(node2);
                this.trace_W_W(oo, node2);
            } else {
                CollideWithObstacles.CCObjectOutline f1 = get(this.x, this.y + 1, this.z, oo);
                if (f1 != null) {
                    if (f1.nw && f1.nwE) {
                        f1.nodes = this.nodes;
                        f1.trace_NW_E(oo, this.nodes.get(this.nodes.size() - 1));
                    } else if (f1.nN) {
                        f1.nodes = this.nodes;
                        f1.trace_N_N(oo, null);
                    }
                }
            }
        }

        void trace_W_W(CollideWithObstacles.CCObjectOutline[][] oo, CollideWithObstacles.CCNode extend) {
            if (extend != null) {
                extend.setXY(this.x - 0.3F, this.y + 0.3F);
            } else {
                CollideWithObstacles.CCNode node2 = CollideWithObstacles.CCNode.alloc().init(this.x - 0.3F, this.y + 0.3F, this.z);
                this.nodes.add(node2);
            }

            this.wW = false;
            if (this.nwW) {
                this.trace_NW_W(oo, this.nodes.get(this.nodes.size() - 1));
            } else {
                CollideWithObstacles.CCObjectOutline f1 = get(this.x - 1, this.y, this.z, oo);
                if (f1 == null) {
                    return;
                }

                if (f1.nS) {
                    f1.nodes = this.nodes;
                    f1.trace_N_S(oo, null);
                }
            }
        }

        void trace_N_N(CollideWithObstacles.CCObjectOutline[][] oo, CollideWithObstacles.CCNode extend) {
            if (extend != null) {
                extend.setXY(this.x + 1 - 0.3F, this.y - 0.3F);
            } else {
                CollideWithObstacles.CCNode node2 = CollideWithObstacles.CCNode.alloc().init(this.x + 1 - 0.3F, this.y - 0.3F, this.z);
                this.nodes.add(node2);
            }

            this.nN = false;
            if (this.nCutoff) {
                CollideWithObstacles.CCNode node2 = this.nodes.get(this.nodes.size() - 1);
                node2.setXY(this.x + 1 + 0.3F, this.y - 0.3F);
                node2 = CollideWithObstacles.CCNode.alloc().init(this.x + 1 + 0.3F, this.y + 0.3F, this.z);
                this.nodes.add(node2);
                node2 = CollideWithObstacles.CCNode.alloc().init(this.x + 1 - 0.3F, this.y + 0.3F, this.z);
                this.nodes.add(node2);
                this.trace_N_S(oo, node2);
            } else {
                CollideWithObstacles.CCObjectOutline f1 = get(this.x + 1, this.y, this.z, oo);
                if (f1 != null) {
                    if (f1.nwN) {
                        f1.nodes = this.nodes;
                        f1.trace_NW_N(oo, this.nodes.get(this.nodes.size() - 1));
                    } else {
                        f1 = get(this.x + 1, this.y - 1, this.z, oo);
                        if (f1 == null) {
                            return;
                        }

                        if (f1.wW) {
                            f1.nodes = this.nodes;
                            f1.trace_W_W(oo, null);
                        }
                    }
                }
            }
        }

        void trace_N_S(CollideWithObstacles.CCObjectOutline[][] oo, CollideWithObstacles.CCNode extend) {
            if (extend != null) {
                extend.setXY(this.x + 0.3F, this.y + 0.3F);
            } else {
                CollideWithObstacles.CCNode node2 = CollideWithObstacles.CCNode.alloc().init(this.x + 0.3F, this.y + 0.3F, this.z);
                this.nodes.add(node2);
            }

            this.nS = false;
            if (this.nwS) {
                this.trace_NW_S(oo, this.nodes.get(this.nodes.size() - 1));
            } else if (this.wE) {
                this.trace_W_E(oo, null);
            }
        }

        void trace(CollideWithObstacles.CCObjectOutline[][] oo, ArrayList<CollideWithObstacles.CCNode> nodes) {
            nodes.clear();
            this.nodes = nodes;
            CollideWithObstacles.CCNode node1 = CollideWithObstacles.CCNode.alloc().init(this.x - 0.3F, this.y - 0.3F, this.z);
            nodes.add(node1);
            this.trace_NW_N(oo, null);
            if (nodes.size() != 2 && node1.x == nodes.get(nodes.size() - 1).x && node1.y == nodes.get(nodes.size() - 1).y) {
                nodes.get(nodes.size() - 1).release();
                nodes.set(nodes.size() - 1, node1);
            } else {
                nodes.clear();
            }
        }

        static CollideWithObstacles.CCObjectOutline alloc() {
            return pool.isEmpty() ? new CollideWithObstacles.CCObjectOutline() : pool.pop();
        }

        void release() {
            assert !pool.contains(this);

            pool.push(this);
        }
    }

    private static final class CCObstacle {
        final ArrayList<CollideWithObstacles.CCEdge> edges = new ArrayList<>();
        CollideWithObstacles.ImmutableRectF bounds;
        static ArrayDeque<CollideWithObstacles.CCObstacle> pool = new ArrayDeque<>();

        CollideWithObstacles.CCObstacle init() {
            this.edges.clear();
            return this;
        }

        boolean hasNode(CollideWithObstacles.CCNode node) {
            for (int i = 0; i < this.edges.size(); i++) {
                CollideWithObstacles.CCEdge edge = this.edges.get(i);
                if (edge.hasNode(node)) {
                    return true;
                }
            }

            return false;
        }

        boolean hasAdjacentNodes(CollideWithObstacles.CCNode node1, CollideWithObstacles.CCNode node2) {
            for (int i = 0; i < this.edges.size(); i++) {
                CollideWithObstacles.CCEdge edge = this.edges.get(i);
                if (edge.hasNode(node1) && edge.hasNode(node2)) {
                    return true;
                }
            }

            return false;
        }

        boolean isPointInPolygon_CrossingNumber(float x, float y) {
            int cn = 0;

            for (int i = 0; i < this.edges.size(); i++) {
                CollideWithObstacles.CCEdge edge = this.edges.get(i);
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

        CollideWithObstacles.EdgeRingHit isPointInPolygon_WindingNumber(float x, float y, int flags) {
            int wn = 0;

            for (int i = 0; i < this.edges.size(); i++) {
                CollideWithObstacles.CCEdge edge = this.edges.get(i);
                if ((flags & 16) != 0 && edge.isPointOn(x, y)) {
                    return CollideWithObstacles.EdgeRingHit.OnEdge;
                }

                if (edge.node1.y <= y) {
                    if (edge.node2.y > y && this.isLeft(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y, x, y) > 0.0F) {
                        wn++;
                    }
                } else if (edge.node2.y <= y && this.isLeft(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y, x, y) < 0.0F) {
                    wn--;
                }
            }

            return wn == 0 ? CollideWithObstacles.EdgeRingHit.Outside : CollideWithObstacles.EdgeRingHit.Inside;
        }

        boolean isPointInside(float x, float y, int flags) {
            return this.isPointInPolygon_WindingNumber(x, y, flags) == CollideWithObstacles.EdgeRingHit.Inside;
        }

        boolean isNodeInsideOf(CollideWithObstacles.CCNode node) {
            if (this.hasNode(node)) {
                return false;
            } else if (!this.bounds.containsPoint(node.x, node.y)) {
                return false;
            } else {
                int flags = 0;
                return this.isPointInside(node.x, node.y, 0);
            }
        }

        CollideWithObstacles.CCNode getClosestPointOnEdge(float x3, float y3, Vector2 out) {
            double closestDist = Double.MAX_VALUE;
            CollideWithObstacles.CCNode closestNode = null;
            float closestX = Float.MAX_VALUE;
            float closestY = Float.MAX_VALUE;

            for (int i = 0; i < this.edges.size(); i++) {
                CollideWithObstacles.CCEdge edge = this.edges.get(i);
                if (edge.node1.visible.contains(edge.node2)) {
                    CollideWithObstacles.CCNode node = edge.closestPoint(x3, y3, out);
                    double distSq = (x3 - out.x) * (x3 - out.x) + (y3 - out.y) * (y3 - out.y);
                    if (distSq < closestDist) {
                        closestX = out.x;
                        closestY = out.y;
                        closestNode = node;
                        closestDist = distSq;
                    }
                }
            }

            out.set(closestX, closestY);
            return closestNode;
        }

        void calcBounds() {
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE;
            float maxY = Float.MIN_VALUE;

            for (int i = 0; i < this.edges.size(); i++) {
                CollideWithObstacles.CCEdge edge = this.edges.get(i);
                minX = Math.min(minX, edge.node1.x);
                minY = Math.min(minY, edge.node1.y);
                maxX = Math.max(maxX, edge.node1.x);
                maxY = Math.max(maxY, edge.node1.y);
            }

            if (this.bounds != null) {
                this.bounds.release();
            }

            float epsilon = 0.01F;
            this.bounds = CollideWithObstacles.ImmutableRectF.alloc().init(minX - 0.01F, minY - 0.01F, maxX - minX + 0.02F, maxY - minY + 0.02F);
        }

        static CollideWithObstacles.CCObstacle alloc() {
            return pool.isEmpty() ? new CollideWithObstacles.CCObstacle() : pool.pop();
        }

        void release() {
            assert !pool.contains(this);

            pool.push(this);
        }
    }

    static final class CompareIntersection implements Comparator<CollideWithObstacles.CCIntersection> {
        CollideWithObstacles.CCEdge edge;

        public int compare(CollideWithObstacles.CCIntersection o1, CollideWithObstacles.CCIntersection o2) {
            float dist1 = this.edge == o1.edge1 ? o1.dist1 : o1.dist2;
            float dist2 = this.edge == o2.edge1 ? o2.dist1 : o2.dist2;
            if (dist1 < dist2) {
                return -1;
            } else {
                return dist1 > dist2 ? 1 : 0;
            }
        }
    }

    private static enum EdgeRingHit {
        OnEdge,
        Inside,
        Outside;
    }

    private static final class ImmutableRectF {
        private float x;
        private float y;
        private float w;
        private float h;
        static ArrayDeque<CollideWithObstacles.ImmutableRectF> pool = new ArrayDeque<>();

        CollideWithObstacles.ImmutableRectF init(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        float left() {
            return this.x;
        }

        float top() {
            return this.y;
        }

        float right() {
            return this.x + this.w;
        }

        float bottom() {
            return this.y + this.h;
        }

        float width() {
            return this.w;
        }

        float height() {
            return this.h;
        }

        boolean containsPoint(float x, float y) {
            return x >= this.left() && x < this.right() && y >= this.top() && y < this.bottom();
        }

        boolean intersects(CollideWithObstacles.ImmutableRectF other) {
            return this.left() < other.right() && this.right() > other.left() && this.top() < other.bottom() && this.bottom() > other.top();
        }

        static CollideWithObstacles.ImmutableRectF alloc() {
            return pool.isEmpty() ? new CollideWithObstacles.ImmutableRectF() : pool.pop();
        }

        void release() {
            assert !pool.contains(this);

            pool.push(this);
        }
    }
}
