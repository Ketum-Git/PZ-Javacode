// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import org.joml.Vector2f;
import zombie.GameWindow;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
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
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;

public class CollideWithObstaclesPoly {
    private static final float RADIUS = 0.3F;
    private final ArrayList<CollideWithObstaclesPoly.CCObstacle> obstacles = new ArrayList<>();
    private final ArrayList<CollideWithObstaclesPoly.CCNode> nodes = new ArrayList<>();
    private final CollideWithObstaclesPoly.ImmutableRectF moveBounds = new CollideWithObstaclesPoly.ImmutableRectF();
    private final CollideWithObstaclesPoly.ImmutableRectF vehicleBounds = new CollideWithObstaclesPoly.ImmutableRectF();
    private static final Vector2 move = new Vector2();
    private static final Vector2 nodeNormal = new Vector2();
    private static final Vector2 edgeVec = new Vector2();
    private final ArrayList<BaseVehicle> vehicles = new ArrayList<>();
    private Clipper clipper;
    private final ByteBuffer xyBuffer = ByteBuffer.allocateDirect(8192);
    private final CollideWithObstaclesPoly.ClosestPointOnEdge closestPointOnEdge = new CollideWithObstaclesPoly.ClosestPointOnEdge();

    private void getVehiclesInRect(float x1, float y1, float x2, float y2, int z) {
        this.vehicles.clear();
        int chunkX1 = (int)(x1 / 8.0F);
        int chunkY1 = (int)(y1 / 8.0F);
        int chunkX2 = (int)Math.ceil(x2 / 8.0F);
        int chunkY2 = (int)Math.ceil(y2 / 8.0F);

        for (int y = chunkY1; y < chunkY2; y++) {
            for (int x = chunkX1; x < chunkX2; x++) {
                IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(x, y) : IsoWorld.instance.currentCell.getChunkForGridSquare(x * 8, y * 8, 0);
                if (chunk != null) {
                    for (int i = 0; i < chunk.vehicles.size(); i++) {
                        BaseVehicle vehicle = chunk.vehicles.get(i);
                        if (vehicle.getScript() != null && vehicle.getZi() == z) {
                            this.vehicles.add(vehicle);
                        }
                    }
                }
            }
        }
    }

    void getObstaclesInRect(float x1, float y1, float x2, float y2, int chrX, int chrY, int z, boolean bWithVehicles) {
        if (this.clipper == null) {
            this.clipper = new Clipper();
        }

        this.clipper.clear();
        this.moveBounds.init(x1 - 2.0F, y1 - 2.0F, x2 - x1 + 4.0F, y2 - y1 + 4.0F);
        int chunkX1 = (int)(this.moveBounds.x / 8.0F);
        int chunkY1 = (int)(this.moveBounds.y / 8.0F);
        int chunkX2 = (int)Math.ceil(this.moveBounds.right() / 8.0F);
        int chunkY2 = (int)Math.ceil(this.moveBounds.bottom() / 8.0F);
        if (Math.abs(x2 - x1) < 2.0F && Math.abs(y2 - y1) < 2.0F) {
            chunkX1 = chrX / 8;
            chunkY1 = chrY / 8;
            chunkX2 = chunkX1 + 1;
            chunkY2 = chunkY1 + 1;
        }

        for (int y = chunkY1; y < chunkY2; y++) {
            for (int x = chunkX1; x < chunkX2; x++) {
                IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(x, y) : IsoWorld.instance.currentCell.getChunk(x, y);
                if (chunk != null) {
                    CollideWithObstaclesPoly.ChunkDataZ chunkDataZ = chunk.collision.init(chunk, z, this);
                    ArrayList<CollideWithObstaclesPoly.CCObstacle> obstacles = bWithVehicles ? chunkDataZ.worldVehicleUnion : chunkDataZ.worldVehicleSeparate;

                    for (int i = 0; i < obstacles.size(); i++) {
                        CollideWithObstaclesPoly.CCObstacle obstacle = obstacles.get(i);
                        if (obstacle.bounds.intersects(this.moveBounds)) {
                            this.obstacles.add(obstacle);
                        }
                    }

                    this.nodes.addAll(chunkDataZ.nodes);
                }
            }
        }
    }

    public Vector2f resolveCollision(IsoGameCharacter chr, float nx, float ny, Vector2f finalPos) {
        finalPos.set(nx, ny);
        boolean render = Core.debug && DebugOptions.instance.collideWithObstacles.render.obstacles.getValue();
        float x1 = chr.getX();
        float y1 = chr.getY();
        float x2 = nx;
        float y2 = ny;
        if (render) {
            LineDrawer.addLine(x1, y1, PZMath.fastfloor(chr.getZ()), nx, ny, PZMath.fastfloor(chr.getZ()), 1.0F, 1.0F, 1.0F, null, true);
        }

        if (x1 == nx && y1 == ny) {
            return finalPos;
        } else {
            move.set(nx - chr.getX(), ny - chr.getY());
            move.normalize();
            this.nodes.clear();
            this.obstacles.clear();
            this.getObstaclesInRect(
                Math.min(x1, nx),
                Math.min(y1, ny),
                Math.max(x1, nx),
                Math.max(y1, ny),
                PZMath.fastfloor(chr.getX()),
                PZMath.fastfloor(chr.getY()),
                PZMath.fastfloor(chr.getZ()),
                true
            );
            this.closestPointOnEdge.edge = null;
            this.closestPointOnEdge.node = null;
            this.closestPointOnEdge.distSq = Double.MAX_VALUE;

            for (int i = 0; i < this.obstacles.size(); i++) {
                CollideWithObstaclesPoly.CCObstacle obstacle = this.obstacles.get(i);
                int flags = 0;
                if (obstacle.isPointInside(chr.getX(), chr.getY(), 0)) {
                    obstacle.getClosestPointOnEdge(chr.getX(), chr.getY(), this.closestPointOnEdge);
                }
            }

            CollideWithObstaclesPoly.CCEdge closestEdge = this.closestPointOnEdge.edge;
            CollideWithObstaclesPoly.CCNode nodeAtXY = this.closestPointOnEdge.node;
            if (closestEdge != null) {
                float dot = closestEdge.normal.dot(move);
                if (dot >= 0.01F) {
                    closestEdge = null;
                }
            }

            if (nodeAtXY != null && nodeAtXY.getNormalAndEdgeVectors(nodeNormal, edgeVec) && nodeNormal.dot(move) + 0.05F >= nodeNormal.dot(edgeVec)) {
                nodeAtXY = null;
                closestEdge = null;
            }

            if (closestEdge == null) {
                this.closestPointOnEdge.edge = null;
                this.closestPointOnEdge.node = null;
                this.closestPointOnEdge.distSq = Double.MAX_VALUE;

                for (int ix = 0; ix < this.obstacles.size(); ix++) {
                    CollideWithObstaclesPoly.CCObstacle obstacle1 = this.obstacles.get(ix);
                    obstacle1.lineSegmentIntersect(x1, y1, x2, y2, this.closestPointOnEdge, render);
                }

                closestEdge = this.closestPointOnEdge.edge;
                nodeAtXY = this.closestPointOnEdge.node;
            }

            if (nodeAtXY != null) {
                move.set(nx - chr.getX(), ny - chr.getY());
                move.normalize();
                CollideWithObstaclesPoly.CCEdge edge1 = closestEdge;
                CollideWithObstaclesPoly.CCEdge edge2 = null;

                for (int ix = 0; ix < nodeAtXY.edges.size(); ix++) {
                    CollideWithObstaclesPoly.CCEdge edge = nodeAtXY.edges.get(ix);
                    if (edge != closestEdge
                        && (edge1.node1.x != edge.node1.x || edge1.node1.y != edge.node1.y || edge1.node2.x != edge.node2.x || edge1.node2.y != edge.node2.y)
                        && (edge1.node1.x != edge.node2.x || edge1.node1.y != edge.node2.y || edge1.node2.x != edge.node1.x || edge1.node2.y != edge.node1.y)
                        && (!edge1.hasNode(edge.node1) || !edge1.hasNode(edge.node2))) {
                        edge2 = edge;
                    }
                }

                if (edge1 != null && edge2 != null) {
                    if (closestEdge == edge1) {
                        CollideWithObstaclesPoly.CCNode nodeOther = nodeAtXY == edge2.node1 ? edge2.node2 : edge2.node1;
                        edgeVec.set(nodeOther.x - nodeAtXY.x, nodeOther.y - nodeAtXY.y);
                        edgeVec.normalize();
                        if (move.dot(edgeVec) >= 0.0F) {
                            closestEdge = edge2;
                        }
                    } else if (closestEdge == edge2) {
                        CollideWithObstaclesPoly.CCNode nodeOther = nodeAtXY == edge1.node1 ? edge1.node2 : edge1.node1;
                        edgeVec.set(nodeOther.x - nodeAtXY.x, nodeOther.y - nodeAtXY.y);
                        edgeVec.normalize();
                        if (move.dot(edgeVec) >= 0.0F) {
                            closestEdge = edge1;
                        }
                    }
                }
            }

            if (closestEdge != null) {
                if (render) {
                    float _x1 = closestEdge.node1.x;
                    float _y1 = closestEdge.node1.y;
                    float _x2 = closestEdge.node2.x;
                    float _y2 = closestEdge.node2.y;
                    LineDrawer.addLine(_x1, _y1, closestEdge.node1.z, _x2, _y2, closestEdge.node1.z, 0.0F, 1.0F, 1.0F, null, true);
                }

                this.closestPointOnEdge.distSq = Double.MAX_VALUE;
                closestEdge.getClosestPointOnEdge(nx, ny, this.closestPointOnEdge);
                finalPos.set(this.closestPointOnEdge.point.x, this.closestPointOnEdge.point.y);
            }

            return finalPos;
        }
    }

    public boolean canStandAt(float x, float y, float z, BaseVehicle ignoreVehicle, int flags) {
        boolean ignoreDoors = (flags & 1) != 0;
        boolean closeToWalls = (flags & 2) != 0;
        float x1 = x - 0.3F;
        float y1 = y - 0.3F;
        float x2 = x + 0.3F;
        float y2 = y + 0.3F;
        this.nodes.clear();
        this.obstacles.clear();
        this.getObstaclesInRect(
            Math.min(x1, x2),
            Math.min(y1, y2),
            Math.max(x1, x2),
            Math.max(y1, y2),
            PZMath.fastfloor(x),
            PZMath.fastfloor(y),
            PZMath.fastfloor(z),
            ignoreVehicle == null
        );

        for (int i = 0; i < this.obstacles.size(); i++) {
            CollideWithObstaclesPoly.CCObstacle obstacle = this.obstacles.get(i);
            if ((ignoreVehicle == null || obstacle.vehicle != ignoreVehicle) && obstacle.isPointInside(x, y, flags)) {
                return false;
            }
        }

        return true;
    }

    public boolean isNotClear(
        float x0, float y0, float x1, float y1, int z, boolean render, BaseVehicle ignoreVehicle, boolean ignoreDoors, boolean closeToWalls
    ) {
        float sx = x0;
        float sy = y0;
        float ex = x1;
        float ey = y1;
        x0 /= 8.0F;
        y0 /= 8.0F;
        x1 /= 8.0F;
        y1 /= 8.0F;
        double dx = Math.abs(x1 - x0);
        double dy = Math.abs(y1 - y0);
        int x = PZMath.fastfloor(x0);
        int y = PZMath.fastfloor(y0);
        int n = 1;
        int x_inc;
        double error;
        if (dx == 0.0) {
            x_inc = 0;
            error = Double.POSITIVE_INFINITY;
        } else if (x1 > x0) {
            x_inc = 1;
            n += PZMath.fastfloor(x1) - x;
            error = (PZMath.fastfloor(x0) + 1.0F - x0) * dy;
        } else {
            x_inc = -1;
            n += x - PZMath.fastfloor(x1);
            error = (x0 - PZMath.fastfloor(x0)) * dy;
        }

        int y_inc;
        if (dy == 0.0) {
            y_inc = 0;
            error -= Double.POSITIVE_INFINITY;
        } else if (y1 > y0) {
            y_inc = 1;
            n += PZMath.fastfloor(y1) - y;
            error -= (PZMath.fastfloor(y0) + 1.0F - y0) * dx;
        } else {
            y_inc = -1;
            n += y - PZMath.fastfloor(y1);
            error -= (y0 - PZMath.fastfloor(y0)) * dx;
        }

        for (; n > 0; n--) {
            IsoChunk chunk = GameServer.server ? ServerMap.instance.getChunk(x, y) : IsoWorld.instance.currentCell.getChunk(x, y);
            if (chunk != null) {
                if (render) {
                    int CPW = 8;
                    LineDrawer.addRect(x * 8, y * 8, z, 8.0F, 8.0F, 1.0F, 1.0F, 1.0F);
                }

                CollideWithObstaclesPoly.ChunkDataZ chunkDataZ = chunk.collision.init(chunk, z, this);
                ArrayList<CollideWithObstaclesPoly.CCObstacle> obstacles = ignoreVehicle == null
                    ? chunkDataZ.worldVehicleUnion
                    : chunkDataZ.worldVehicleSeparate;

                for (int i = 0; i < obstacles.size(); i++) {
                    CollideWithObstaclesPoly.CCObstacle obstacle = obstacles.get(i);
                    if ((ignoreVehicle == null || obstacle.vehicle != ignoreVehicle) && obstacle.lineSegmentIntersects(sx, sy, ex, ey, render)) {
                        return true;
                    }
                }
            }

            if (error > 0.0) {
                y += y_inc;
                error -= dx;
            } else {
                x += x_inc;
                error += dy;
            }
        }

        return false;
    }

    private void vehicleMoved(VehiclePoly poly) {
        int PAD = 2;
        int minX = (int)Math.min(poly.x1, Math.min(poly.x2, Math.min(poly.x3, poly.x4)));
        int minY = (int)Math.min(poly.y1, Math.min(poly.y2, Math.min(poly.y3, poly.y4)));
        int maxX = (int)Math.max(poly.x1, Math.max(poly.x2, Math.max(poly.x3, poly.x4)));
        int maxY = (int)Math.max(poly.y1, Math.max(poly.y2, Math.max(poly.y3, poly.y4)));
        int z = PZMath.fastfloor(poly.z);
        int chunkMinX = (minX - 2) / 8;
        int chunkMinY = (minY - 2) / 8;
        int chunkMaxX = (int)Math.ceil((maxX + 2 - 1.0F) / 8.0F);
        int chunkMaxY = (int)Math.ceil((maxY + 2 - 1.0F) / 8.0F);

        for (int y = chunkMinY; y <= chunkMaxY; y++) {
            for (int x = chunkMinX; x <= chunkMaxX; x++) {
                IsoChunk chunk = IsoWorld.instance.currentCell.getChunk(x, y);
                if (chunk != null && chunk.collision.data[z] != null) {
                    CollideWithObstaclesPoly.ChunkDataZ chunkDataZ = chunk.collision.data[z];
                    chunk.collision.data[z] = null;
                    chunkDataZ.clear();
                    CollideWithObstaclesPoly.ChunkDataZ.pool.release(chunkDataZ);
                }
            }
        }
    }

    public void vehicleMoved(VehiclePoly oldPolyPlusRadius, VehiclePoly newPolyPlusRadius) {
        this.vehicleMoved(oldPolyPlusRadius);
        this.vehicleMoved(newPolyPlusRadius);
    }

    public void render() {
        boolean render = Core.debug && DebugOptions.instance.collideWithObstacles.render.obstacles.getValue();
        if (render) {
            IsoPlayer player = IsoPlayer.getInstance();
            if (player == null) {
                return;
            }

            this.nodes.clear();
            this.obstacles.clear();
            this.getObstaclesInRect(
                player.getX(),
                player.getY(),
                player.getX(),
                player.getY(),
                PZMath.fastfloor(player.getX()),
                PZMath.fastfloor(player.getY()),
                PZMath.fastfloor(player.getZ()),
                true
            );
            if (DebugOptions.instance.collideWithObstacles.render.normals.getValue()) {
                for (CollideWithObstaclesPoly.CCNode node : this.nodes) {
                    if (node.getNormalAndEdgeVectors(nodeNormal, edgeVec)) {
                        LineDrawer.addLine(node.x, node.y, node.z, node.x + nodeNormal.x, node.y + nodeNormal.y, node.z, 0.0F, 0.0F, 1.0F, null, true);
                    }
                }
            }

            for (CollideWithObstaclesPoly.CCObstacle obstacle : this.obstacles) {
                obstacle.render();
            }
        }
    }

    private static final class CCEdge {
        private CollideWithObstaclesPoly.CCNode node1;
        private CollideWithObstaclesPoly.CCNode node2;
        private CollideWithObstaclesPoly.CCObstacle obstacle;
        private final Vector2 normal = new Vector2();
        private static final ObjectPool<CollideWithObstaclesPoly.CCEdge> pool = new ObjectPool<>(CollideWithObstaclesPoly.CCEdge::new);

        private CollideWithObstaclesPoly.CCEdge init(
            CollideWithObstaclesPoly.CCNode node1, CollideWithObstaclesPoly.CCNode node2, CollideWithObstaclesPoly.CCObstacle obstacle
        ) {
            if (node1.x == node2.x && node1.y == node2.y) {
                boolean var4 = false;
            }

            this.node1 = node1;
            this.node2 = node2;
            node1.edges.add(this);
            node2.edges.add(this);
            this.obstacle = obstacle;
            this.normal.set(node2.x - node1.x, node2.y - node1.y);
            this.normal.normalize();
            this.normal.rotate((float)Math.toRadians(90.0));
            return this;
        }

        private boolean hasNode(CollideWithObstaclesPoly.CCNode node) {
            return node == this.node1 || node == this.node2;
        }

        private void getClosestPointOnEdge(float x3, float y3, CollideWithObstaclesPoly.ClosestPointOnEdge out) {
            float x1 = this.node1.x;
            float y1 = this.node1.y;
            float x2 = this.node2.x;
            float y2 = this.node2.y;
            double u = ((x3 - x1) * (x2 - x1) + (y3 - y1) * (y2 - y1)) / (Math.pow(x2 - x1, 2.0) + Math.pow(y2 - y1, 2.0));
            double xu = x1 + u * (x2 - x1);
            double yu = y1 + u * (y2 - y1);
            double epsilon = 0.001;
            CollideWithObstaclesPoly.CCNode node = null;
            if (u <= 0.001) {
                xu = x1;
                yu = y1;
                node = this.node1;
            } else if (u >= 0.999) {
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

        private boolean isPointOn(float x3, float y3) {
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

        private static CollideWithObstaclesPoly.CCEdge alloc() {
            return pool.alloc();
        }

        private void release() {
            pool.release(this);
        }

        private static void releaseAll(ArrayList<CollideWithObstaclesPoly.CCEdge> objs) {
            pool.releaseAll(objs);
        }
    }

    private static final class CCEdgeRing extends ArrayList<CollideWithObstaclesPoly.CCEdge> {
        private static final ObjectPool<CollideWithObstaclesPoly.CCEdgeRing> pool = new ObjectPool<CollideWithObstaclesPoly.CCEdgeRing>(
            CollideWithObstaclesPoly.CCEdgeRing::new
        ) {
            public void release(CollideWithObstaclesPoly.CCEdgeRing obj) {
                CollideWithObstaclesPoly.CCEdge.releaseAll(obj);
                this.clear();
                super.release(obj);
            }
        };

        private float isLeft(float x0, float y0, float x1, float y1, float x2, float y2) {
            return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
        }

        private CollideWithObstaclesPoly.EdgeRingHit isPointInPolygon_WindingNumber(float x, float y, int flags) {
            int wn = 0;

            for (int i = 0; i < this.size(); i++) {
                CollideWithObstaclesPoly.CCEdge edge = this.get(i);
                if ((flags & 16) != 0 && edge.isPointOn(x, y)) {
                    return CollideWithObstaclesPoly.EdgeRingHit.OnEdge;
                }

                if (edge.node1.y <= y) {
                    if (edge.node2.y > y && this.isLeft(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y, x, y) > 0.0F) {
                        wn++;
                    }
                } else if (edge.node2.y <= y && this.isLeft(edge.node1.x, edge.node1.y, edge.node2.x, edge.node2.y, x, y) < 0.0F) {
                    wn--;
                }
            }

            return wn == 0 ? CollideWithObstaclesPoly.EdgeRingHit.Outside : CollideWithObstaclesPoly.EdgeRingHit.Inside;
        }

        private boolean lineSegmentIntersects(float sx, float sy, float ex, float ey, boolean render, boolean outer) {
            CollideWithObstaclesPoly.move.set(ex - sx, ey - sy);
            float lineSegmentLength = CollideWithObstaclesPoly.move.getLength();
            CollideWithObstaclesPoly.move.normalize();
            float dirX = CollideWithObstaclesPoly.move.x;
            float dirY = CollideWithObstaclesPoly.move.y;

            for (int j = 0; j < this.size(); j++) {
                CollideWithObstaclesPoly.CCEdge edge = this.get(j);
                if (!edge.isPointOn(sx, sy) && !edge.isPointOn(ex, ey)) {
                    float dot = edge.normal.dot(CollideWithObstaclesPoly.move);
                    if (!(dot >= 0.01F)) {
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
                                float px = sx + t * dirX;
                                float py = sy + t * dirY;
                                if (render) {
                                    this.render(outer);
                                    LineDrawer.addRect(px - 0.05F, py - 0.05F, edge.node1.z, 0.1F, 0.1F, 1.0F, 1.0F, 1.0F);
                                }

                                return true;
                            }
                        }
                    }
                }
            }

            return this.isPointInPolygon_WindingNumber((sx + ex) / 2.0F, (sy + ey) / 2.0F, 0) != CollideWithObstaclesPoly.EdgeRingHit.Outside;
        }

        private void lineSegmentIntersect(
            float x1, float y1, float x2, float y2, CollideWithObstaclesPoly.ClosestPointOnEdge closestPointOnEdge, boolean render
        ) {
            CollideWithObstaclesPoly.move.set(x2 - x1, y2 - y1).normalize();

            for (int j = 0; j < this.size(); j++) {
                CollideWithObstaclesPoly.CCEdge edge = this.get(j);
                float dot = edge.normal.dot(CollideWithObstaclesPoly.move);
                if (!(dot >= 0.0F)) {
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
                        if (ua >= 0.0 && ua <= 1.0 && ub >= 0.0 && ub <= 1.0) {
                            if (ub < 0.01 || ub > 0.99) {
                                CollideWithObstaclesPoly.CCNode node = ub < 0.01 ? edge.node1 : edge.node2;
                                double distSq = IsoUtils.DistanceToSquared(x1, y1, node.x, node.y);
                                if (distSq >= closestPointOnEdge.distSq) {
                                    continue;
                                }

                                if (node.getNormalAndEdgeVectors(CollideWithObstaclesPoly.nodeNormal, CollideWithObstaclesPoly.edgeVec)) {
                                    if (!(
                                        CollideWithObstaclesPoly.nodeNormal.dot(CollideWithObstaclesPoly.move) + 0.05F
                                            >= CollideWithObstaclesPoly.nodeNormal.dot(CollideWithObstaclesPoly.edgeVec)
                                    )) {
                                        closestPointOnEdge.edge = edge;
                                        closestPointOnEdge.node = node;
                                        closestPointOnEdge.distSq = distSq;
                                    }
                                    continue;
                                }
                            }

                            float intersectX = (float)(x1 + ua * (x2 - x1));
                            float intersectY = (float)(y1 + ua * (y2 - y1));
                            double distSqx = IsoUtils.DistanceToSquared(x1, y1, intersectX, intersectY);
                            if (distSqx < closestPointOnEdge.distSq) {
                                closestPointOnEdge.edge = edge;
                                closestPointOnEdge.node = null;
                                closestPointOnEdge.distSq = distSqx;
                            }
                        }
                    }
                }
            }
        }

        private void getClosestPointOnEdge(float x3, float y3, CollideWithObstaclesPoly.ClosestPointOnEdge out) {
            for (int i = 0; i < this.size(); i++) {
                CollideWithObstaclesPoly.CCEdge edge = this.get(i);
                edge.getClosestPointOnEdge(x3, y3, out);
            }
        }

        private void render(boolean outer) {
            if (!this.isEmpty()) {
                float r = 0.0F;
                float g = outer ? 1.0F : 0.5F;
                float b = outer ? 0.0F : 0.5F;
                BaseVehicle.Vector3fObjectPool pool = BaseVehicle.TL_vector3f_pool.get();

                for (CollideWithObstaclesPoly.CCEdge edge : this) {
                    CollideWithObstaclesPoly.CCNode n1 = edge.node1;
                    CollideWithObstaclesPoly.CCNode n2 = edge.node2;
                    LineDrawer.addLine(n1.x, n1.y, n1.z, n2.x, n2.y, n2.z, 0.0F, g, b, null, true);
                    boolean PolymapRenderEdgeDirection = false;
                }

                CollideWithObstaclesPoly.CCNode node1 = this.get(0).node1;
                LineDrawer.addRect(node1.x - 0.1F, node1.y - 0.1F, node1.z, 0.2F, 0.2F, 1.0F, 0.0F, 0.0F);
            }
        }

        private static void releaseAll(ArrayList<CollideWithObstaclesPoly.CCEdgeRing> objs) {
            pool.releaseAll(objs);
        }
    }

    private static final class CCNode {
        private float x;
        private float y;
        private int z;
        private final ArrayList<CollideWithObstaclesPoly.CCEdge> edges = new ArrayList<>();
        private static final ObjectPool<CollideWithObstaclesPoly.CCNode> pool = new ObjectPool<>(CollideWithObstaclesPoly.CCNode::new);

        private CollideWithObstaclesPoly.CCNode init(float x, float y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.edges.clear();
            return this;
        }

        private CollideWithObstaclesPoly.CCNode setXY(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }

        private boolean getNormalAndEdgeVectors(Vector2 normal, Vector2 edgeVec) {
            CollideWithObstaclesPoly.CCEdge edge1 = null;
            CollideWithObstaclesPoly.CCEdge edge2 = null;

            for (int i = 0; i < this.edges.size(); i++) {
                CollideWithObstaclesPoly.CCEdge edge = this.edges.get(i);
                if (edge1 == null) {
                    edge1 = edge;
                } else if (!edge1.hasNode(edge.node1) || !edge1.hasNode(edge.node2)) {
                    edge2 = edge;
                }
            }

            if (edge1 != null && edge2 != null) {
                float vx = edge1.normal.x + edge2.normal.x;
                float vy = edge1.normal.y + edge2.normal.y;
                normal.set(vx, vy);
                normal.normalize();
                if (edge1.node1 == this) {
                    edgeVec.set(edge1.node2.x - edge1.node1.x, edge1.node2.y - edge1.node1.y);
                } else {
                    edgeVec.set(edge1.node1.x - edge1.node2.x, edge1.node1.y - edge1.node2.y);
                }

                edgeVec.normalize();
                return true;
            } else {
                return false;
            }
        }

        private static CollideWithObstaclesPoly.CCNode alloc() {
            return pool.alloc();
        }

        private void release() {
            pool.release(this);
        }

        private static void releaseAll(ArrayList<CollideWithObstaclesPoly.CCNode> objs) {
            pool.releaseAll(objs);
        }
    }

    private static final class CCObstacle {
        private final CollideWithObstaclesPoly.CCEdgeRing outer = new CollideWithObstaclesPoly.CCEdgeRing();
        private final ArrayList<CollideWithObstaclesPoly.CCEdgeRing> inner = new ArrayList<>();
        private BaseVehicle vehicle;
        private CollideWithObstaclesPoly.ImmutableRectF bounds;
        private static final ObjectPool<CollideWithObstaclesPoly.CCObstacle> pool = new ObjectPool<CollideWithObstaclesPoly.CCObstacle>(
            CollideWithObstaclesPoly.CCObstacle::new
        ) {
            public void release(CollideWithObstaclesPoly.CCObstacle obj) {
                CollideWithObstaclesPoly.CCEdge.releaseAll(obj.outer);
                CollideWithObstaclesPoly.CCEdgeRing.releaseAll(obj.inner);
                obj.outer.clear();
                obj.inner.clear();
                obj.vehicle = null;
                super.release(obj);
            }
        };

        private CollideWithObstaclesPoly.CCObstacle init() {
            this.outer.clear();
            this.inner.clear();
            this.vehicle = null;
            return this;
        }

        private boolean isPointInside(float x, float y, int flags) {
            if (this.outer.isPointInPolygon_WindingNumber(x, y, flags) != CollideWithObstaclesPoly.EdgeRingHit.Inside) {
                return false;
            } else if (this.inner.isEmpty()) {
                return true;
            } else {
                for (int i = 0; i < this.inner.size(); i++) {
                    CollideWithObstaclesPoly.CCEdgeRing edges = this.inner.get(i);
                    if (edges.isPointInPolygon_WindingNumber(x, y, flags) != CollideWithObstaclesPoly.EdgeRingHit.Outside) {
                        return false;
                    }
                }

                return true;
            }
        }

        private boolean lineSegmentIntersects(float sx, float sy, float ex, float ey, boolean render) {
            if (this.outer.lineSegmentIntersects(sx, sy, ex, ey, render, true)) {
                return true;
            } else {
                for (int i = 0; i < this.inner.size(); i++) {
                    CollideWithObstaclesPoly.CCEdgeRing edges = this.inner.get(i);
                    if (edges.lineSegmentIntersects(sx, sy, ex, ey, render, false)) {
                        return true;
                    }
                }

                return false;
            }
        }

        private void lineSegmentIntersect(
            float x1, float y1, float x2, float y2, CollideWithObstaclesPoly.ClosestPointOnEdge closestPointOnEdge, boolean render
        ) {
            this.outer.lineSegmentIntersect(x1, y1, x2, y2, closestPointOnEdge, render);

            for (int i = 0; i < this.inner.size(); i++) {
                CollideWithObstaclesPoly.CCEdgeRing edges = this.inner.get(i);
                edges.lineSegmentIntersect(x1, y1, x2, y2, closestPointOnEdge, render);
            }
        }

        private void getClosestPointOnEdge(float x3, float y3, CollideWithObstaclesPoly.ClosestPointOnEdge out) {
            this.outer.getClosestPointOnEdge(x3, y3, out);

            for (int i = 0; i < this.inner.size(); i++) {
                CollideWithObstaclesPoly.CCEdgeRing edges = this.inner.get(i);
                edges.getClosestPointOnEdge(x3, y3, out);
            }
        }

        private void calcBounds() {
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE;
            float maxY = Float.MIN_VALUE;

            for (int i = 0; i < this.outer.size(); i++) {
                CollideWithObstaclesPoly.CCEdge edge = this.outer.get(i);
                minX = Math.min(minX, edge.node1.x);
                minY = Math.min(minY, edge.node1.y);
                maxX = Math.max(maxX, edge.node1.x);
                maxY = Math.max(maxY, edge.node1.y);
            }

            if (this.bounds != null) {
                this.bounds.release();
            }

            float epsilon = 0.01F;
            this.bounds = CollideWithObstaclesPoly.ImmutableRectF.alloc().init(minX - 0.01F, minY - 0.01F, maxX - minX + 0.02F, maxY - minY + 0.02F);
        }

        private void render() {
            this.outer.render(true);

            for (int i = 0; i < this.inner.size(); i++) {
                this.inner.get(i).render(false);
            }
        }

        private static CollideWithObstaclesPoly.CCObstacle alloc() {
            return pool.alloc();
        }

        private void release() {
            pool.release(this);
        }

        private static void releaseAll(ArrayList<CollideWithObstaclesPoly.CCObstacle> objs) {
            pool.releaseAll(objs);
        }
    }

    public static final class ChunkData {
        private final CollideWithObstaclesPoly.ChunkDataZ[] data = new CollideWithObstaclesPoly.ChunkDataZ[8];
        private boolean clear;

        public CollideWithObstaclesPoly.ChunkDataZ init(IsoChunk chunk, int z, CollideWithObstaclesPoly instance) {
            assert Thread.currentThread() == GameWindow.gameThread;

            if (this.clear) {
                this.clear = false;
                this.clearInner();
            }

            if (this.data[z] == null) {
                this.data[z] = CollideWithObstaclesPoly.ChunkDataZ.pool.alloc();
                this.data[z].init(chunk, z, instance);
            }

            return this.data[z];
        }

        private void clearInner() {
            PZArrayUtil.forEach(this.data, e -> {
                if (e != null) {
                    e.clear();
                    CollideWithObstaclesPoly.ChunkDataZ.pool.release(e);
                }
            });
            Arrays.fill(this.data, null);
        }

        public void clear() {
            this.clear = true;
        }
    }

    public static final class ChunkDataZ {
        public final ArrayList<CollideWithObstaclesPoly.CCObstacle> worldVehicleUnion = new ArrayList<>();
        public final ArrayList<CollideWithObstaclesPoly.CCObstacle> worldVehicleSeparate = new ArrayList<>();
        public final ArrayList<CollideWithObstaclesPoly.CCNode> nodes = new ArrayList<>();
        public int z;
        public static final ObjectPool<CollideWithObstaclesPoly.ChunkDataZ> pool = new ObjectPool<>(CollideWithObstaclesPoly.ChunkDataZ::new);

        public void init(IsoChunk chunk, int z, CollideWithObstaclesPoly instance) {
            this.z = z;
            Clipper clipper = instance.clipper;
            clipper.clear();
            boolean BEVEL = true;
            float BEVEL_RADIUS = 0.19800001F;
            boolean OFFSET = false;
            int cx = chunk.wx * 8;
            int cy = chunk.wy * 8;

            for (int y = cy - 2; y < cy + 8 + 2; y++) {
                for (int x = cx - 2; x < cx + 8 + 2; x++) {
                    IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                    if (square != null && !square.getObjects().isEmpty()) {
                        if (square.isSolid() || square.isSolidTrans() && !square.isAdjacentToWindow() && !square.isAdjacentToHoppable()) {
                            clipper.addAABBBevel(x - 0.3F, y - 0.3F, x + 1.0F + 0.3F, y + 1.0F + 0.3F, 0.19800001F);
                        }

                        boolean bCollideW = square.has(IsoFlagType.collideW) || square.hasBlockedDoor(false) || square.HasStairsNorth();
                        if (square.has(IsoFlagType.windowW) || square.has(IsoFlagType.WindowW)) {
                            bCollideW = true;
                        }

                        if (bCollideW) {
                            if (!this.isCollideW(x, y - 1, z)) {
                            }

                            boolean doorN = false;
                            if (!this.isCollideW(x, y + 1, z)) {
                            }

                            boolean doorS = false;
                            clipper.addAABBBevel(x - 0.3F, y - (doorN ? 0.0F : 0.3F), x + 0.3F, y + 1.0F + (doorS ? 0.0F : 0.3F), 0.19800001F);
                        }

                        boolean bCollideN = square.has(IsoFlagType.collideN) || square.hasBlockedDoor(true) || square.HasStairsWest();
                        if (square.has(IsoFlagType.windowN) || square.has(IsoFlagType.WindowN)) {
                            bCollideN = true;
                        }

                        if (bCollideN) {
                            if (!this.isCollideN(x - 1, y, z)) {
                            }

                            boolean doorW = false;
                            if (!this.isCollideN(x + 1, y, z)) {
                            }

                            boolean doorE = false;
                            clipper.addAABBBevel(x - (doorW ? 0.0F : 0.3F), y - 0.3F, x + 1.0F + (doorE ? 0.0F : 0.3F), y + 0.3F, 0.19800001F);
                        }

                        if (square.HasStairsNorth()) {
                            IsoGridSquare square2 = IsoWorld.instance.currentCell.getGridSquare(x + 1, y, z);
                            if (square2 != null) {
                                clipper.addAABBBevel(x + 1 - 0.3F, y - 0.3F, x + 1.0F + 0.3F, y + 1.0F + 0.3F, 0.19800001F);
                            }

                            if (square.has(IsoObjectType.stairsTN)) {
                                IsoGridSquare below = IsoWorld.instance.currentCell.getGridSquare(x, y, z - 1);
                                if (below == null || !below.has(IsoObjectType.stairsTN)) {
                                    clipper.addAABBBevel(x - 0.3F, y - 0.3F, x + 1.0F + 0.3F, y + 0.3F, 0.19800001F);
                                    float moveOntoNextSquareALittle = 0.1F;
                                    clipper.clipAABB(x + 0.3F, y - 0.1F, x + 1.0F - 0.3F, y + 0.3F);
                                }
                            }
                        }

                        if (square.HasStairsWest()) {
                            IsoGridSquare square2x = IsoWorld.instance.currentCell.getGridSquare(x, y + 1, z);
                            if (square2x != null) {
                                clipper.addAABBBevel(x - 0.3F, y + 1.0F - 0.3F, x + 1.0F + 0.3F, y + 1.0F + 0.3F, 0.19800001F);
                            }

                            if (square.has(IsoObjectType.stairsTW)) {
                                IsoGridSquare below = IsoWorld.instance.currentCell.getGridSquare(x, y, z - 1);
                                if (below == null || !below.has(IsoObjectType.stairsTW)) {
                                    clipper.addAABBBevel(x - 0.3F, y - 0.3F, x + 0.3F, y + 1.0F + 0.3F, 0.19800001F);
                                    float moveOntoNextSquareALittle = 0.1F;
                                    clipper.clipAABB(x - 0.1F, y + 0.3F, x + 0.3F, y + 1.0F - 0.3F);
                                }
                            }
                        }
                    }
                }
            }

            ByteBuffer xyBuffer = instance.xyBuffer;

            assert this.worldVehicleSeparate.isEmpty();

            this.clipperToObstacles(clipper, xyBuffer, this.worldVehicleSeparate);
            int x1 = chunk.wx * 8;
            int y1 = chunk.wy * 8;
            int x2 = x1 + 8;
            int y2 = y1 + 8;
            x1 -= 2;
            y1 -= 2;
            x2 += 2;
            y2 += 2;
            CollideWithObstaclesPoly.ImmutableRectF chunkBounds = instance.moveBounds.init((float)x1, (float)y1, (float)(x2 - x1), (float)(y2 - y1));
            instance.getVehiclesInRect((float)(x1 - 5), (float)(y1 - 5), (float)(x2 + 5), (float)(y2 + 5), z);

            for (int i = 0; i < instance.vehicles.size(); i++) {
                BaseVehicle vehicle = instance.vehicles.get(i);
                VehiclePoly poly = vehicle.getPolyPlusRadius();
                float xMin = Math.min(poly.x1, Math.min(poly.x2, Math.min(poly.x3, poly.x4)));
                float yMin = Math.min(poly.y1, Math.min(poly.y2, Math.min(poly.y3, poly.y4)));
                float xMax = Math.max(poly.x1, Math.max(poly.x2, Math.max(poly.x3, poly.x4)));
                float yMax = Math.max(poly.y1, Math.max(poly.y2, Math.max(poly.y3, poly.y4)));
                instance.vehicleBounds.init(xMin, yMin, xMax - xMin, yMax - yMin);
                if (chunkBounds.intersects(instance.vehicleBounds)) {
                    clipper.addPolygon(poly.x1, poly.y1, poly.x4, poly.y4, poly.x3, poly.y3, poly.x2, poly.y2);
                    CollideWithObstaclesPoly.CCNode node1 = CollideWithObstaclesPoly.CCNode.alloc().init(poly.x1, poly.y1, z);
                    CollideWithObstaclesPoly.CCNode node2 = CollideWithObstaclesPoly.CCNode.alloc().init(poly.x2, poly.y2, z);
                    CollideWithObstaclesPoly.CCNode node3 = CollideWithObstaclesPoly.CCNode.alloc().init(poly.x3, poly.y3, z);
                    CollideWithObstaclesPoly.CCNode node4 = CollideWithObstaclesPoly.CCNode.alloc().init(poly.x4, poly.y4, z);
                    CollideWithObstaclesPoly.CCObstacle obstacle = CollideWithObstaclesPoly.CCObstacle.alloc().init();
                    obstacle.vehicle = vehicle;
                    CollideWithObstaclesPoly.CCEdge edge1 = CollideWithObstaclesPoly.CCEdge.alloc().init(node1, node2, obstacle);
                    CollideWithObstaclesPoly.CCEdge edge2 = CollideWithObstaclesPoly.CCEdge.alloc().init(node2, node3, obstacle);
                    CollideWithObstaclesPoly.CCEdge edge3 = CollideWithObstaclesPoly.CCEdge.alloc().init(node3, node4, obstacle);
                    CollideWithObstaclesPoly.CCEdge edge4 = CollideWithObstaclesPoly.CCEdge.alloc().init(node4, node1, obstacle);
                    obstacle.outer.add(edge1);
                    obstacle.outer.add(edge2);
                    obstacle.outer.add(edge3);
                    obstacle.outer.add(edge4);
                    obstacle.calcBounds();
                    this.worldVehicleSeparate.add(obstacle);
                    this.nodes.add(node1);
                    this.nodes.add(node2);
                    this.nodes.add(node3);
                    this.nodes.add(node4);
                }
            }

            assert this.worldVehicleUnion.isEmpty();

            this.clipperToObstacles(clipper, xyBuffer, this.worldVehicleUnion);
        }

        private void getEdgesFromBuffer(ByteBuffer polyBuffer, CollideWithObstaclesPoly.CCObstacle obstacle, boolean outer) {
            int pointCount = polyBuffer.getShort();
            if (pointCount < 3) {
                polyBuffer.position(polyBuffer.position() + pointCount * 4 * 2);
            } else {
                CollideWithObstaclesPoly.CCEdgeRing edges = obstacle.outer;
                if (!outer) {
                    edges = CollideWithObstaclesPoly.CCEdgeRing.pool.alloc();
                    edges.clear();
                    obstacle.inner.add(edges);
                }

                int nodeFirst = this.nodes.size();

                for (int j = 0; j < pointCount; j++) {
                    float x = polyBuffer.getFloat();
                    float y = polyBuffer.getFloat();
                    CollideWithObstaclesPoly.CCNode node1 = CollideWithObstaclesPoly.CCNode.alloc().init(x, y, this.z);
                    this.nodes.add(nodeFirst, node1);
                }

                for (int j = nodeFirst; j < this.nodes.size() - 1; j++) {
                    CollideWithObstaclesPoly.CCNode node1 = this.nodes.get(j);
                    CollideWithObstaclesPoly.CCNode node2 = this.nodes.get(j + 1);
                    CollideWithObstaclesPoly.CCEdge edge1 = CollideWithObstaclesPoly.CCEdge.alloc().init(node1, node2, obstacle);
                    edges.add(edge1);
                }

                CollideWithObstaclesPoly.CCNode node1 = this.nodes.get(this.nodes.size() - 1);
                CollideWithObstaclesPoly.CCNode node2 = this.nodes.get(nodeFirst);
                edges.add(CollideWithObstaclesPoly.CCEdge.alloc().init(node1, node2, obstacle));
            }
        }

        private void clipperToObstacles(Clipper clipper, ByteBuffer polyBuffer, ArrayList<CollideWithObstaclesPoly.CCObstacle> obstacles) {
            int polyCount = clipper.generatePolygons();

            for (int i = 0; i < polyCount; i++) {
                polyBuffer.clear();
                clipper.getPolygon(i, polyBuffer);
                CollideWithObstaclesPoly.CCObstacle obstacle = CollideWithObstaclesPoly.CCObstacle.alloc().init();
                this.getEdgesFromBuffer(polyBuffer, obstacle, true);
                short holeCount = polyBuffer.getShort();

                for (int j = 0; j < holeCount; j++) {
                    this.getEdgesFromBuffer(polyBuffer, obstacle, false);
                }

                obstacle.calcBounds();
                obstacles.add(obstacle);
            }
        }

        private boolean isCollideW(int x, int y, int z) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            return square != null && (square.has(IsoFlagType.collideW) || square.hasBlockedDoor(false) || square.HasStairsNorth());
        }

        private boolean isCollideN(int x, int y, int z) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            return square != null && (square.has(IsoFlagType.collideN) || square.hasBlockedDoor(true) || square.HasStairsWest());
        }

        private boolean isOpenDoorAt(int x, int y, int z, boolean north) {
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            return square != null && square.getDoor(north) != null && !square.hasBlockedDoor(north);
        }

        public void clear() {
            CollideWithObstaclesPoly.CCNode.releaseAll(this.nodes);
            this.nodes.clear();
            CollideWithObstaclesPoly.CCObstacle.releaseAll(this.worldVehicleUnion);
            this.worldVehicleUnion.clear();
            CollideWithObstaclesPoly.CCObstacle.releaseAll(this.worldVehicleSeparate);
            this.worldVehicleSeparate.clear();
        }
    }

    private static final class ClosestPointOnEdge {
        private CollideWithObstaclesPoly.CCEdge edge;
        private CollideWithObstaclesPoly.CCNode node;
        private final Vector2f point = new Vector2f();
        private double distSq;
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
        private static final ArrayDeque<CollideWithObstaclesPoly.ImmutableRectF> pool = new ArrayDeque<>();

        private CollideWithObstaclesPoly.ImmutableRectF init(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        private float left() {
            return this.x;
        }

        private float top() {
            return this.y;
        }

        private float right() {
            return this.x + this.w;
        }

        private float bottom() {
            return this.y + this.h;
        }

        private float width() {
            return this.w;
        }

        private float height() {
            return this.h;
        }

        private boolean containsPoint(float x, float y) {
            return x >= this.left() && x < this.right() && y >= this.top() && y < this.bottom();
        }

        private boolean intersects(CollideWithObstaclesPoly.ImmutableRectF other) {
            return this.left() < other.right() && this.right() > other.left() && this.top() < other.bottom() && this.bottom() > other.top();
        }

        private static CollideWithObstaclesPoly.ImmutableRectF alloc() {
            return pool.isEmpty() ? new CollideWithObstaclesPoly.ImmutableRectF() : pool.pop();
        }

        private void release() {
            assert !pool.contains(this);

            pool.push(this);
        }
    }
}
