// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.debug.LineDrawer;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.vehicles.BaseVehicle;

final class LineClearCollideMain {
    final Vector2 perp = new Vector2();
    final ArrayList<Point> pts = new ArrayList<>();
    final VehicleRect sweepAabb = new VehicleRect();
    final VehicleRect vehicleAabb = new VehicleRect();
    final VehiclePoly vehiclePoly = new VehiclePoly();
    final Vector2[] polyVec = new Vector2[4];
    final Vector2[] vehicleVec = new Vector2[4];
    final PointPool pointPool = new PointPool();
    final LiangBarsky lb = new LiangBarsky();

    LineClearCollideMain() {
        for (int i = 0; i < 4; i++) {
            this.polyVec[i] = new Vector2();
            this.vehicleVec[i] = new Vector2();
        }
    }

    private float clamp(float f1, float min, float max) {
        if (f1 < min) {
            f1 = min;
        }

        if (f1 > max) {
            f1 = max;
        }

        return f1;
    }

    @Deprecated
    boolean canStandAtOld(PolygonalMap2 map, float x, float y, float z, BaseVehicle ignoreVehicle, int flags) {
        boolean ignoreDoors = (flags & 1) != 0;
        boolean closeToWalls = (flags & 2) != 0;
        int minX = PZMath.fastfloor(x - 0.3F);
        int minY = PZMath.fastfloor(y - 0.3F);
        int maxX = (int)Math.ceil(x + 0.3F);
        int maxY = (int)Math.ceil(y + 0.3F);

        for (int sy = minY; sy < maxY; sy++) {
            for (int sx = minX; sx < maxX; sx++) {
                boolean bTargetSquare = x >= sx && y >= sy && x < sx + 1 && y < sy + 1;
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(sx, sy, PZMath.fastfloor(z));
                boolean bHasFloor = square != null && (square.solidFloorCached ? square.solidFloor : square.TreatAsSolidFloor());
                if (square == null || square.getObjects().isEmpty()) {
                    IsoGridSquare below = IsoWorld.instance.currentCell.getGridSquare(sx, sy, PZMath.fastfloor(z) - 1);
                    if (below != null && below.getSlopedSurfaceHeight(0.5F, 0.5F) > 0.9F) {
                        bHasFloor = true;
                    }
                }

                if (square != null
                    && !square.isSolid()
                    && (!square.isSolidTrans() || square.isAdjacentToWindow() || square.isAdjacentToHoppable())
                    && bHasFloor) {
                    if (square.HasStairs()) {
                        if (!closeToWalls && !bTargetSquare) {
                            if (square.isStairsEdgeBlocked(IsoDirections.N)) {
                                float closestX = this.clamp(x, sx, sx + 1);
                                float closestY = sy;
                                float distanceX = x - closestX;
                                float distanceY = y - closestY;
                                float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                                if (distanceSquared < 0.09F) {
                                    return false;
                                }
                            }

                            if (square.isStairsEdgeBlocked(IsoDirections.S)) {
                                float closestX = this.clamp(x, sx, sx + 1);
                                float closestY = sy + 1;
                                float distanceX = x - closestX;
                                float distanceY = y - closestY;
                                float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                                if (distanceSquared < 0.09F) {
                                    return false;
                                }
                            }

                            if (square.isStairsEdgeBlocked(IsoDirections.W)) {
                                float closestX = sx;
                                float closestY = this.clamp(y, sy, sy + 1);
                                float distanceX = x - closestX;
                                float distanceY = y - closestY;
                                float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                                if (distanceSquared < 0.09F) {
                                    return false;
                                }
                            }

                            if (square.isStairsEdgeBlocked(IsoDirections.E)) {
                                float closestX = sx + 1;
                                float closestY = this.clamp(y, sy, sy + 1);
                                float distanceX = x - closestX;
                                float distanceY = y - closestY;
                                float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                                if (distanceSquared < 0.09F) {
                                    return false;
                                }
                            }
                        }
                    } else if (square.hasSlopedSurface()) {
                        if (!closeToWalls && !bTargetSquare) {
                            if (square.isSlopedSurfaceEdgeBlocked(IsoDirections.N)) {
                                float closestX = this.clamp(x, sx, sx + 1);
                                float closestY = sy;
                                float distanceX = x - closestX;
                                float distanceY = y - closestY;
                                float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                                if (distanceSquared < 0.09F) {
                                    return false;
                                }
                            }

                            if (square.isSlopedSurfaceEdgeBlocked(IsoDirections.S)) {
                                float closestX = this.clamp(x, sx, sx + 1);
                                float closestY = sy + 1;
                                float distanceX = x - closestX;
                                float distanceY = y - closestY;
                                float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                                if (distanceSquared < 0.09F) {
                                    return false;
                                }
                            }

                            if (square.isSlopedSurfaceEdgeBlocked(IsoDirections.W)) {
                                float closestX = sx;
                                float closestY = this.clamp(y, sy, sy + 1);
                                float distanceX = x - closestX;
                                float distanceY = y - closestY;
                                float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                                if (distanceSquared < 0.09F) {
                                    return false;
                                }
                            }

                            if (square.isSlopedSurfaceEdgeBlocked(IsoDirections.E)) {
                                float closestX = sx + 1;
                                float closestY = this.clamp(y, sy, sy + 1);
                                float distanceX = x - closestX;
                                float distanceY = y - closestY;
                                float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                                if (distanceSquared < 0.09F) {
                                    return false;
                                }
                            }
                        }
                    } else if (!closeToWalls) {
                        if (square.has(IsoFlagType.collideW) || !ignoreDoors && square.hasBlockedDoor(false)) {
                            float closestX = sx;
                            float closestY = this.clamp(y, sy, sy + 1);
                            float distanceX = x - closestX;
                            float distanceY = y - closestY;
                            float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                            if (distanceSquared < 0.09F) {
                                return false;
                            }
                        }

                        if (square.has(IsoFlagType.collideN) || !ignoreDoors && square.hasBlockedDoor(true)) {
                            float closestX = this.clamp(x, sx, sx + 1);
                            float closestY = sy;
                            float distanceX = x - closestX;
                            float distanceY = y - closestY;
                            float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                            if (distanceSquared < 0.09F) {
                                return false;
                            }
                        }
                    }
                } else if (closeToWalls) {
                    if (bTargetSquare) {
                        return false;
                    }
                } else {
                    float closestX = this.clamp(x, sx, sx + 1);
                    float closestY = this.clamp(y, sy, sy + 1);
                    float distanceX = x - closestX;
                    float distanceY = y - closestY;
                    float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                    if (distanceSquared < 0.09F) {
                        return false;
                    }
                }
            }
        }

        int chunkMinX = (PZMath.fastfloor(x) - 4) / 8 - 1;
        int chunkMinY = (PZMath.fastfloor(y) - 4) / 8 - 1;
        int chunkMaxX = (int)Math.ceil((x + 4.0F) / 8.0F) + 1;
        int chunkMaxY = (int)Math.ceil((y + 4.0F) / 8.0F) + 1;

        for (int cy = chunkMinY; cy < chunkMaxY; cy++) {
            for (int cx = chunkMinX; cx < chunkMaxX; cx++) {
                IsoChunk chunk = GameServer.server
                    ? ServerMap.instance.getChunk(cx, cy)
                    : IsoWorld.instance.currentCell.getChunkForGridSquare(cx * 8, cy * 8, 0);
                if (chunk != null) {
                    for (int i = 0; i < chunk.vehicles.size(); i++) {
                        BaseVehicle vehicle = chunk.vehicles.get(i);
                        if (vehicle != ignoreVehicle
                            && vehicle.addedToWorld
                            && PZMath.fastfloor(vehicle.getZ()) == PZMath.fastfloor(z)
                            && vehicle.getPolyPlusRadius().containsPoint(x, y)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    boolean canStandAtClipper(PolygonalMap2 map, float x, float y, float z, BaseVehicle ignoreVehicle, int flags) {
        return PolygonalMap2.instance.collideWithObstaclesPoly.canStandAt(x, y, z, ignoreVehicle, flags);
    }

    public void drawCircle(float x, float y, float z, float radius, float r, float g, float b, float a) {
        LineDrawer.DrawIsoCircle(x, y, z, radius, 16, r, g, b, a);
    }

    boolean isNotClearOld(PolygonalMap2 map, float fromX, float fromY, float toX, float toY, int z, BaseVehicle ignoreVehicle, int flags) {
        boolean ignoreDoors = (flags & 1) != 0;
        boolean closeToWalls = (flags & 2) != 0;
        boolean checkCost = (flags & 4) != 0;
        boolean render = (flags & 8) != 0;
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(PZMath.fastfloor(fromX), PZMath.fastfloor(fromY), z);
        if (!this.canStandAtOld(map, toX, toY, z, ignoreVehicle, flags)) {
            if (render) {
                this.drawCircle(toX, toY, z, 0.3F, 1.0F, 0.0F, 0.0F, 1.0F);
            }

            return true;
        } else {
            float perpX = toY - fromY;
            float perpY = -(toX - fromX);
            this.perp.set(perpX, perpY);
            this.perp.normalize();
            float x0 = fromX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
            float y0 = fromY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
            float x1 = toX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
            float y1 = toY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
            this.perp.set(-perpX, -perpY);
            this.perp.normalize();
            float x2 = fromX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
            float y2 = fromY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
            float x3 = toX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
            float y3 = toY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;

            for (int i = 0; i < this.pts.size(); i++) {
                this.pointPool.release(this.pts.get(i));
            }

            this.pts.clear();
            this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(fromX), PZMath.fastfloor(fromY)));
            if (PZMath.fastfloor(fromX) != PZMath.fastfloor(toX) || PZMath.fastfloor(fromY) != PZMath.fastfloor(toY)) {
                this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(toX), PZMath.fastfloor(toY)));
            }

            map.supercover(x0, y0, x1, y1, z, this.pointPool, this.pts);
            map.supercover(x2, y2, x3, y3, z, this.pointPool, this.pts);
            if (render) {
                for (int i = 0; i < this.pts.size(); i++) {
                    Point pt = this.pts.get(i);
                    LineDrawer.addLine(pt.x, pt.y, z, pt.x + 1.0F, pt.y + 1.0F, z, 1.0F, 1.0F, 0.0F, null, false);
                }
            }

            boolean collided = false;

            for (int i = 0; i < this.pts.size(); i++) {
                Point pt = this.pts.get(i);
                square = IsoWorld.instance.currentCell.getGridSquare(pt.x, pt.y, z);
                if (checkCost && square != null && SquareUpdateTask.getCost(square) > 0) {
                    return true;
                }

                boolean bHasFloor = square != null && (square.solidFloorCached ? square.solidFloor : square.TreatAsSolidFloor());
                if (square == null || square.getObjects().isEmpty()) {
                    IsoGridSquare below = IsoWorld.instance.currentCell.getGridSquare(pt.x, pt.y, z - 1);
                    if (below != null && below.getSlopedSurfaceHeight(0.5F, 0.5F) > 0.9F) {
                        bHasFloor = true;
                    }
                }

                if (square == null || square.isSolid() || square.isSolidTrans() && !square.isAdjacentToWindow() && !square.isAdjacentToHoppable() || !bHasFloor
                    )
                 {
                    float w = 0.3F;
                    float n = 0.3F;
                    float e = 0.3F;
                    float s = 0.3F;
                    if (fromX < pt.x && toX < pt.x) {
                        w = 0.0F;
                    } else if (fromX >= pt.x + 1 && toX >= pt.x + 1) {
                        e = 0.0F;
                    }

                    if (fromY < pt.y && toY < pt.y) {
                        n = 0.0F;
                    } else if (fromY >= pt.y + 1 && toY >= pt.y + 1) {
                        s = 0.0F;
                    }

                    if (this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, pt.x - w, pt.y - n, pt.x + 1.0F + e, pt.y + 1.0F + s)) {
                        if (!render) {
                            return true;
                        }

                        LineDrawer.addLine(pt.x - w, pt.y - n, z, pt.x + 1.0F + e, pt.y + 1.0F + s, z, 1.0F, 0.0F, 0.0F, null, false);
                        collided = true;
                    }
                } else if (square.HasStairs()) {
                    if (square.HasStairsNorth()) {
                        collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render);
                        if (collided && !render) {
                            return true;
                        }

                        collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x + 1, pt.y, true, false, z, render);
                        if (collided && !render) {
                            return true;
                        }

                        if (square.has(IsoObjectType.stairsTN)) {
                            collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render);
                            if (collided && !render) {
                                return true;
                            }
                        }
                    }

                    if (square.HasStairsWest()) {
                        collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render);
                        if (collided && !render) {
                            return true;
                        }

                        collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y + 1, true, false, z, render);
                        if (collided && !render) {
                            return true;
                        }

                        if (square.has(IsoObjectType.stairsTW)) {
                            collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render);
                            if (collided && !render) {
                                return true;
                            }
                        }
                    }
                } else if (square.hasSlopedSurface()) {
                    IsoDirections dir = square.getSlopedSurfaceDirection();
                    if (dir == IsoDirections.N || dir == IsoDirections.S) {
                        if (square.getAdjacentSquare(IsoDirections.W) == null || !square.hasIdenticalSlopedSurface(square.getAdjacentSquare(IsoDirections.W))) {
                            collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render);
                            if (collided && !render) {
                                return true;
                            }
                        }

                        if (square.getAdjacentSquare(IsoDirections.E) == null || !square.hasIdenticalSlopedSurface(square.getAdjacentSquare(IsoDirections.E))) {
                            collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x + 1, pt.y, true, false, z, render);
                            if (collided && !render) {
                                return true;
                            }
                        }

                        IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
                        if (dir == IsoDirections.N && (squareN == null || square.getSlopedSurfaceHeightMax() != squareN.getSlopedSurfaceHeight(0.5F, 1.0F))) {
                            collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render);
                            if (collided && !render) {
                                return true;
                            }
                        }

                        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
                        if (dir == IsoDirections.S && (squareS == null || square.getSlopedSurfaceHeightMax() != squareS.getSlopedSurfaceHeight(0.5F, 0.0F))) {
                            collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y + 1, true, false, z, render);
                            if (collided && !render) {
                                return true;
                            }
                        }
                    }

                    if (dir == IsoDirections.W || dir == IsoDirections.E) {
                        IsoGridSquare squareNx = square.getAdjacentSquare(IsoDirections.N);
                        if (squareNx == null || !square.hasIdenticalSlopedSurface(squareNx)) {
                            collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render);
                            if (collided && !render) {
                                return true;
                            }
                        }

                        IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
                        if (squareS == null || !square.hasIdenticalSlopedSurface(squareS)) {
                            collided |= this.testCollisionHorizontal(fromX, fromY, toX, toY, pt.x, pt.y + 1, true, false, z, render);
                            if (collided && !render) {
                                return true;
                            }
                        }

                        IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
                        if (dir == IsoDirections.W && (squareW == null || square.getSlopedSurfaceHeightMax() != squareW.getSlopedSurfaceHeight(1.0F, 0.5F))) {
                            collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x, pt.y, false, true, z, render);
                            if (collided && !render) {
                                return true;
                            }
                        }

                        IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
                        if (dir == IsoDirections.E && (squareE == null || square.getSlopedSurfaceHeightMax() != squareE.getSlopedSurfaceHeight(0.0F, 0.5F))) {
                            collided |= this.testCollisionVertical(fromX, fromY, toX, toY, pt.x + 1, pt.y, true, false, z, render);
                            if (collided && !render) {
                                return true;
                            }
                        }
                    }
                } else {
                    if (square.has(IsoFlagType.collideW) || !ignoreDoors && square.hasBlockedDoor(false)) {
                        float wx = 0.3F;
                        float nx = 0.3F;
                        float ex = 0.3F;
                        float sx = 0.3F;
                        if (fromX < pt.x && toX < pt.x) {
                            wx = 0.0F;
                        } else if (fromX >= pt.x && toX >= pt.x) {
                            ex = 0.0F;
                        }

                        if (fromY < pt.y && toY < pt.y) {
                            nx = 0.0F;
                        } else if (fromY >= pt.y + 1 && toY >= pt.y + 1) {
                            sx = 0.0F;
                        }

                        if (this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, pt.x - wx, pt.y - nx, pt.x + ex, pt.y + 1.0F + sx)) {
                            if (!render) {
                                return true;
                            }

                            LineDrawer.addLine(pt.x - wx, pt.y - nx, z, pt.x + ex, pt.y + 1.0F + sx, z, 1.0F, 0.0F, 0.0F, null, false);
                            collided = true;
                        }
                    }

                    if (square.has(IsoFlagType.collideN) || !ignoreDoors && square.hasBlockedDoor(true)) {
                        float wxx = 0.3F;
                        float nxx = 0.3F;
                        float exx = 0.3F;
                        float sxx = 0.3F;
                        if (fromX < pt.x && toX < pt.x) {
                            wxx = 0.0F;
                        } else if (fromX >= pt.x + 1 && toX >= pt.x + 1) {
                            exx = 0.0F;
                        }

                        if (fromY < pt.y && toY < pt.y) {
                            nxx = 0.0F;
                        } else if (fromY >= pt.y && toY >= pt.y) {
                            sxx = 0.0F;
                        }

                        if (this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, pt.x - wxx, pt.y - nxx, pt.x + 1.0F + exx, pt.y + sxx)) {
                            if (!render) {
                                return true;
                            }

                            LineDrawer.addLine(pt.x - wxx, pt.y - nxx, z, pt.x + 1.0F + exx, pt.y + sxx, z, 1.0F, 0.0F, 0.0F, null, false);
                            collided = true;
                        }
                    }
                }
            }

            float RADIUS = 0.15F;
            this.perp.set(perpX, perpY);
            this.perp.normalize();
            x0 = fromX + this.perp.x * 0.15F;
            y0 = fromY + this.perp.y * 0.15F;
            x1 = toX + this.perp.x * 0.15F;
            y1 = toY + this.perp.y * 0.15F;
            this.perp.set(-perpX, -perpY);
            this.perp.normalize();
            x2 = fromX + this.perp.x * 0.15F;
            y2 = fromY + this.perp.y * 0.15F;
            x3 = toX + this.perp.x * 0.15F;
            y3 = toY + this.perp.y * 0.15F;
            float minX = Math.min(x0, Math.min(x1, Math.min(x2, x3)));
            float minY = Math.min(y0, Math.min(y1, Math.min(y2, y3)));
            float maxX = Math.max(x0, Math.max(x1, Math.max(x2, x3)));
            float maxY = Math.max(y0, Math.max(y1, Math.max(y2, y3)));
            this.sweepAabb
                .init(
                    PZMath.fastfloor(minX),
                    PZMath.fastfloor(minY),
                    (int)Math.ceil(maxX) - PZMath.fastfloor(minX),
                    (int)Math.ceil(maxY) - PZMath.fastfloor(minY),
                    z
                );
            this.polyVec[0].set(x0, y0);
            this.polyVec[1].set(x1, y1);
            this.polyVec[2].set(x3, y3);
            this.polyVec[3].set(x2, y2);
            int chunkMinX = this.sweepAabb.left() / 8 - 1;
            int chunkMinY = this.sweepAabb.top() / 8 - 1;
            int chunkMaxX = (int)Math.ceil(this.sweepAabb.right() / 8.0F) + 1;
            int chunkMaxY = (int)Math.ceil(this.sweepAabb.bottom() / 8.0F) + 1;

            for (int cy = chunkMinY; cy < chunkMaxY; cy++) {
                for (int cx = chunkMinX; cx < chunkMaxX; cx++) {
                    IsoChunk chunk = GameServer.server
                        ? ServerMap.instance.getChunk(cx, cy)
                        : IsoWorld.instance.currentCell.getChunkForGridSquare(cx * 8, cy * 8, 0);
                    if (chunk != null) {
                        for (int i = 0; i < chunk.vehicles.size(); i++) {
                            BaseVehicle vehicle = chunk.vehicles.get(i);
                            if (vehicle != ignoreVehicle && vehicle.vehicleId != -1) {
                                this.vehiclePoly.init(vehicle.getPoly());
                                this.vehiclePoly.getAABB(this.vehicleAabb);
                                if (this.vehicleAabb.intersects(this.sweepAabb) && this.polyVehicleIntersect(this.vehiclePoly, render)) {
                                    collided = true;
                                    if (!render) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return collided;
        }
    }

    boolean testCollisionHorizontal(
        float fromX, float fromY, float toX, float toY, float ptX, float ptY, boolean bSkipN, boolean bSkipS, float z, boolean render
    ) {
        float w = 0.3F;
        float n = 0.3F;
        float e = 0.3F;
        float s = 0.3F;
        if (fromX < ptX && toX < ptX) {
            w = 0.0F;
        } else if (fromX >= ptX + 1.0F && toX >= ptX + 1.0F) {
            e = 0.0F;
        }

        if (fromY < ptY && toY < ptY) {
            n = 0.0F;
        } else if (fromY >= ptY && toY >= ptY) {
            s = 0.0F;
        }

        if (bSkipN) {
            n = 0.0F;
        }

        if (bSkipS) {
            s = 0.0F;
        }

        if (this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, ptX - w, ptY - n, ptX + 1.0F + e, ptY + s)) {
            if (render) {
                LineDrawer.addLine(ptX - w, ptY - n, z, ptX + 1.0F + e, ptY + s, z, 1.0F, 0.0F, 0.0F, null, false);
            }

            return true;
        } else {
            return false;
        }
    }

    boolean testCollisionVertical(float fromX, float fromY, float toX, float toY, float ptX, float ptY, boolean bSkipW, boolean bSkipE, float z, boolean render) {
        float w = 0.3F;
        float n = 0.3F;
        float e = 0.3F;
        float s = 0.3F;
        if (fromX < ptX && toX < ptX) {
            w = 0.0F;
        } else if (fromX >= ptX && toX >= ptX) {
            e = 0.0F;
        }

        if (fromY < ptY && toY < ptY) {
            n = 0.0F;
        } else if (fromY >= ptY + 1.0F && toY >= ptY + 1.0F) {
            s = 0.0F;
        }

        if (bSkipW) {
            w = 0.0F;
        }

        if (bSkipE) {
            e = 0.0F;
        }

        if (this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, ptX - w, ptY - n, ptX + e, ptY + 1.0F + s)) {
            if (render) {
                LineDrawer.addLine(ptX - w, ptY - n, z, ptX + e, ptY + 1.0F + s, z, 1.0F, 0.0F, 0.0F, null, false);
            }

            return true;
        } else {
            return false;
        }
    }

    boolean isNotClearClipper(PolygonalMap2 map, float fromX, float fromY, float toX, float toY, int z, BaseVehicle ignoreVehicle, int flags) {
        boolean ignoreDoors = (flags & 1) != 0;
        boolean closeToWalls = (flags & 2) != 0;
        boolean checkCost = (flags & 4) != 0;
        boolean render = (flags & 8) != 0;
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(PZMath.fastfloor(fromX), PZMath.fastfloor(fromY), z);
        if (square != null && square.HasStairs()) {
            return !square.isSameStaircase(PZMath.fastfloor(toX), PZMath.fastfloor(toY), z);
        } else if (!this.canStandAtClipper(map, toX, toY, z, ignoreVehicle, flags)) {
            if (render) {
                this.drawCircle(toX, toY, z, 0.3F, 1.0F, 0.0F, 0.0F, 1.0F);
            }

            return true;
        } else {
            return PolygonalMap2.instance.collideWithObstaclesPoly.isNotClear(fromX, fromY, toX, toY, z, render, ignoreVehicle, ignoreDoors, closeToWalls);
        }
    }

    boolean isNotClear(PolygonalMap2 map, float fromX, float fromY, float toX, float toY, int z, BaseVehicle ignoreVehicle, int flags) {
        return this.isNotClearOld(map, fromX, fromY, toX, toY, z, ignoreVehicle, flags);
    }

    Vector2 getCollidepoint(PolygonalMap2 map, float fromX, float fromY, float toX, float toY, int z, BaseVehicle ignoreVehicle, int flags) {
        boolean ignoreDoors = (flags & 1) != 0;
        boolean closeToWalls = (flags & 2) != 0;
        boolean checkCost = (flags & 4) != 0;
        boolean render = (flags & 8) != 0;
        float perpX = toY - fromY;
        float perpY = -(toX - fromX);
        this.perp.set(perpX, perpY);
        this.perp.normalize();
        float x0 = fromX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y0 = fromY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
        float x1 = toX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y1 = toY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
        this.perp.set(-perpX, -perpY);
        this.perp.normalize();
        float x2 = fromX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y2 = fromY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;
        float x3 = toX + this.perp.x * PolygonalMap2.RADIUS_DIAGONAL;
        float y3 = toY + this.perp.y * PolygonalMap2.RADIUS_DIAGONAL;

        for (int i = 0; i < this.pts.size(); i++) {
            this.pointPool.release(this.pts.get(i));
        }

        this.pts.clear();
        this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(fromX), PZMath.fastfloor(fromY)));
        if (PZMath.fastfloor(fromX) != PZMath.fastfloor(toX) || PZMath.fastfloor(fromY) != PZMath.fastfloor(toY)) {
            this.pts.add(this.pointPool.alloc().init(PZMath.fastfloor(toX), PZMath.fastfloor(toY)));
        }

        map.supercover(x0, y0, x1, y1, z, this.pointPool, this.pts);
        map.supercover(x2, y2, x3, y3, z, this.pointPool, this.pts);
        this.pts
            .sort(
                (pt1, pt2) -> PZMath.fastfloor(IsoUtils.DistanceManhatten(fromX, fromY, pt1.x, pt1.y) - IsoUtils.DistanceManhatten(fromX, fromY, pt2.x, pt2.y))
            );
        if (render) {
            for (int i = 0; i < this.pts.size(); i++) {
                Point pt = this.pts.get(i);
                LineDrawer.addLine(pt.x, pt.y, z, pt.x + 1.0F, pt.y + 1.0F, z, 1.0F, 1.0F, 0.0F, null, false);
            }
        }

        for (int i = 0; i < this.pts.size(); i++) {
            Point pt = this.pts.get(i);
            IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(pt.x, pt.y, z);
            if (checkCost && square != null && SquareUpdateTask.getCost(square) > 0) {
                return PolygonalMap2.temp.set(pt.x + 0.5F, pt.y + 0.5F);
            }

            if (square != null
                && !square.isSolid()
                && (!square.isSolidTrans() || square.isAdjacentToWindow() || square.isAdjacentToHoppable())
                && !square.HasStairs()
                && (square.solidFloorCached ? square.solidFloor : square.TreatAsSolidFloor())) {
                if (square.has(IsoFlagType.collideW) || !ignoreDoors && square.hasBlockedDoor(false)) {
                    float w = 0.3F;
                    float n = 0.3F;
                    float e = 0.3F;
                    float s = 0.3F;
                    if (fromX < pt.x && toX < pt.x) {
                        w = 0.0F;
                    } else if (fromX >= pt.x && toX >= pt.x) {
                        e = 0.0F;
                    }

                    if (fromY < pt.y && toY < pt.y) {
                        n = 0.0F;
                    } else if (fromY >= pt.y + 1 && toY >= pt.y + 1) {
                        s = 0.0F;
                    }

                    if (this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, pt.x - w, pt.y - n, pt.x + e, pt.y + 1.0F + s)) {
                        if (render) {
                            LineDrawer.addLine(pt.x - w, pt.y - n, z, pt.x + e, pt.y + 1.0F + s, z, 1.0F, 0.0F, 0.0F, null, false);
                        }

                        return PolygonalMap2.temp.set(pt.x + (fromX - toX < 0.0F ? -0.5F : 0.5F), pt.y + 0.5F);
                    }
                }

                if (square.has(IsoFlagType.collideN) || !ignoreDoors && square.hasBlockedDoor(true)) {
                    float wx = 0.3F;
                    float nx = 0.3F;
                    float ex = 0.3F;
                    float sx = 0.3F;
                    if (fromX < pt.x && toX < pt.x) {
                        wx = 0.0F;
                    } else if (fromX >= pt.x + 1 && toX >= pt.x + 1) {
                        ex = 0.0F;
                    }

                    if (fromY < pt.y && toY < pt.y) {
                        nx = 0.0F;
                    } else if (fromY >= pt.y && toY >= pt.y) {
                        sx = 0.0F;
                    }

                    if (this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, pt.x - wx, pt.y - nx, pt.x + 1.0F + ex, pt.y + sx)) {
                        if (render) {
                            LineDrawer.addLine(pt.x - wx, pt.y - nx, z, pt.x + 1.0F + ex, pt.y + sx, z, 1.0F, 0.0F, 0.0F, null, false);
                        }

                        return PolygonalMap2.temp.set(pt.x + 0.5F, pt.y + (fromY - toY < 0.0F ? -0.5F : 0.5F));
                    }
                }
            } else {
                float wxx = 0.3F;
                float nxx = 0.3F;
                float exx = 0.3F;
                float sxx = 0.3F;
                if (fromX < pt.x && toX < pt.x) {
                    wxx = 0.0F;
                } else if (fromX >= pt.x + 1 && toX >= pt.x + 1) {
                    exx = 0.0F;
                }

                if (fromY < pt.y && toY < pt.y) {
                    nxx = 0.0F;
                } else if (fromY >= pt.y + 1 && toY >= pt.y + 1) {
                    sxx = 0.0F;
                }

                if (this.lb.lineRectIntersect(fromX, fromY, toX - fromX, toY - fromY, pt.x - wxx, pt.y - nxx, pt.x + 1.0F + exx, pt.y + 1.0F + sxx)) {
                    if (render) {
                        LineDrawer.addLine(pt.x - wxx, pt.y - nxx, z, pt.x + 1.0F + exx, pt.y + 1.0F + sxx, z, 1.0F, 0.0F, 0.0F, null, false);
                    }

                    return PolygonalMap2.temp.set(pt.x + 0.5F, pt.y + 0.5F);
                }
            }
        }

        return PolygonalMap2.temp.set(toX, toY);
    }

    boolean polyVehicleIntersect(VehiclePoly poly, boolean render) {
        this.vehicleVec[0].set(poly.x1, poly.y1);
        this.vehicleVec[1].set(poly.x2, poly.y2);
        this.vehicleVec[2].set(poly.x3, poly.y3);
        this.vehicleVec[3].set(poly.x4, poly.y4);
        boolean intersect = false;

        for (int i = 0; i < 4; i++) {
            Vector2 a = this.polyVec[i];
            Vector2 b = i == 3 ? this.polyVec[0] : this.polyVec[i + 1];

            for (int j = 0; j < 4; j++) {
                Vector2 c = this.vehicleVec[j];
                Vector2 d = j == 3 ? this.vehicleVec[0] : this.vehicleVec[j + 1];
                if (Line2D.linesIntersect(a.x, a.y, b.x, b.y, c.x, c.y, d.x, d.y)) {
                    if (render) {
                        LineDrawer.addLine(a.x, a.y, 0.0F, b.x, b.y, 0.0F, 1.0F, 0.0F, 0.0F, null, true);
                        LineDrawer.addLine(c.x, c.y, 0.0F, d.x, d.y, 0.0F, 1.0F, 0.0F, 0.0F, null, true);
                    }

                    intersect = true;
                }
            }
        }

        return intersect;
    }
}
