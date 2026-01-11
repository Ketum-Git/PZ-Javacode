// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.vehicles.Clipper;

public final class FBORenderOcclusion {
    private static FBORenderOcclusion instance;
    boolean enabled = true;
    int occludedGridX1;
    int occludedGridY1;
    int occludedGridX2;
    int occludedGridY2;
    int[] occludedGrid;
    final int[][] occludingChunkLevels = new int[64][];
    final TFloatArrayList floatArrayList1 = new TFloatArrayList();
    final TFloatArrayList floatArrayList2 = new TFloatArrayList();
    Clipper clipper;
    ByteBuffer clipperBuffer;
    int testValue;

    public static FBORenderOcclusion getInstance() {
        if (instance == null) {
            instance = new FBORenderOcclusion();
        }

        return instance;
    }

    private FBORenderOcclusion() {
    }

    public void init() {
        if (this.occludingChunkLevels[0] == null) {
            if (this.clipper == null) {
                this.clipper = new Clipper();
            }

            int wx1 = 0;
            int wy1 = 0;
            int level1 = 31;
            this.createChunkPolygon(0, 0, 31, this.floatArrayList1);
            int CPW = 8;
            float minPtX = IsoUtils.XToScreen(-3.0F, -3.0F, 31.0F, 0);
            float minPtY = IsoUtils.YToScreen(-3.0F, -3.0F, 31.0F, 0);
            float maxPtX = IsoUtils.XToScreen(11.0F, 11.0F, 31.0F, 0);
            float maxPtY = IsoUtils.YToScreen(11.0F, 11.0F, 31.0F, 0);
            TIntArrayList ints = new TIntArrayList();

            for (int level2 = 30; level2 >= -32; level2--) {
                ints.clear();
                int minX = PZMath.fastfloor(this.XToIso(minPtX, minPtY, level2) / 8.0F);
                int minY = PZMath.fastfloor(this.YToIso(minPtX, minPtY, level2) / 8.0F);
                int maxX = PZMath.fastfloor(this.XToIso(maxPtX, maxPtY, level2) / 8.0F);
                int maxY = PZMath.fastfloor(this.YToIso(maxPtX, maxPtY, level2) / 8.0F);

                for (int wy2 = minY; wy2 <= maxY; wy2++) {
                    for (int wx2 = minX; wx2 <= maxX; wx2++) {
                        this.createChunkPolygon(wx2, wy2, level2, this.floatArrayList2);
                        if (this.polygonsOverlap(this.floatArrayList1, this.floatArrayList2)) {
                            ints.add(wx2);
                            ints.add(wy2);
                        }
                    }
                }

                this.occludingChunkLevels[level2 + 32] = ints.toArray();
            }
        }
    }

    float XToIso(float screenX, float screenY, float level) {
        float tx = (screenX + 2.0F * screenY) / (64.0F * Core.tileScale);
        return tx + 3.0F * level;
    }

    float YToIso(float screenX, float screenY, float level) {
        float ty = (screenX - 2.0F * screenY) / (-64.0F * Core.tileScale);
        return ty + 3.0F * level;
    }

    public void invalidateOverlappedChunkLevels(int playerIndex, IsoChunk chunk, int level) {
        IsoChunkMap chunkMap = IsoWorld.instance.getCell().getChunkMap(playerIndex);
        int n = 62;

        for (int level2 = level - 1; level2 >= chunkMap.minHeight; level2--) {
            int[] occludingChunkLevels1 = this.occludingChunkLevels[n];
            n--;

            for (int i = 0; i < occludingChunkLevels1.length; i += 2) {
                int wx = occludingChunkLevels1[i];
                int wy = occludingChunkLevels1[i + 1];
                IsoChunk chunk2 = chunkMap.getChunk(chunk.wx - chunkMap.getWorldXMin() + wx, chunk.wy - chunkMap.getWorldYMin() + wy);
                if (chunk2 != null && level2 >= chunk2.getMinLevel() && level2 <= chunk2.getMaxLevel()) {
                    FBORenderLevels renderLevels = chunk2.getRenderLevels(playerIndex);
                    renderLevels.invalidateLevel(level2, 1024L);
                }
            }
        }
    }

    boolean polygonsOverlap(TFloatArrayList polygon1, TFloatArrayList polygon2) {
        this.clipper.clear();
        this.addPolygon(polygon1, false);
        this.addPolygon(polygon2, true);
        int numPolygons = this.clipper.generatePolygons(1, 0.0, 0);
        if (numPolygons == 0) {
            return false;
        } else {
            for (int i = 0; i < numPolygons; i++) {
                this.clipperBuffer.clear();
                this.clipper.getPolygon(i, this.clipperBuffer);
                int numPoints = this.clipperBuffer.getShort();
                if (numPoints >= 3) {
                    return true;
                }
            }

            return false;
        }
    }

    void addPolygon(TFloatArrayList points, boolean bClip) {
        if (this.clipperBuffer == null || this.clipperBuffer.capacity() < points.size() * 8 * 4) {
            this.clipperBuffer = ByteBuffer.allocateDirect(points.size() * 8 * 4);
        }

        this.clipperBuffer.clear();
        if (this.isClockwise(points)) {
            for (int i = this.numPoints(points) - 1; i >= 0; i--) {
                this.clipperBuffer.putFloat(this.getX(points, i));
                this.clipperBuffer.putFloat(this.getY(points, i));
            }
        } else {
            for (int i = 0; i < this.numPoints(points); i++) {
                this.clipperBuffer.putFloat(this.getX(points, i));
                this.clipperBuffer.putFloat(this.getY(points, i));
            }
        }

        this.clipper.addPath(this.numPoints(points), this.clipperBuffer, bClip);
    }

    void createChunkPolygon(int wx, int wy, int level, TFloatArrayList out) {
        this.createPolygon(wx * 8, wy * 8, level, 8, out);
    }

    void createPolygon(int x, int y, int level, int edgeSquares, TFloatArrayList out) {
        out.clear();
        out.add(IsoUtils.XToScreen(x, y + edgeSquares, level, 0));
        out.add(IsoUtils.YToScreen(x, y + edgeSquares, level, 0));
        out.add(IsoUtils.XToScreen(x, y + edgeSquares, level + 1, 0));
        out.add(IsoUtils.YToScreen(x, y + edgeSquares, level + 1, 0));
        out.add(IsoUtils.XToScreen(x, y, level + 1, 0));
        out.add(IsoUtils.YToScreen(x, y, level + 1, 0));
        out.add(IsoUtils.XToScreen(x + edgeSquares, y, level + 1, 0));
        out.add(IsoUtils.YToScreen(x + edgeSquares, y, level + 1, 0));
        out.add(IsoUtils.XToScreen(x + edgeSquares, y, level, 0));
        out.add(IsoUtils.YToScreen(x + edgeSquares, y, level, 0));
        out.add(IsoUtils.XToScreen(x + edgeSquares, y + edgeSquares, level, 0));
        out.add(IsoUtils.YToScreen(x + edgeSquares, y + edgeSquares, level, 0));
    }

    boolean isClockwise(TFloatArrayList points) {
        float sum = 0.0F;

        for (int i = 0; i < this.numPoints(points); i++) {
            float p1x = this.getX(points, i);
            float p1y = this.getY(points, i);
            float p2x = this.getX(points, (i + 1) % this.numPoints(points));
            float p2y = this.getY(points, (i + 1) % this.numPoints(points));
            sum += (p2x - p1x) * (p2y + p1y);
        }

        return sum > 0.0;
    }

    int numPoints(TFloatArrayList points) {
        return points.size() / 2;
    }

    float getX(TFloatArrayList points, int index) {
        return points.get(index * 2);
    }

    float getY(TFloatArrayList points, int index) {
        return points.get(index * 2 + 1);
    }

    public void setFloorOccluded(int x, int y, int z) {
    }

    public void setNorthWallOccluded(int x, int y, int z) {
    }

    public void setWestWallOccluded(int x, int y, int z) {
    }

    public boolean isOccluded(int x, int y, int z) {
        if (!this.enabled) {
            return false;
        } else {
            this.testValue = z;
            if (!this.isOccludedAux(x, y, z)) {
                return false;
            } else if (!this.isOccludedAux(x - 1, y - 1, z)) {
                return false;
            } else if (!this.isOccludedAux(x - 2, y - 2, z)) {
                return false;
            } else if (!this.isOccludedAux(x - 3, y - 3, z)) {
                return false;
            } else if (!this.isOccludedAux(x - 1, y - 0, z)) {
                return false;
            } else if (!this.isOccludedAux(x - 0, y - 1, z)) {
                return false;
            } else if (!this.isOccludedAux(x - 2, y - 1, z)) {
                return false;
            } else if (!this.isOccludedAux(x - 1, y - 2, z)) {
                return false;
            } else {
                return !this.isOccludedAux(x - 3, y - 2, z) ? false : this.isOccludedAux(x - 2, y - 3, z);
            }
        }
    }

    boolean isOccludedAux(int x, int y, int z) {
        int zeroX = x - z * 3 - this.occludedGridX1;
        int zeroY = y - z * 3 - this.occludedGridY1;
        if (zeroX >= 0 && zeroY >= 0 && zeroX <= this.occludedGridX2 - this.occludedGridX1 && zeroY <= this.occludedGridY2 - this.occludedGridY1) {
            int zeroIndex = zeroX + zeroY * (this.occludedGridX2 - this.occludedGridX1 + 1);
            int value = this.occludedGrid[zeroIndex];
            return value > this.testValue;
        } else {
            return false;
        }
    }

    public void removeChunkFromWorld(IsoChunk chunk) {
        if (this.enabled) {
            for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
                for (int level = chunk.minLevel; level <= chunk.maxLevel; level++) {
                    this.invalidateOverlappedChunkLevels(playerIndex, chunk, level);
                }
            }
        }
    }
}
