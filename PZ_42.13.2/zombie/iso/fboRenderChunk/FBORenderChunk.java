// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoUtils;
import zombie.iso.PlayerCamera;

@UsedFromLua
public final class FBORenderChunk {
    public static final int PIXELS_PER_LEVEL = 96 * Core.tileScale;
    public static final int FLOOR_HEIGHT = 32 * Core.tileScale;
    public static final int JUMBO_HEIGHT = 256 * Core.tileScale;
    public static final int TEXTURE_HEIGHT = 1024;
    public static final int LEVELS_PER_TEXTURE = 2;
    public static final long DIRTY_NONE = 0L;
    public static final long DIRTY_BLOOD = 1L;
    public static final long DIRTY_CORPSE = 2L;
    public static final long DIRTY_ITEM_ADD = 4L;
    public static final long DIRTY_ITEM_REMOVE = 8L;
    public static final long DIRTY_ITEM_MODIFY = 16L;
    public static final long DIRTY_LIGHTING = 32L;
    @UsedFromLua
    public static final long DIRTY_OBJECT_ADD = 64L;
    public static final long DIRTY_OBJECT_REMOVE = 128L;
    public static final long DIRTY_OBJECT_MODIFY = 256L;
    public static final long DIRTY_CREATE = 512L;
    public static final long DIRTY_REDRAW = 1024L;
    public static final long DIRTY_CUTAWAYS = 2048L;
    public static final long DIRTY_TREES = 4096L;
    public static final long DIRTY_OBSCURING = 8192L;
    public static final long DIRTY_REDO_CUTAWAYS = 16384L;
    private FBORenderLevels renderLevels;
    public int index = -1;
    public TextureFBO fbo;
    public boolean submitted;
    public boolean isInit;
    public Texture tex;
    public Texture depth;
    public int w;
    public int h;
    public IsoChunk chunk;
    public boolean highRes;
    public int minLevel;
    public float renderX;
    public float renderY;
    public float renderW;
    public float renderH;

    public void setRenderLevels(FBORenderLevels renderLevels) {
        this.renderLevels = renderLevels;
    }

    public FBORenderLevels getRenderLevels() {
        return this.renderLevels;
    }

    public int getTextureWidth(float cameraZoom) {
        return FBORenderLevels.calculateTextureWidthForLevels(this.getMinLevel(), this.getTopLevel(), cameraZoom);
    }

    public int getTextureHeight(float cameraZoom) {
        return FBORenderLevels.calculateTextureHeightForLevels(this.getMinLevel(), this.getTopLevel(), cameraZoom);
    }

    public int getMinLevel() {
        return this.minLevel;
    }

    public int getTopLevel() {
        if (this.minLevel < 0) {
            int minLevel2 = Math.abs(this.getMinLevel() + 1) / 2;
            return Math.min(-(minLevel2 + 1) * 2 + 2 - 1, this.chunk.maxLevel);
        } else {
            return Math.min(this.minLevel + 2 - 1, this.chunk.maxLevel);
        }
    }

    public boolean isTopLevel(int level) {
        return level == this.getTopLevel();
    }

    public void preInit() {
        this.tex = new Texture(this.w, this.h, 16, true);
        this.tex.setNameOnly("FBORenderChunk.tex");
        this.depth = new Texture(this.w, this.h, 512, true);
        this.depth.setNameOnly("FBORenderChunk.depth");
    }

    public void init() {
        if (!this.isInit) {
            this.depth.TexDeferedCreation(this.w, this.h, 512);
            this.tex.TexDeferedCreation(this.w, this.h, 16);
            this.fbo = new TextureFBO(this.tex, this.depth, false);
            this.isInit = true;
        }
    }

    public void beginMainThread(boolean bClear) {
        SpriteRenderer.instance.FBORenderChunkStart(this.index, bClear);
    }

    public void endMainThread() {
        SpriteRenderer.instance.FBORenderChunkEnd();
    }

    public void beginRenderThread(boolean bClear) {
        if (!this.isInit) {
            this.init();
        }

        if (bClear) {
            this.fbo.startDrawing(true, true);
            GL11.glEnable(2929);
            GL11.glDepthFunc(519);
            GL11.glDepthMask(true);
            GL11.glClearDepth(1.0);
            GL11.glClear(256);
            GLStateRenderThread.DepthFunc.restore();
            GLStateRenderThread.DepthMask.restore();
            GLStateRenderThread.DepthTest.restore();
        } else {
            this.fbo.startDrawing(false, false);
        }
    }

    public void endRenderThread() {
        this.fbo.endDrawing();
        if (DebugOptions.instance.fboRenderChunk.mipMaps.getValue() && !this.highRes) {
            this.tex.bind();
            GL30.glGenerateMipmap(3553);
        }
    }

    public Texture getTexture() {
        return this.tex;
    }

    public void renderInWorldMainThread() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (SceneShaderStore.chunkRenderShader != null) {
            IndieGL.StartShader(SceneShaderStore.chunkRenderShader.getID());
            int numSprites = SpriteRenderer.instance.states.getPopulatingActiveState().numSprites;
            TextureDraw texd = SpriteRenderer.instance.states.getPopulatingActiveState().sprite[numSprites - 1];
            texd.tex1 = this.depth;
            int CPW = 8;
            IsoDepthHelper.Results result = IsoDepthHelper.getChunkDepthData(
                PZMath.fastfloor(IsoCamera.frameState.camCharacterX / 8.0F),
                PZMath.fastfloor(IsoCamera.frameState.camCharacterY / 8.0F),
                this.chunk.wx,
                this.chunk.wy,
                this.getMinLevel()
            );
            texd.chunkDepth = result.depthStart;
            if (!DebugOptions.instance.fboRenderChunk.combinedFbo.getValue()) {
                PlayerCamera camera = IsoCamera.cameras[playerIndex];
                float dx = camera.fixJigglyModelsSquareX;
                float dy = camera.fixJigglyModelsSquareY;
                float depthStart = (result.indexX + result.indexY - dx - dy) / 8.0F / 40.0F;
                depthStart *= 0.46187335F;
                depthStart -= FBORenderLevels.calculateMinLevel(this.getMinLevel()) * 0.0028867084F;
                texd.chunkDepth = depthStart;
            }
        }

        IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
        float x = IsoUtils.XToScreen(this.chunk.wx * 8, this.chunk.wy * 8, this.getMinLevel(), 0);
        float y = IsoUtils.YToScreen(this.chunk.wx * 8, this.chunk.wy * 8, this.getMinLevel(), 0);
        float w = this.w;
        float h = this.h;
        if (this.highRes) {
            w /= 2.0F;
            h /= 2.0F;
        }

        y -= PIXELS_PER_LEVEL * (this.getTopLevel() - this.getMinLevel() + 1);
        y -= FBORenderLevels.extraHeightForJumboTrees(this.getMinLevel(), this.getTopLevel());
        x -= IsoCamera.getOffX();
        y -= IsoCamera.getOffY();
        if (!DebugOptions.instance.fboRenderChunk.combinedFbo.getValue()) {
            x /= IsoCamera.frameState.zoom;
            y /= IsoCamera.frameState.zoom;
            w /= IsoCamera.frameState.zoom;
            h /= IsoCamera.frameState.zoom;
        }

        x -= w / 2.0F;
        if (!DebugOptions.instance.fboRenderChunk.combinedFbo.getValue()) {
            x += IsoCamera.cameras[playerIndex].fixJigglyModelsX;
            y += IsoCamera.cameras[playerIndex].fixJigglyModelsY;
        }

        if (this.tex.getTextureId() != null) {
            boolean bMipMaps = DebugOptions.instance.fboRenderChunk.mipMaps.getValue() && !this.highRes;
            this.tex.getTextureId().setMinFilter(bMipMaps ? 9987 : 9728);
            this.tex.getTextureId().setMagFilter(IsoCamera.frameState.zoom == 0.75F ? 9729 : 9728);
        }

        IndieGL.glDepthFunc(515);
        IndieGL.glDepthMask(true);
        IndieGL.enableDepthTest();
        SpriteRenderer.instance.render(this.getTexture(), x, y, w, h, 1.0F, 1.0F, 1.0F, 1.0F, null);
        IndieGL.enableDepthTest();
        IndieGL.glDepthMask(true);
        if (SceneShaderStore.chunkRenderShader != null) {
            IndieGL.EndShader();
        }

        this.renderX = x;
        this.renderY = y;
        this.renderW = w;
        this.renderH = h;
    }
}
