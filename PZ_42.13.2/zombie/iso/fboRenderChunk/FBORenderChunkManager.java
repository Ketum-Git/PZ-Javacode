// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.DefaultShader;
import zombie.core.SceneShaderStore;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;

public final class FBORenderChunkManager {
    public static FBORenderChunkManager instance = new FBORenderChunkManager();
    final HashMap<Integer, ArrayList<FBORenderChunk>> sizeChunkStore = new HashMap<>();
    final Stack<FBORenderChunk> toRecycle = new Stack<>();
    public final HashSet<IsoChunk> chunkFullMap = new HashSet<>();
    public final HashMap<Integer, FBORenderChunk> chunks = new HashMap<>();
    public final ArrayList<FBORenderChunk> toRenderThisFrame = new ArrayList<>();
    private final ArrayList<FBORenderChunk> tempRenderChunks = new ArrayList<>();
    public FBORenderChunk renderThreadCurrent;
    int rcIndex;
    float yoff;
    float xoff;
    boolean caching;
    public FBORenderChunk renderChunk;
    public Texture combinedTexture;
    public Texture combinedDepthTexture;
    private TextureFBO combinedFbo;

    public float getYOffset() {
        return this.yoff;
    }

    public float getXOffset() {
        return this.xoff;
    }

    public void gameLoaded() {
        this.recycle();
        int playerIndex = 0;
        IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[0];

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                IsoChunk chunk = chunkMap.getChunk(x, y);
                if (chunk != null) {
                    FBORenderLevels renderLevels = chunk.getRenderLevels(0);

                    for (int level = chunk.minLevel; level <= chunk.maxLevel; level++) {
                        float cameraZoom = 1.0F;
                        FBORenderChunk renderChunk = renderLevels.getOrCreateFBOForLevel(level, cameraZoom);
                        cameraZoom = 0.5F;
                        renderChunk = renderLevels.getOrCreateFBOForLevel(level, cameraZoom);
                    }
                }
            }
        }
    }

    public void recycle() {
        for (int i = 0; i < this.toRecycle.size(); i++) {
            FBORenderChunk fboRenderChunk = this.toRecycle.get(i);
            int sizeIndex = fboRenderChunk.h;
            if (!fboRenderChunk.submitted) {
                ArrayList<FBORenderChunk> st;
                if (this.sizeChunkStore.containsKey(sizeIndex)) {
                    st = this.sizeChunkStore.get(sizeIndex);
                } else {
                    st = new ArrayList<>();
                    this.sizeChunkStore.put(sizeIndex, st);
                }

                st.add(fboRenderChunk);
                this.toRecycle.remove(fboRenderChunk);
                i--;
            }
        }
    }

    public boolean endCaching() {
        if (this.caching) {
            this.renderChunk.endMainThread();
            SpriteRenderer.instance.glDoEndFrame();
            SpriteRenderer.instance
                .glDoStartFrame(
                    Core.getInstance().getScreenWidth(),
                    Core.getInstance().getScreenHeight(),
                    Core.getInstance().getCurrentPlayerZoom(),
                    IsoCamera.frameState.playerIndex
                );
            this.caching = false;
            return true;
        } else {
            return false;
        }
    }

    public void submitCachesForFrame() {
        int playerIndex = IsoCamera.frameState.playerIndex;
        TIntObjectHashMap<FBORenderChunk> cachedRenderChunkIndexMap = SpriteRenderer.instance.getPopulatingState().cachedRenderChunkIndexMap;
        this.tempRenderChunks.clear();
        cachedRenderChunkIndexMap.forEachValue(fboRenderChunk -> {
            this.tempRenderChunks.add(fboRenderChunk);
            return true;
        });

        for (int i = 0; i < this.tempRenderChunks.size(); i++) {
            FBORenderChunk renderChunk1 = this.tempRenderChunks.get(i);
            if (renderChunk1.getRenderLevels().getPlayerIndex() == playerIndex) {
                cachedRenderChunkIndexMap.remove(renderChunk1.index);
            }
        }

        for (int ix = 0; ix < this.toRenderThisFrame.size(); ix++) {
            FBORenderChunk renderChunk1 = this.toRenderThisFrame.get(ix);
            cachedRenderChunkIndexMap.put(renderChunk1.index, renderChunk1);
            renderChunk1.submitted = true;
        }
    }

    public boolean beginRenderChunkLevel(IsoChunk chunk, int level, float cameraZoom, boolean canRender, boolean canClear) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
        this.renderChunk = renderLevels.getOrCreateFBOForLevel(level, cameraZoom);
        int textureWidth = this.renderChunk.w;
        this.xoff = textureWidth / 2.0F;
        this.yoff = (this.renderChunk.getTopLevel() - this.renderChunk.getMinLevel() + 1) * FBORenderChunk.PIXELS_PER_LEVEL;
        this.yoff = this.yoff + this.renderChunk.getMinLevel() * FBORenderChunk.PIXELS_PER_LEVEL;
        this.yoff = this.yoff + FBORenderLevels.extraHeightForJumboTrees(this.renderChunk.getMinLevel(), this.renderChunk.getTopLevel());
        if (renderLevels.isDirty(level, cameraZoom)) {
            if (level == this.renderChunk.getMinLevel()) {
                this.caching = true;
                SpriteRenderer.instance.glDoEndFrame();
                SpriteRenderer.instance.glDoStartFrameFlipY(this.renderChunk.w, this.renderChunk.h, this.renderChunk.highRes ? 1.0F : 0.0F, playerIndex);
                this.renderChunk.beginMainThread(true);
            }

            return true;
        } else {
            return false;
        }
    }

    public void endRenderChunkLevel(IsoChunk chunk, int level, float cameraZoom, boolean clearDirty) {
        if (this.renderChunk.isTopLevel(level)) {
            if (this.isCaching()) {
                int playerIndex = IsoCamera.frameState.playerIndex;
                FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
                if (clearDirty) {
                    renderLevels.clearDirty(level, cameraZoom);
                }

                this.endCaching();
            }

            this.toRenderThisFrame.add(this.renderChunk);
            this.renderChunk = null;
        }
    }

    public void clearCache() {
        for (IsoChunk chunk : this.chunkFullMap) {
            for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                FBORenderLevels renderLevels = chunk.getRenderLevels(playerIndex);
                renderLevels.clearCache();
            }
        }

        this.chunkFullMap.clear();
        this.chunks.clear();
    }

    protected void addToStore(FBORenderChunk val) {
        if (!this.toRecycle.contains(val)) {
            this.toRecycle.add(val);
        }
    }

    public void freeChunk(IsoChunk chunk) {
        for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
            chunk.getRenderLevels(playerIndex).freeChunk();
        }
    }

    public FBORenderChunk getFullRenderChunk(IsoChunk c, int w, int h) {
        ArrayList<FBORenderChunk> st = this.sizeChunkStore.get(h);
        boolean isNew = false;
        FBORenderChunk rc;
        if (st != null && !st.isEmpty()) {
            rc = st.remove(st.size() - 1);
        } else {
            rc = new FBORenderChunk();
            isNew = true;
        }

        rc.chunk = c;
        rc.w = w;
        rc.h = h;
        if (rc.index == -1) {
            rc.index = this.rcIndex++;
        }

        this.chunkFullMap.add(c);
        this.chunks.put(rc.index, rc);
        if (isNew) {
            rc.preInit();
        }

        return rc;
    }

    public boolean isCaching() {
        return this.caching;
    }

    public void renderThreadChunkEnd() {
        if (this.renderThreadCurrent != null) {
            this.renderThreadCurrent.endRenderThread();
            this.renderThreadCurrent = null;
        }
    }

    public void renderThreadChunkStart(int index, boolean bClear) {
        this.renderThreadCurrent = SpriteRenderer.instance.getRenderingState().cachedRenderChunkIndexMap.get(index);
        if (this.renderThreadCurrent != null) {
            this.renderThreadCurrent.beginRenderThread(bClear);
        }
    }

    private void checkCombinedFBO() {
        int width = (int)Math.ceil(Core.width * 2.5F);
        int height = (int)Math.ceil(Core.height * 2.5F);
        if (this.combinedFbo == null || this.combinedFbo.getWidth() != width || this.combinedFbo.getHeight() != height) {
            if (this.combinedFbo != null) {
                this.combinedFbo.destroy();
            }

            this.combinedTexture = new Texture(width, height, 16);
            this.combinedDepthTexture = new Texture(width, height, 512);
            this.combinedFbo = new TextureFBO(this.combinedTexture, this.combinedDepthTexture, false);
        }
    }

    public void startFrame() {
        this.toRenderThisFrame.clear();
    }

    public void endFrame() {
        if (!DebugOptions.instance.fboRenderChunk.renderChunkTextures.getValue()) {
            instance.submitCachesForFrame();
            SpriteRenderer.instance.releaseFBORenderChunkLock();
        } else {
            if (DebugOptions.instance.fboRenderChunk.combinedFbo.getValue() && !this.toRenderThisFrame.isEmpty()) {
                int playerIndex = IsoCamera.frameState.playerIndex;
                int offscreenWidth = Core.getInstance().getOffscreenWidth(playerIndex);
                int offscreenHeight = Core.getInstance().getOffscreenHeight(playerIndex);
                SpriteRenderer.instance.glDoEndFrame();
                this.checkCombinedFBO();
                SpriteRenderer.instance.glDoStartFrameNoZoom(this.combinedTexture.getWidth(), this.combinedTexture.getHeight(), 1.0F, playerIndex);
                SpriteRenderer.instance.glBuffer(10, playerIndex);

                for (int i = 0; i < this.toRenderThisFrame.size(); i++) {
                    FBORenderChunk renderChunk1 = this.toRenderThisFrame.get(i);
                    if (renderChunk1.getRenderLevels().getPlayerIndex() == playerIndex) {
                        renderChunk1.renderInWorldMainThread();
                    }
                }

                SpriteRenderer.instance.glBuffer(11, playerIndex);
                SpriteRenderer.instance.glDoEndFrame();
                SpriteRenderer.instance.glDoStartFrame(Core.width, Core.height, 1.0F, IsoCamera.frameState.playerIndex);
                IndieGL.enableDepthTest();
                IndieGL.glDepthFunc(519);
                IndieGL.glDepthMask(true);
                IndieGL.glBlendFunc(770, 771);
                float zoom = Core.getInstance().getZoom(playerIndex);
                if (SceneShaderStore.chunkRenderShader != null) {
                    IndieGL.StartShader(SceneShaderStore.chunkRenderShader.getID());
                    int numSprites = SpriteRenderer.instance.states.getPopulatingActiveState().numSprites;
                    TextureDraw texd = SpriteRenderer.instance.states.getPopulatingActiveState().sprite[numSprites - 1];
                    texd.tex1 = this.combinedDepthTexture;
                    texd.chunkDepth = 0.0F;
                }

                if (this.combinedTexture.getTextureId() != null) {
                    this.combinedTexture.getTextureId().setMinFilter(9729);
                    this.combinedTexture.getTextureId().setMagFilter(9729);
                }

                PlayerCamera camera = IsoCamera.cameras[playerIndex];
                this.combinedTexture
                    .rendershader2(
                        0.0F + camera.fixJigglyModelsX,
                        0.0F + camera.fixJigglyModelsY,
                        Core.width,
                        Core.height,
                        0,
                        this.combinedTexture.getHeight() - (int)(Core.height * zoom),
                        (int)(Core.width * zoom),
                        (int)(Core.height * zoom),
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                    );
                if (SceneShaderStore.chunkRenderShader != null) {
                    IndieGL.EndShader();
                }

                SpriteRenderer.instance.glDoEndFrame();
                SpriteRenderer.instance.glDoStartFrame(offscreenWidth, offscreenHeight, Core.getInstance().getCurrentPlayerZoom(), playerIndex);
            }

            if (!DebugOptions.instance.fboRenderChunk.combinedFbo.getValue() && !this.toRenderThisFrame.isEmpty()) {
                int playerIndex = IsoCamera.frameState.playerIndex;
                int offscreenWidth = Core.getInstance().getOffscreenWidth(playerIndex);
                int offscreenHeight = Core.getInstance().getOffscreenHeight(playerIndex);
                SpriteRenderer.instance.glDoEndFrame();
                SpriteRenderer.instance.glDoStartFrameNoZoom(offscreenWidth, offscreenHeight, Core.getInstance().getCurrentPlayerZoom(), playerIndex);

                for (int ix = 0; ix < this.toRenderThisFrame.size(); ix++) {
                    FBORenderChunk renderChunk1 = this.toRenderThisFrame.get(ix);
                    if (renderChunk1.getRenderLevels().getPlayerIndex() == playerIndex) {
                        renderChunk1.renderInWorldMainThread();
                    }
                }

                SpriteRenderer.instance.glDoEndFrame();
                SpriteRenderer.instance.glDoStartFrame(offscreenWidth, offscreenHeight, Core.getInstance().getCurrentPlayerZoom(), playerIndex);
            }

            instance.submitCachesForFrame();
            SpriteRenderer.instance.releaseFBORenderChunkLock();
        }
    }

    public void startDrawingCombined() {
        GL11.glEnable(2929);
        GL11.glDepthFunc(519);
        GL11.glDepthMask(true);
        this.combinedFbo.startDrawing(true, false);
        GL11.glClearDepth(1.0);
        GL11.glClear(256);
    }

    public void endDrawingCombined() {
        this.combinedFbo.endDrawing();
        GL20.glUseProgram(0);
        DefaultShader.isActive = false;
        ShaderHelper.forgetCurrentlyBound();
    }

    public void Reset() {
        this.clearCache();
        this.recycle();

        for (ArrayList<FBORenderChunk> renderChunks : this.sizeChunkStore.values()) {
            for (FBORenderChunk renderChunk : renderChunks) {
                if (renderChunk.fbo != null) {
                    renderChunk.fbo.destroy();
                    renderChunk.fbo = null;
                }
            }

            renderChunks.clear();
        }

        this.sizeChunkStore.clear();
        this.renderChunk = null;
    }
}
