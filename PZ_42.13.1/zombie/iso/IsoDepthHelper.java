// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.core.math.PZMath;
import zombie.iso.fboRenderChunk.FBORenderLevels;

public class IsoDepthHelper {
    private static final ThreadLocal<IsoDepthHelper.Results> results = ThreadLocal.withInitial(IsoDepthHelper.Results::new);
    public static final float CHUNK_DEPTH = 0.023093667F;
    public static final float SQUARE_DEPTH = 0.0028867084F;
    public static final float LEVEL_DEPTH = 0.0028867084F;
    public static final int CHUNK_WIDTH_OF_DEPTH_BUFFER = 20;

    public static IsoDepthHelper.Results getChunkDepthData(int centreWX, int centreWY, int wx, int wy, int level) {
        IsoDepthHelper.Results r = results.get();
        int xscrMin = centreWX + 10;
        int yscrMin = centreWY + 10;
        int xscrMax = centreWX - 10;
        int yscrMax = centreWY - 10;
        int difX = Math.abs(xscrMax - xscrMin);
        int difY = Math.abs(yscrMax - yscrMin);
        int indexX = wx - xscrMax;
        int indexY = wy - yscrMax;
        int indexX2 = wx - 1 - xscrMax;
        int indexY2 = wy - 1 - yscrMax;
        indexX = difX - indexX;
        indexY = difY - indexY;
        indexX2 = difX - indexX2;
        indexY2 = difY - indexY2;
        indexX *= 8;
        indexY *= 8;
        indexX2 *= 8;
        indexY2 *= 8;
        difX *= 8;
        difY *= 8;
        float max = difX + difY;
        r.sizeX = difX;
        r.sizeY = difY;
        r.indexX2 = indexX2;
        r.indexY2 = indexY2;
        r.indexX = indexX;
        r.indexY = indexY;
        r.maxDepth = max;
        r.depthStart = (indexX + indexY) / max;
        r.depthEnd = (indexX2 + indexY2) / max;
        int CPW = 8;
        r.depthStart = (indexX + indexY) / 8.0F / 40.0F;
        r.depthStart *= 0.46187335F;
        r.depthStart = r.depthStart - FBORenderLevels.calculateMinLevel(PZMath.fastfloor((float)level)) * 0.0028867084F;
        return r;
    }

    public static IsoDepthHelper.Results getDepthSize() {
        IsoDepthHelper.Results r = results.get();
        int xscrMin = 10;
        int yscrMin = 10;
        int xscrMax = -10;
        int yscrMax = -10;
        xscrMin *= 8;
        yscrMin *= 8;
        xscrMax *= 8;
        yscrMax *= 8;
        int difX = Math.abs(xscrMax - xscrMin);
        int difY = Math.abs(yscrMax - yscrMin);
        r.sizeX = difX;
        r.sizeY = difY;
        return r;
    }

    public static float calculateDepth(float x, float y, float z) {
        float depth = (7.0F - z) * 2.5F;
        float xMod = PZMath.coordmodulof(x, 8);
        float yMod = PZMath.coordmodulof(y, 8);
        xMod = 8.0F - xMod;
        yMod = 8.0F - yMod;
        int xscrMin = 0;
        int yscrMin = 0;
        int xscrMax = -20;
        int yscrMax = -20;
        xscrMin *= 8;
        yscrMin *= 8;
        xscrMax *= 8;
        yscrMax *= 8;
        int difX = Math.abs(xscrMax - xscrMin);
        int difY = Math.abs(yscrMax - yscrMin);
        float max = difX + difY;
        float d = (xMod + yMod + depth) / max;
        int minLevel = FBORenderLevels.calculateMinLevel(PZMath.fastfloor(z));
        float zOffset = (2.0F - (z - minLevel)) * 0.0028867084F;
        int CPW = 8;
        return (xMod + yMod) / 16.0F * 0.023093667F + zOffset;
    }

    public static IsoDepthHelper.Results getSquareDepthData(int centreX, int centreY, float x, float y, float z) {
        centreX = PZMath.fastfloor(centreX / 8.0F);
        centreY = PZMath.fastfloor(centreY / 8.0F);
        IsoDepthHelper.Results r = getChunkDepthData(centreX, centreY, PZMath.fastfloor(x / 8.0F), PZMath.fastfloor(y / 8.0F), PZMath.fastfloor(z));
        r.depthStart = r.depthStart + calculateDepth(x, y, z);
        return r;
    }

    public static class Results {
        public int indexX;
        public int indexY;
        public int indexX2;
        public int indexY2;
        public int sizeX;
        public int sizeY;
        public float depthStart;
        public float depthEnd;
        public float maxDepth;
    }
}
