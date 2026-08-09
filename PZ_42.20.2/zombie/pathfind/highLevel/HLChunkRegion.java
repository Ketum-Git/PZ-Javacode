// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.highLevel;

import java.nio.ByteBuffer;
import zombie.core.utils.BooleanGrid;
import zombie.debug.LineDrawer;
import zombie.pathfind.Chunk;
import zombie.pathfind.PolygonalMap2;
import zombie.vehicles.Clipper;

public final class HLChunkRegion {
    static final int CPW = 8;
    HLChunkLevel levelData;
    final BooleanGrid squaresMask = new BooleanGrid(8, 8);
    int minX;
    int minY;
    int maxX;
    int maxY;
    final int[] edgeN = new int[8];
    final int[] edgeS = new int[8];
    final int[] edgeW = new int[8];
    final int[] edgeE = new int[8];

    Chunk getChunk() {
        return this.levelData.getChunk();
    }

    int getLevel() {
        return this.levelData.getLevel();
    }

    boolean containsSquare(int worldSquareX, int worldSquareY) {
        return this.squaresMask.getValue(worldSquareX - this.getChunk().getMinX(), worldSquareY - this.getChunk().getMinY());
    }

    boolean containsSquareLocal(int chunkSquareX, int chunkSquareY) {
        return this.squaresMask.getValue(chunkSquareX, chunkSquareY);
    }

    void initEdges() {
        for (int y = 0; y < 8; y++) {
            this.edgeN[y] = 0;
            this.edgeS[y] = 0;

            for (int x = 0; x < 8; x++) {
                if (this.containsSquareLocal(x, y)) {
                    if (!this.containsSquareLocal(x, y - 1)) {
                        this.edgeN[y] = this.edgeN[y] | 1 << x;
                    }

                    if (!this.containsSquareLocal(x, y + 1)) {
                        this.edgeS[y] = this.edgeS[y] | 1 << x;
                    }
                }
            }
        }

        for (int xx = 0; xx < 8; xx++) {
            this.edgeW[xx] = 0;
            this.edgeE[xx] = 0;

            for (int y = 0; y < 8; y++) {
                if (this.containsSquareLocal(xx, y)) {
                    if (!this.containsSquareLocal(xx - 1, y)) {
                        this.edgeW[xx] = this.edgeW[xx] | 1 << y;
                    }

                    if (!this.containsSquareLocal(xx + 1, y)) {
                        this.edgeE[xx] = this.edgeE[xx] | 1 << y;
                    }
                }
            }
        }
    }

    boolean isOnEdgeOfLoadedArea() {
        boolean bOnEdge = false;
        if (this.edgeN[0] != 0) {
            bOnEdge |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx, this.getChunk().wy - 1) == null;
        }

        if (this.edgeS[7] != 0) {
            bOnEdge |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx, this.getChunk().wy + 1) == null;
        }

        if (this.edgeW[0] != 0) {
            bOnEdge |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx - 1, this.getChunk().wy) == null;
        }

        if (this.edgeE[7] != 0) {
            bOnEdge |= PolygonalMap2.instance.getChunkFromChunkPos(this.getChunk().wx + 1, this.getChunk().wy) == null;
        }

        return bOnEdge;
    }

    void renderDebug() {
        Clipper clipper = HLGlobals.clipper;
        clipper.clear();

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if (this.squaresMask.getValue(x, y)) {
                    clipper.addAABB(x, y, x + 1, y + 1);
                }
            }
        }

        ByteBuffer polyBuffer = HLGlobals.clipperBuffer;
        int polyCount = clipper.generatePolygons(-0.1F, 3);

        for (int i = 0; i < polyCount; i++) {
            polyBuffer.clear();
            clipper.getPolygon(i, polyBuffer);
            this.renderPolygon(polyBuffer, true);
            short holeCount = polyBuffer.getShort();

            for (int j = 0; j < holeCount; j++) {
                this.renderPolygon(polyBuffer, false);
            }
        }
    }

    private void renderPolygon(ByteBuffer polyBuffer, boolean outer) {
        int pointCount = polyBuffer.getShort();
        if (pointCount < 3) {
            polyBuffer.position(polyBuffer.position() + pointCount * 4 * 2);
        } else {
            int left = this.getChunk().wx * 8;
            int top = this.getChunk().wy * 8;
            float xFirst = 0.0F;
            float yFirst = 0.0F;
            float xPrev = 0.0F;
            float yPrev = 0.0F;
            float r = 1.0F;
            float g = 1.0F;
            float b = 1.0F;
            float a = 1.0F;
            if (!outer) {
                r *= 0.5F;
                g *= 0.5F;
                b *= 0.5F;
            }

            int level = this.getLevel() - 32;

            for (int j = 0; j < pointCount; j++) {
                float x = polyBuffer.getFloat();
                float y = polyBuffer.getFloat();
                if (j == 0) {
                    xFirst = x;
                    yFirst = y;
                } else {
                    LineDrawer.addLine(left + xPrev, top + yPrev, level, left + x, top + y, level, r, g, b, 1.0F);
                    if (j == pointCount - 1) {
                        LineDrawer.addLine(left + x, top + y, level, left + xFirst, top + yFirst, level, r, g, b, 1.0F);
                    }
                }

                xPrev = x;
                yPrev = y;
            }
        }
    }
}
