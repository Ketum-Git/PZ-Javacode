// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.savefile;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjglx.opengl.Display;
import zombie.IndieGL;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.RenderSettings;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.sprite.SpriteRenderState;
import zombie.core.textures.MultiTextureFBO2;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.sprite.IsoSprite;
import zombie.ui.UIManager;

public final class SavefileThumbnail {
    private static final int WIDTH = 256;
    private static final int HEIGHT = 256;
    private static VertexBufferObject.VertexFormat formatPositionColorUv;
    private static boolean creatingThumbnail;
    public static String serverAddress;
    public static int serverPort;
    public static String accountUsername;
    static final SavefileThumbnail.CopyTextureDrawer copyTextureDrawer = new SavefileThumbnail.CopyTextureDrawer();

    public static void createForMP(String serverAddress, int serverPort, String accountUsername) {
        SavefileThumbnail.serverAddress = serverAddress;
        SavefileThumbnail.serverPort = serverPort;
        SavefileThumbnail.accountUsername = accountUsername;
        create();
    }

    public static void create() {
        int playerIndex = -1;

        for (int n = 0; n < IsoPlayer.numPlayers; n++) {
            if (IsoPlayer.players[n] != null) {
                playerIndex = n;
                break;
            }
        }

        if (playerIndex != -1) {
            create(playerIndex);
        }
    }

    public static void create(int playerIndex) {
        Core core = Core.getInstance();
        MultiTextureFBO2 offscreenBuffer = core.offscreenBuffer;
        float oldZoom = offscreenBuffer.getZoom(playerIndex);
        float oldTargetZoom = offscreenBuffer.getTargetZoom(playerIndex);
        setZoom(playerIndex, 1.0F, 1.0F);
        IsoCamera.cameras[playerIndex].center();
        creatingThumbnail = true;

        try {
            renderWorld(playerIndex, true, true);
            SpriteRenderer.instance.drawGeneric(new SavefileThumbnail.TakeScreenShotDrawer(playerIndex));
        } finally {
            creatingThumbnail = false;
        }

        setZoom(playerIndex, oldZoom, oldTargetZoom);
        IsoCamera.cameras[playerIndex].center();
        restoreScreenImage();
        core.RenderOffScreenBuffer();
        if (core.StartFrameUI()) {
            UIManager.render();
        }

        core.EndFrameUI();
    }

    private static void renderWorld(int playerIndex, boolean bClear, boolean bFixCamera) {
        IsoPlayer.setInstance(IsoPlayer.players[playerIndex]);
        IsoCamera.setCameraCharacter(IsoPlayer.players[playerIndex]);
        IsoSprite.globalOffsetX = -1.0F;
        StartFrame(playerIndex, bClear);
        if (bFixCamera) {
            SpriteRenderer.instance.drawGeneric(new SavefileThumbnail.FixCameraDrawer(playerIndex));
        }

        IsoCamera.frameState.set(playerIndex);
        IndieGL.disableDepthTest();
        IsoWorld.instance.render();
        RenderSettings.getInstance().legacyPostRender(playerIndex);
        Core.getInstance().EndFrame(playerIndex);
    }

    private static void StartFrame(int nPlayer, boolean clear) {
        if (!LuaManager.thread.step) {
            Core.getInstance().offscreenBuffer.update();
            IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
            if (isoGameCharacter != null) {
                PlayerCamera camera = IsoCamera.cameras[nPlayer];
                camera.calculateModelViewProjection(isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ());
                camera.calculateFixForJigglyModels(isoGameCharacter.getX(), isoGameCharacter.getY(), isoGameCharacter.getZ());
            }

            if (SceneShaderStore.weatherShader != null && Core.getInstance().offscreenBuffer.current != null) {
                SceneShaderStore.weatherShader.setTexture(Core.getInstance().offscreenBuffer.getTexture(nPlayer));
            }

            if (clear) {
                SpriteRenderer.instance.prePopulating();
            }

            if (!clear) {
                SpriteRenderer.instance.initFromIsoCamera(nPlayer);
            }

            copyScreenImage();
            Texture.bindCount = 0;
            if (Core.getInstance().offscreenBuffer.current != null) {
                SpriteRenderer.instance.glBuffer(1, nPlayer);
            }

            IndieGL.glDepthMask(true);
            IndieGL.glDoStartFrame(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), Core.getInstance().getZoom(nPlayer), nPlayer);
            IndieGL.glClear(17664);
            if (DebugOptions.instance.terrain.renderTiles.highContrastBg.getValue()) {
                SpriteRenderer.instance.glClearColor(255, 0, 255, 255);
                SpriteRenderer.instance.glClear(16384);
            }

            IndieGL.enableBlend();
            Core.getInstance().frameStage = 1;
        }
    }

    private static void setZoom(int playerIndex, float zoom, float targetZoom) {
        Core.getInstance().offscreenBuffer.setZoom(playerIndex, zoom);
        Core.getInstance().offscreenBuffer.setTargetZoom(playerIndex, targetZoom);
        IsoCamera.cameras[playerIndex].zoom = zoom;
        IsoCamera.cameras[playerIndex].offscreenWidth = IsoCamera.getOffscreenWidth(playerIndex);
        IsoCamera.cameras[playerIndex].offscreenHeight = IsoCamera.getOffscreenHeight(playerIndex);
    }

    private static void createWithRenderShader(int playerIndex) {
        int width = 256;
        int height = 256;
        Texture fboTexture = new Texture(256, 256, 16);
        TextureFBO fbo = new TextureFBO(fboTexture, false);
        GL11.glPushAttrib(1048575);

        try {
            fbo.startDrawing(true, false);
            GL11.glViewport(0, 0, 256, 256);
            Core core = Core.getInstance();
            Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
            PROJECTION.setOrtho2D(0.0F, 256.0F, 256.0F, 0.0F);
            core.projectionMatrixStack.push(PROJECTION);
            Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
            MODELVIEW.identity();
            core.modelViewMatrixStack.push(MODELVIEW);
            GL11.glDisable(3089);
            GL11.glDisable(2960);
            GL11.glDisable(3042);
            GL11.glDisable(3008);
            GL11.glDisable(2929);
            GL11.glDisable(2884);
            int sx = IsoCamera.getScreenLeft(playerIndex) + IsoCamera.getScreenWidth(playerIndex) / 2 - 128;
            int sy = IsoCamera.getScreenTop(playerIndex) + IsoCamera.getScreenHeight(playerIndex) / 2 - 128;
            int offscreenW = core.getOffscreenBuffer().getTexture().getWidthHW();
            int offscreenH = core.getOffscreenBuffer().getTexture().getHeightHW();
            float u0 = (float)sx / offscreenW;
            float u1 = (float)(sx + 256) / offscreenW;
            float v0 = (float)sy / offscreenH;
            float v1 = (float)(sy + 256) / offscreenH;
            if (formatPositionColorUv == null) {
                formatPositionColorUv = new VertexBufferObject.VertexFormat(3);
                formatPositionColorUv.setElement(0, VertexBufferObject.VertexType.VertexArray, 12);
                formatPositionColorUv.setElement(1, VertexBufferObject.VertexType.TextureCoordArray, 8);
                formatPositionColorUv.setElement(2, VertexBufferObject.VertexType.ColorArray, 16);
                formatPositionColorUv.calculate();
            }

            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(formatPositionColorUv);
            vbor.setShaderProgram(SceneShaderStore.weatherShader.getProgram());
            vbor.setTextureID(((Texture)core.getOffscreenBuffer().getTexture()).getTextureId());
            vbor.setMode(7);
            vbor.addQuad(0.0F, 0.0F, u0, v1, 0.0F, 256.0F, u0, v0, 256.0F, 256.0F, u1, v0, 256.0F, 0.0F, u1, v1, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            vbor.endRun();
            vbor.flush();
            core.TakeScreenshot(0, 0, 256, 256, TextureFBO.getFuncs().GL_COLOR_ATTACHMENT0());
            fbo.endDrawing();
            Core.getInstance().projectionMatrixStack.pop();
            Core.getInstance().modelViewMatrixStack.pop();
        } finally {
            fbo.destroy();
            GL11.glPopAttrib();
        }
    }

    private static void copyScreenImage() {
        MultiTextureFBO2 offscreenBuffer = Core.getInstance().offscreenBuffer;
        if (offscreenBuffer.current != null) {
            TextureDraw textureDraw = SpriteRenderer.instance.drawGeneric(copyTextureDrawer);
            textureDraw.a = 0;
        }
    }

    private static void restoreScreenImage() {
        MultiTextureFBO2 offscreenBuffer = Core.getInstance().offscreenBuffer;
        if (offscreenBuffer.current != null) {
            TextureDraw textureDraw = SpriteRenderer.instance.drawGeneric(copyTextureDrawer);
            textureDraw.a = 1;
        }
    }

    public static boolean isCreatingThumbnail() {
        return creatingThumbnail;
    }

    static final class CopyTextureDrawer extends TextureDraw.GenericDrawer {
        Texture copyTexture;
        TextureFBO copyFbo;

        @Override
        public void render() {
        }

        @Override
        public void render(TextureDraw texd) {
            MultiTextureFBO2 offscreenBuffer = Core.getInstance().offscreenBuffer;
            Texture offscreenTexture = (Texture)offscreenBuffer.current.getTexture();
            if (texd.a == 0) {
                this.copyTexture = new Texture(offscreenTexture.getWidth(), offscreenTexture.getHeight(), 16);
                this.copyFbo = new TextureFBO(this.copyTexture, true);
                if (Display.capabilities.OpenGL30) {
                    this.blitFramebuffer(offscreenBuffer.current, this.copyFbo);
                } else {
                    this.copyTexture(offscreenBuffer.current, this.copyFbo);
                }
            } else {
                if (Display.capabilities.OpenGL30) {
                    this.blitFramebuffer(this.copyFbo, offscreenBuffer.current);
                } else {
                    this.copyTexture(this.copyFbo, offscreenBuffer.current);
                }

                this.copyFbo.destroy();
                this.copyFbo = null;
                this.copyTexture = null;
                GLStateRenderThread.restore();
            }
        }

        void blitFramebuffer(TextureFBO fboSrc, TextureFBO fboDst) {
            try {
                TextureFBO.getFuncs().glBindFramebuffer(36008, fboSrc.getBufferId());
                TextureFBO.getFuncs().glBindFramebuffer(36009, fboDst.getBufferId());
                GL30.glBlitFramebuffer(0, 0, fboSrc.getWidth(), fboSrc.getHeight(), 0, 0, fboDst.getWidth(), fboDst.getHeight(), 17664, 9728);
            } finally {
                TextureFBO.getFuncs().glBindFramebuffer(36008, 0);
                TextureFBO.getFuncs().glBindFramebuffer(36009, 0);
            }
        }

        void copyTexture(TextureFBO fboSrc, TextureFBO fboDst) {
            Texture texSrc = (Texture)fboSrc.getTexture();
            fboDst.startDrawing(true, false);
            Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
            PROJECTION.setOrtho2D(0.0F, texSrc.getWidth(), texSrc.getHeight(), 0.0F);
            Core.getInstance().projectionMatrixStack.push(PROJECTION);
            Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
            MODELVIEW.identity();
            Core.getInstance().modelViewMatrixStack.push(MODELVIEW);
            GL11.glViewport(0, 0, texSrc.getWidth(), texSrc.getHeight());
            GL11.glDepthMask(false);
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.startRun(vbor.formatPositionColorUv);
            vbor.setTextureID(texSrc.getTextureId());
            vbor.setMode(7);
            float u0 = 0.0F;
            float v0 = 0.0F;
            float u1 = (float)texSrc.getWidth() / texSrc.getWidthHW();
            float v1 = (float)texSrc.getHeight() / texSrc.getHeightHW();
            vbor.addQuad(0.0F, 0.0F, 0.0F, 0.0F, texSrc.getWidth(), texSrc.getHeight(), u1, v1, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            vbor.endRun();
            vbor.flush();
            fboDst.endDrawing();
            Core.getInstance().projectionMatrixStack.pop();
            Core.getInstance().modelViewMatrixStack.pop();
            GLStateRenderThread.restore();
        }
    }

    private static final class FixCameraDrawer extends TextureDraw.GenericDrawer {
        int playerIndex;
        float zoom;
        int offscreenWidth;
        int offscreenHeight;

        FixCameraDrawer(int playerIndex) {
            PlayerCamera camera = IsoCamera.cameras[playerIndex];
            this.playerIndex = playerIndex;
            this.zoom = camera.zoom;
            this.offscreenWidth = camera.offscreenWidth;
            this.offscreenHeight = camera.offscreenHeight;
        }

        @Override
        public void render() {
            SpriteRenderState renderState = SpriteRenderer.instance.getRenderingState();
            renderState.playerCamera[this.playerIndex].zoom = this.zoom;
            renderState.playerCamera[this.playerIndex].offscreenWidth = this.offscreenWidth;
            renderState.playerCamera[this.playerIndex].offscreenHeight = this.offscreenHeight;
            renderState.zoomLevel[this.playerIndex] = this.zoom;
        }
    }

    private static final class TakeScreenShotDrawer extends TextureDraw.GenericDrawer {
        int playerIndex;

        TakeScreenShotDrawer(int playerIndex) {
            this.playerIndex = playerIndex;
        }

        @Override
        public void render() {
            Core core = Core.getInstance();
            MultiTextureFBO2 offscreenBuffer = core.offscreenBuffer;
            if (offscreenBuffer.current == null) {
                Core.getInstance().TakeScreenshot(256, 256, 1029);
            } else if (SceneShaderStore.weatherShader == null) {
                Core.getInstance().getOffscreenBuffer().startDrawing(false, false);
                Core.getInstance().TakeScreenshot(256, 256, TextureFBO.getFuncs().GL_COLOR_ATTACHMENT0());
                Core.getInstance().getOffscreenBuffer().endDrawing();
            } else {
                SavefileThumbnail.createWithRenderShader(this.playerIndex);
            }
        }
    }
}
