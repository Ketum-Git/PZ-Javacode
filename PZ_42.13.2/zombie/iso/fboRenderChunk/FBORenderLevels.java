// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.utils.ImageUtils;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;

public final class FBORenderLevels {
    public static boolean clearCachedSquares = true;
    private final int playerIndex;
    private final IsoChunk chunk;
    private final FBORenderLevels.NLevels[] fbos = new FBORenderLevels.NLevels[32];
    public final ArrayList<IsoGridSquare> treeSquares = new ArrayList<>();
    final ArrayList<IsoGridSquare> waterSquares = new ArrayList<>();
    final ArrayList<IsoGridSquare> waterShoreSquares = new ArrayList<>();
    final ArrayList<IsoGridSquare> waterAttachSquares = new ArrayList<>();
    public final FBORenderSnow.ChunkLevel snowLevelZero;
    public final FBORenderSnow.ChunkLevel snowLevelNotZero;
    float renderX;
    float renderY;
    float renderW;
    float renderH;
    boolean inStencilRect;
    public int adjacentChunkLoadedCounter;
    public IsoChunk seamChunkE;
    public IsoChunk seamChunkSe;
    public IsoChunk seamChunkS;
    public int prevMinZ = Integer.MAX_VALUE;
    public int prevMaxZ = Integer.MIN_VALUE;

    public FBORenderLevels(int playerIndex, IsoChunk chunk) {
        this.playerIndex = playerIndex;
        this.chunk = chunk;
        int minZRoundedDown = calculateMinLevel(-32);

        for (int i = 0; i < this.fbos.length; i++) {
            this.fbos[i] = new FBORenderLevels.NLevels(calculateMinLevel(minZRoundedDown + i * 2));
        }

        this.snowLevelZero = new FBORenderSnow.ChunkLevel(chunk);
        this.snowLevelNotZero = new FBORenderSnow.ChunkLevel(chunk);
    }

    public int getPlayerIndex() {
        return this.playerIndex;
    }

    public IsoChunk getChunk() {
        return this.chunk;
    }

    private int indexForLevel(int level) {
        int minZRoundedDown = calculateMinLevel(-32);
        return (calculateMinLevel(level) - minZRoundedDown) / 2;
    }

    private FBORenderLevels.NLevels getNLevels(int level) {
        return this.fbos[this.indexForLevel(level)];
    }

    void calculateRenderBounds(int minLevel, int maxLevel) {
        int nLevels = maxLevel - minLevel + 1;
        float width = 64 * Core.tileScale * 8;
        float height = FBORenderChunk.FLOOR_HEIGHT * 8 + FBORenderChunk.PIXELS_PER_LEVEL * nLevels;
        float x = IsoUtils.XToScreen(this.chunk.wx * 8, this.chunk.wy * 8, minLevel, 0);
        float y = IsoUtils.YToScreen(this.chunk.wx * 8, this.chunk.wy * 8, minLevel, 0);
        y -= FBORenderChunk.PIXELS_PER_LEVEL * nLevels;
        int dh = extraHeightForJumboTrees(minLevel, maxLevel);
        height += dh;
        y -= dh;
        x -= IsoCamera.cameras[this.playerIndex].getOffX();
        y -= IsoCamera.cameras[this.playerIndex].getOffY();
        float zoom = Core.getInstance().getZoom(this.playerIndex);
        x /= zoom;
        y /= zoom;
        width /= zoom;
        height /= zoom;
        x -= width / 2.0F;
        this.renderX = x;
        this.renderY = y;
        this.renderW = width;
        this.renderH = height;
    }

    void calculateRenderBounds(int level) {
        int nLevels = 2;
        int minLevel = calculateMinLevel(level);
        int maxLevel = minLevel + 2 - 1;
        this.calculateRenderBounds(minLevel, maxLevel);
    }

    public boolean calculateOnScreen(int level) {
        this.calculateRenderBounds(level);
        int screenX = IsoCamera.getScreenLeft(this.playerIndex) * 0;
        int screenY = IsoCamera.getScreenTop(this.playerIndex) * 0;
        int screenX2 = screenX + IsoCamera.getScreenWidth(this.playerIndex);
        int screenY2 = screenY + IsoCamera.getScreenHeight(this.playerIndex);
        return this.renderX + this.renderW > screenX && this.renderX < screenX2 && this.renderY + this.renderH > screenY && this.renderY < screenY2;
    }

    public boolean calculateInStencilRect(int level) {
        this.calculateRenderBounds(level, level);
        float zoom = Core.getInstance().getZoom(this.playerIndex);
        int texW = 512 * Core.tileScale;
        int texH = 512 * Core.tileScale;
        float px = IsoCamera.getScreenWidth(this.playerIndex) / 2.0F * zoom - IsoCamera.cameras[this.playerIndex].rightClickX;
        float py = IsoCamera.getScreenHeight(this.playerIndex) / 2.0F * zoom - IsoCamera.cameras[this.playerIndex].rightClickY;
        float StencilX1 = (px - texW / 2.0F) / zoom;
        float StencilY1 = (py - texH / 2.0F) / zoom;
        float StencilX2 = StencilX1 + texW / zoom;
        float StencilY2 = StencilY1 + texH / zoom;
        return this.renderX + this.renderW > StencilX1 && this.renderX < StencilX2 && this.renderY + this.renderH > StencilY1 && this.renderY < StencilY2;
    }

    public void setOnScreen(int level, boolean bOnScreen) {
        this.getNLevels(level).onScreen = bOnScreen;
    }

    public boolean isOnScreen(int level) {
        return this.getNLevels(level).onScreen;
    }

    public FBORenderChunk getOrCreateFBOForLevel(int level, float cameraZoom) {
        FBORenderChunk renderChunk = this.getFBOForLevel(level, cameraZoom);
        if (renderChunk != null && !this.isTextureSizeValid(renderChunk, cameraZoom)) {
            this.freeFBO(renderChunk);
            renderChunk = null;
        }

        if (renderChunk == null) {
            renderChunk = this.createFBOForLevel(level, cameraZoom);
        }

        return renderChunk;
    }

    public FBORenderChunk getFBOForLevel(int level, float cameraZoom) {
        return this.getNLevels(level).getZoomInfo(cameraZoom).renderChunk;
    }

    public void clearCachedSquares(int level) {
        FBORenderLevels.NLevels nLevels = this.getNLevels(level);
        nLevels.animatedAttachments.clear();
        nLevels.corpseSquares.clear();
        nLevels.fliesSquares.clear();
        nLevels.itemSquares.clear();
        nLevels.puddleSquares.clear();
        nLevels.translucentFloor.clear();
        nLevels.translucentNonFloor.clear();
    }

    public ArrayList<IsoGridSquare> getCachedSquares_AnimatedAttachments(int level) {
        return this.getNLevels(level).animatedAttachments;
    }

    public ArrayList<IsoGridSquare> getCachedSquares_Corpses(int level) {
        return this.getNLevels(level).corpseSquares;
    }

    public ArrayList<IsoGridSquare> getCachedSquares_Flies(int level) {
        return this.getNLevels(level).fliesSquares;
    }

    public ArrayList<IsoGridSquare> getCachedSquares_Items(int level) {
        return this.getNLevels(level).itemSquares;
    }

    public ArrayList<IsoGridSquare> getCachedSquares_Puddles(int level) {
        return this.getNLevels(level).puddleSquares;
    }

    public ArrayList<IsoGridSquare> getCachedSquares_TranslucentFloor(int level) {
        return this.getNLevels(level).translucentFloor;
    }

    public ArrayList<IsoGridSquare> getCachedSquares_TranslucentNonFloor(int level) {
        return this.getNLevels(level).translucentNonFloor;
    }

    public void setRenderedSquaresCount(int level, int count) {
        this.getNLevels(level).renderedSquareCount = count;
    }

    public int getRenderedSquaresCount(int level) {
        return this.getNLevels(level).renderedSquareCount;
    }

    public static int getTextureScale(float cameraZoom) {
        if (DebugOptions.instance.fboRenderChunk.highResChunkTextures.getValue()) {
            return cameraZoom < 0.75F ? 2 : 1;
        } else {
            return 1;
        }
    }

    private boolean isTextureSizeValid(FBORenderChunk renderChunk, float cameraZoom) {
        if (renderChunk == null) {
            return false;
        } else {
            int textureWidth = calculateTextureWidthForLevels(renderChunk.getMinLevel(), renderChunk.getTopLevel(), cameraZoom);
            int textureHeight = calculateTextureHeightForLevels(renderChunk.getMinLevel(), renderChunk.getTopLevel(), cameraZoom);
            return textureWidth == renderChunk.w && textureHeight == renderChunk.h;
        }
    }

    public static int calculateMinLevel(int level) {
        int minLevel = level / 2 * 2;
        if (level < 0) {
            minLevel = Math.abs(level + 1) / 2;
            minLevel = -(minLevel + 1) * 2;
        }

        return minLevel;
    }

    public int getMinLevel(int level) {
        FBORenderLevels.NLevels nLevels = this.getNLevels(level);
        return Math.max(nLevels.minLevel, this.chunk.minLevel);
    }

    public int getMaxLevel(int level) {
        FBORenderLevels.NLevels nLevels = this.getNLevels(level);
        return Math.min(nLevels.minLevel + 2 - 1, this.chunk.maxLevel);
    }

    private FBORenderChunk createFBOForLevel(int level, float cameraZoom) {
        int minLevel = level / 2 * 2;
        int maxLevel = Math.min(minLevel + 2 - 1, this.getChunk().maxLevel);
        if (level < 0) {
            minLevel = Math.abs(level + 1) / 2;
            minLevel = -(minLevel + 1) * 2;
            maxLevel = Math.min(minLevel + 2 - 1, this.chunk.maxLevel);
            minLevel = Math.max(minLevel, this.getChunk().minLevel);
        }

        int textureWidth = calculateTextureWidthForLevels(minLevel, maxLevel, cameraZoom);
        int textureHeight = calculateTextureHeightForLevels(minLevel, maxLevel, cameraZoom);
        FBORenderChunk renderChunk = FBORenderChunkManager.instance.getFullRenderChunk(this.getChunk(), textureWidth, textureHeight);
        renderChunk.highRes = getTextureScale(cameraZoom) > 1;
        renderChunk.setRenderLevels(this);
        renderChunk.minLevel = minLevel;
        FBORenderLevels.NLevels nLevels = this.getNLevels(minLevel);
        nLevels.setDirty(512L);
        nLevels.zoomInfo[getTextureScale(cameraZoom) - 1].renderChunk = renderChunk;
        return renderChunk;
    }

    public void invalidateLevel(int level, long dirtyFlags) {
        FBORenderLevels.NLevels nLevels = this.getNLevels(level);
        nLevels.invalidate(dirtyFlags);
    }

    public void invalidateAll(long dirtyFlags) {
        for (int i = 0; i < this.fbos.length; i++) {
            this.fbos[i].setDirty(dirtyFlags);
        }
    }

    public boolean isDirty(int level, float cameraZoom) {
        FBORenderLevels.NLevels nLevels = this.getNLevels(level);
        return nLevels.isDirty(cameraZoom);
    }

    public boolean isDirty(int level, long dirtyFlags, float cameraZoom) {
        FBORenderLevels.NLevels nLevels = this.getNLevels(level);
        FBORenderLevels.ZoomInfo zoomInfo = nLevels.getZoomInfo(cameraZoom);
        return zoomInfo.dirty && (zoomInfo.dirtyFlags & dirtyFlags) != 0L;
    }

    public void clearDirty(int level, float cameraZoom) {
        FBORenderLevels.NLevels nLevels = this.getNLevels(level);
        nLevels.clearDirty(cameraZoom);
    }

    public void clearDirty(int level, long dirtyFlags, float cameraZoom) {
        FBORenderLevels.NLevels nLevels = this.getNLevels(level);
        nLevels.clearDirty(dirtyFlags, cameraZoom);
    }

    public void freeChunk() {
        for (int i = 0; i < this.fbos.length; i++) {
            FBORenderLevels.NLevels nLevels = this.fbos[i];

            for (int j = 0; j < nLevels.zoomInfo.length; j++) {
                FBORenderLevels.ZoomInfo zoomInfo = nLevels.zoomInfo[j];
                if (zoomInfo.renderChunk != null) {
                    this.freeFBO(zoomInfo.renderChunk);
                }
            }
        }

        this.seamChunkE = this.seamChunkSe = this.seamChunkS = null;
        this.prevMinZ = Integer.MAX_VALUE;
        this.prevMaxZ = Integer.MIN_VALUE;
    }

    public void freeFBO(FBORenderChunk renderChunk) {
        FBORenderChunkManager.instance.chunks.remove(renderChunk.index);
        FBORenderChunkManager.instance.toRenderThisFrame.remove(renderChunk);
        FBORenderLevels.NLevels nLevels = this.getNLevels(renderChunk.getMinLevel());

        for (int i = 0; i < nLevels.zoomInfo.length; i++) {
            FBORenderLevels.ZoomInfo zoomInfo = nLevels.zoomInfo[i];
            if (zoomInfo.renderChunk == renderChunk) {
                zoomInfo.renderChunk = null;
                zoomInfo.dirty = true;
                zoomInfo.dirtyFlags = 0L;
                break;
            }
        }

        nLevels.onScreen = false;
        if (this.hasNoRenderChunks()) {
            FBORenderChunkManager.instance.chunkFullMap.remove(this.chunk);
        }

        if (renderChunk.tex != null) {
            FBORenderChunkManager.instance.addToStore(renderChunk);
        }
    }

    public void freeFBOsForLevel(int level) {
        FBORenderLevels.NLevels nLevels = this.getNLevels(level);

        for (int i = 0; i < nLevels.zoomInfo.length; i++) {
            FBORenderChunk renderChunk = nLevels.zoomInfo[i].renderChunk;
            if (renderChunk != null) {
                this.freeFBO(renderChunk);
            }
        }
    }

    boolean hasNoRenderChunks() {
        for (int i = 0; i < this.fbos.length; i++) {
            FBORenderLevels.NLevels nLevels = this.fbos[i];

            for (int j = 0; j < nLevels.zoomInfo.length; j++) {
                FBORenderLevels.ZoomInfo zoomInfo = nLevels.zoomInfo[j];
                if (zoomInfo.renderChunk != null) {
                    return false;
                }
            }
        }

        return true;
    }

    public void handleDelayedLoading(IsoObject object) {
        this.getNLevels(object.getSquare().getZ()).delayedLoading = true;
    }

    public boolean isDelayedLoading(int level) {
        return this.getNLevels(level).delayedLoading;
    }

    public void clearDelayedLoading(int level) {
        this.getNLevels(level).delayedLoading = false;
    }

    public void clearCache() {
        for (FBORenderLevels.NLevels nLevels : this.fbos) {
            for (int j = 0; j < nLevels.zoomInfo.length; j++) {
                FBORenderLevels.ZoomInfo zoomInfo = nLevels.zoomInfo[j];
                this.clearCache(zoomInfo.renderChunk);
                zoomInfo.renderChunk = null;
            }
        }
    }

    private void clearCache(FBORenderChunk renderChunk) {
        if (renderChunk != null) {
            if (renderChunk.tex != null && renderChunk.tex.getTextureId() != null) {
                FBORenderChunkManager.instance.addToStore(renderChunk);
            }
        }
    }

    public static int calculateTextureWidthForLevels(int minLevel, int maxLevel, float cameraZoom) {
        int CPW = 8;
        int numLevels = maxLevel - minLevel + 1;
        float x1 = IsoUtils.XToScreen(0.0F, 0.0F, 0.0F, 0);
        float x2 = IsoUtils.XToScreen(8.0F, 0.0F, 0.0F, 0);
        float x3 = IsoUtils.XToScreen(8.0F, 8.0F, 0.0F, 0);
        float x4 = IsoUtils.XToScreen(0.0F, 8.0F, 0.0F, 0);
        float minX = PZMath.min(x1, x2, x3, x4);
        float maxX = PZMath.max(x1, x2, x3, x4);
        x1 = IsoUtils.XToScreen(0.0F, 0.0F, numLevels, 0);
        x2 = IsoUtils.XToScreen(8.0F, 0.0F, numLevels, 0);
        x3 = IsoUtils.XToScreen(8.0F, 8.0F, numLevels, 0);
        x4 = IsoUtils.XToScreen(0.0F, 8.0F, numLevels, 0);
        minX = PZMath.min(minX, x1, x2, x3, x4);
        maxX = PZMath.max(maxX, x1, x2, x3, x4);
        int w = (int)Math.ceil(maxX - minX);
        return ImageUtils.getNextPowerOfTwoHW(w * getTextureScale(cameraZoom));
    }

    public static int calculateTextureHeightForLevels(int minLevel, int maxLevel, float cameraZoom) {
        int CPW = 8;
        int numLevels = maxLevel - minLevel + 1;
        float y1 = IsoUtils.YToScreen(0.0F, 0.0F, 0.0F, 0);
        float y2 = IsoUtils.YToScreen(8.0F, 0.0F, 0.0F, 0);
        float y3 = IsoUtils.YToScreen(8.0F, 8.0F, 0.0F, 0);
        float y4 = IsoUtils.YToScreen(0.0F, 8.0F, 0.0F, 0);
        float minY = PZMath.min(y1, y2, y3, y4);
        float maxY = PZMath.max(y1, y2, y3, y4);
        y1 = IsoUtils.YToScreen(0.0F, 0.0F, numLevels, 0);
        y2 = IsoUtils.YToScreen(8.0F, 0.0F, numLevels, 0);
        y3 = IsoUtils.YToScreen(8.0F, 8.0F, numLevels, 0);
        y4 = IsoUtils.YToScreen(0.0F, 8.0F, numLevels, 0);
        minY = PZMath.min(minY, y1, y2, y3, y4);
        maxY = PZMath.max(maxY, y1, y2, y3, y4);
        int h = (int)Math.ceil(maxY - minY);
        int var15 = h + extraHeightForJumboTrees(minLevel, maxLevel);
        return ImageUtils.getNextPowerOfTwoHW(var15 * getTextureScale(cameraZoom));
    }

    public static int extraHeightForJumboTrees(int minLevel, int maxLevel) {
        if (minLevel != 0) {
            return 0;
        } else if (maxLevel > 1) {
            return 0;
        } else {
            return maxLevel == 1
                ? FBORenderChunk.JUMBO_HEIGHT - FBORenderChunk.PIXELS_PER_LEVEL * 2 - FBORenderChunk.FLOOR_HEIGHT / 2
                : FBORenderChunk.JUMBO_HEIGHT - FBORenderChunk.PIXELS_PER_LEVEL - FBORenderChunk.FLOOR_HEIGHT / 2;
        }
    }

    private static final class NLevels {
        final int minLevel;
        final FBORenderLevels.ZoomInfo[] zoomInfo = new FBORenderLevels.ZoomInfo[2];
        boolean onScreen;
        boolean delayedLoading;
        int renderedSquareCount;
        final ArrayList<IsoGridSquare> animatedAttachments = new ArrayList<>();
        final ArrayList<IsoGridSquare> corpseSquares = new ArrayList<>();
        final ArrayList<IsoGridSquare> fliesSquares = new ArrayList<>();
        final ArrayList<IsoGridSquare> itemSquares = new ArrayList<>();
        final ArrayList<IsoGridSquare> puddleSquares = new ArrayList<>();
        final ArrayList<IsoGridSquare> translucentFloor = new ArrayList<>();
        final ArrayList<IsoGridSquare> translucentNonFloor = new ArrayList<>();

        NLevels(int minLevel) {
            this.minLevel = minLevel;

            for (int i = 0; i < this.zoomInfo.length; i++) {
                this.zoomInfo[i] = new FBORenderLevels.ZoomInfo();
            }
        }

        FBORenderLevels.ZoomInfo getZoomInfo(float cameraZoom) {
            return this.zoomInfo[FBORenderLevels.getTextureScale(cameraZoom) - 1];
        }

        public boolean isDirty(float cameraZoom) {
            return this.getZoomInfo(cameraZoom).dirty;
        }

        public void setDirty(long flags) {
            for (int i = 0; i < this.zoomInfo.length; i++) {
                this.zoomInfo[i].dirty = true;
                this.zoomInfo[i].dirtyFlags |= flags;
            }
        }

        public void clearDirty(float cameraZoom) {
            FBORenderLevels.ZoomInfo zoomInfo = this.getZoomInfo(cameraZoom);
            zoomInfo.dirty = false;
            zoomInfo.dirtyFlags = 0L;
        }

        public void clearDirty(long flags, float cameraZoom) {
            FBORenderLevels.ZoomInfo zoomInfo = this.getZoomInfo(cameraZoom);
            zoomInfo.dirtyFlags &= ~flags;
            zoomInfo.dirty = zoomInfo.dirtyFlags != 0L;
        }

        void invalidate(long dirtyFlags) {
            this.setDirty(dirtyFlags);
            if (FBORenderLevels.clearCachedSquares) {
                if (!FBORenderChunkManager.instance.isCaching()) {
                    this.animatedAttachments.clear();
                    this.corpseSquares.clear();
                    this.fliesSquares.clear();
                    this.itemSquares.clear();
                    this.puddleSquares.clear();
                    this.translucentFloor.clear();
                    this.translucentNonFloor.clear();
                }
            }
        }
    }

    private static final class ZoomInfo {
        FBORenderChunk renderChunk;
        boolean dirty;
        long dirtyFlags = 0L;
    }
}
