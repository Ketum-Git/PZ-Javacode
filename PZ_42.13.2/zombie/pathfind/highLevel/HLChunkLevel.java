// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.highLevel;

import java.util.ArrayList;
import zombie.core.profiling.AbstractPerformanceProfileProbe;
import zombie.debug.LineDrawer;
import zombie.iso.IsoDirections;
import zombie.pathfind.Chunk;
import zombie.pathfind.ChunkLevel;
import zombie.pathfind.PolygonalMap2;
import zombie.pathfind.Square;

public final class HLChunkLevel {
    static final int CPW = 8;
    public int modificationCount = -1;
    int modificationCountStairs = -1;
    final ChunkLevel chunkLevel;
    final ArrayList<HLChunkRegion> regionList = new ArrayList<>();
    final ArrayList<HLStaircase> stairs = new ArrayList<>();
    final ArrayList<HLSlopedSurface> slopedSurfaces = new ArrayList<>();

    public HLChunkLevel(ChunkLevel chunkLevel) {
        this.chunkLevel = chunkLevel;
    }

    Chunk getChunk() {
        return this.chunkLevel.getChunk();
    }

    int getLevel() {
        return this.chunkLevel.getLevel();
    }

    public void initRegions() {
        this.releaseRegions();
        Square[][] squares = this.getChunk().getSquaresForLevel(this.getLevel());

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Square square = squares[x][y];
                if (this.canWalkOnSquare(square)
                    && !this.isSquareInRegion(square.getX(), square.getY())
                    && (
                        PolygonalMap2.instance.getVisGraphAt(square.getX() + 0.5F, square.getY() + 0.5F, square.getZ(), 1) == null
                            || square.has(504)
                            || square.hasSlopedSurface()
                    )) {
                    HLChunkRegion region = HLGlobals.chunkRegionPool.alloc();
                    region.levelData = this;
                    region.squaresMask.clear();
                    this.floodFill(region, squares, square);
                    region.initEdges();
                    this.regionList.add(region);
                }
            }
        }

        if (!this.regionList.isEmpty()) {
        }
    }

    void releaseRegions() {
        HLGlobals.chunkRegionPool.releaseAll(this.regionList);
        this.regionList.clear();
    }

    void initStairsIfNeeded() {
        if (HLAStar.modificationCount != this.modificationCountStairs) {
            this.modificationCountStairs = HLAStar.modificationCount;
            this.initStairs();
        }
    }

    void initStairs() {
        try (AbstractPerformanceProfileProbe ignored = HLAStar.PerfInitStairs.profile()) {
            this.initStairsInternal();
        }
    }

    void initStairsInternal() {
        this.releaseStairs();
        Square[][] squares = this.getChunk().getSquaresForLevel(this.getLevel());

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Square square = squares[x][y];
                if (square != null) {
                    if (square.has(64)) {
                        HLStaircase stair = HLGlobals.staircasePool.alloc();
                        stair.set(IsoDirections.N, square.getX(), square.getY(), square.getZ());
                        this.stairs.add(stair);
                    }

                    if (square.has(8)) {
                        HLStaircase stair = HLGlobals.staircasePool.alloc();
                        stair.set(IsoDirections.W, square.getX(), square.getY(), square.getZ());
                        this.stairs.add(stair);
                    }
                }
            }
        }

        this.initSlopedSurfaces();
    }

    void releaseStairs() {
        HLGlobals.staircasePool.releaseAll(this.stairs);
        this.stairs.clear();
    }

    HLStaircase getStaircaseAt(int x, int y) {
        for (int i = 0; i < this.stairs.size(); i++) {
            HLStaircase staircase = this.stairs.get(i);
            if (staircase.isBottomFloorAt(x, y)) {
                return staircase;
            }
        }

        return null;
    }

    void initSlopedSurfaces() {
        this.releaseSlopedSurfaces();
        Square[][] squares = this.getChunk().getSquaresForLevel(this.getLevel());

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                Square square = squares[x][y];
                if (square != null) {
                    IsoDirections slopeDir = square.getSlopedSurfaceDirection();
                    if (slopeDir != null && !(square.getSlopedSurfaceHeightMax() < 1.0F)) {
                        HLSlopedSurface slopedSurface = HLGlobals.slopedSurfacePool.alloc();
                        slopedSurface.set(slopeDir, square.getX(), square.getY(), square.getZ());
                        this.slopedSurfaces.add(slopedSurface);
                    }
                }
            }
        }
    }

    void releaseSlopedSurfaces() {
        HLGlobals.slopedSurfacePool.releaseAll(this.slopedSurfaces);
        this.slopedSurfaces.clear();
    }

    HLSlopedSurface getSlopedSurfaceAt(int x, int y) {
        for (int i = 0; i < this.slopedSurfaces.size(); i++) {
            HLSlopedSurface slopedSurface = this.slopedSurfaces.get(i);
            if (slopedSurface.isBottomFloorAt(x, y)) {
                return slopedSurface;
            }
        }

        return null;
    }

    HLLevelTransition getLevelTransitionAt(int x, int y) {
        HLLevelTransition levelTransition = this.getStaircaseAt(x, y);
        if (levelTransition == null) {
            levelTransition = this.getSlopedSurfaceAt(x, y);
        }

        return levelTransition;
    }

    public void removeFromWorld() {
        this.releaseRegions();
        this.releaseStairs();
        this.releaseSlopedSurfaces();
        this.modificationCount = -1;
        this.modificationCountStairs = -1;
    }

    HLChunkRegion findRegionContainingSquare(int worldSquareX, int worldSquareY) {
        for (int i = 0; i < this.regionList.size(); i++) {
            HLChunkRegion region = this.regionList.get(i);
            if (region.containsSquare(worldSquareX, worldSquareY)) {
                return region;
            }
        }

        return null;
    }

    boolean isSquareInRegion(int worldSquareX, int worldSquareY) {
        return this.findRegionContainingSquare(worldSquareX, worldSquareY) != null;
    }

    boolean canWalkOnSquare(Square square) {
        if (square == null) {
            return false;
        } else {
            return !square.TreatAsSolidFloor() ? false : !square.isReallySolid();
        }
    }

    boolean isCanPathTransition(Square square1, Square square2) {
        if (square1 != null && square2 != null && square1 != square2 && square1.getZ() == square2.getZ()) {
            int dx = square2.getX() - square1.getX();
            int dy = square2.getY() - square1.getY();
            if (dx < 0) {
                if (square1.has(8192)) {
                    return true;
                }
            } else if (dx > 0 && square2.has(8192)) {
                return true;
            }

            if (dy < 0) {
                if (square1.has(16384)) {
                    return true;
                }
            } else if (dy > 0 && square2.has(16384)) {
                return true;
            }

            return false;
        } else {
            return false;
        }
    }

    void floodFill(HLChunkRegion region, Square[][] squares, Square square) {
        HLGlobals.floodFill.reset();
        HLGlobals.floodFill.calculate(region, squares, square);
    }

    void renderDebug() {
        for (int i = 0; i < this.regionList.size(); i++) {
            HLChunkRegion region = this.regionList.get(i);
            region.renderDebug();
        }

        this.initStairsIfNeeded();

        for (int i = 0; i < this.stairs.size(); i++) {
            HLStaircase stair = this.stairs.get(i);
            LineDrawer.addLine(
                stair.getBottomFloorX() + 0.5F,
                stair.getBottomFloorY() + 0.5F,
                stair.getBottomFloorZ() - 32,
                stair.getTopFloorX() + 0.5F,
                stair.getTopFloorY() + 0.5F,
                stair.getTopFloorZ() - 32,
                1.0F,
                1.0F,
                1.0F,
                1.0F
            );
        }
    }
}
