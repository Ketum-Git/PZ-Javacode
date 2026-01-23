// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.CharacterModelCamera;
import zombie.core.opengl.ShaderProgram;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.ModelCamera;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.interfaces.ITexture;
import zombie.iso.IsoCamera;
import zombie.popman.ObjectPool;

public final class ModelOutlines {
    public static final ModelOutlines instance = new ModelOutlines();
    public TextureFBO fboA;
    public TextureFBO fboB;
    public TextureFBO fboC;
    public boolean dirty;
    private int playerIndex;
    private final ColorInfo outlineColor = new ColorInfo();
    private boolean behindPlayer;
    private ModelSlotRenderData playerRenderData;
    private ShaderProgram thickenHShader;
    private ShaderProgram thickenVShader;
    private ShaderProgram blitShader;
    private final ObjectPool<ModelOutlines.Drawer> drawerPool = new ObjectPool<>(ModelOutlines.Drawer::new);

    public void startFrameMain(int playerIndex) {
        ModelOutlines.Drawer drawer = this.drawerPool.alloc();
        drawer.startFrame = true;
        drawer.playerIndex = playerIndex;
        SpriteRenderer.instance.drawGeneric(drawer);
    }

    public void endFrameMain(int playerIndex) {
        ModelOutlines.Drawer drawer = this.drawerPool.alloc();
        drawer.startFrame = false;
        drawer.playerIndex = playerIndex;
        SpriteRenderer.instance.drawGeneric(drawer);
    }

    public void startFrame(int playerIndex) {
        this.dirty = false;
        this.playerIndex = playerIndex;
        this.playerRenderData = null;
    }

    public void checkFBOs() {
        if (this.fboA != null && (this.fboA.getWidth() != Core.width || this.fboB.getHeight() != Core.height)) {
            this.fboA.destroy();
            this.fboB.destroy();
            this.fboC.destroy();
            this.fboA = null;
            this.fboB = null;
            this.fboC = null;
        }

        if (this.fboA == null) {
            Texture texA = new Texture(Core.width, Core.height, 16);
            this.fboA = new TextureFBO(texA, false);
            Texture texB = new Texture(Core.width, Core.height, 16);
            this.fboB = new TextureFBO(texB, false);
            Texture texC = new Texture(Core.width, Core.height, 16);
            this.fboC = new TextureFBO(texC, false);
        }
    }

    public void setPlayerRenderData(ModelSlotRenderData slotData) {
        this.playerRenderData = slotData;
    }

    public boolean beginRenderOutline(ColorInfo outlineColor, boolean bBehindPlayer, boolean bPlayerToMask) {
        if (!bPlayerToMask && this.dirty && (!this.outlineColor.equals(outlineColor) || bBehindPlayer != this.behindPlayer)) {
            this.endFrame(SpriteRenderer.instance.getRenderingPlayerIndex());
            this.dirty = false;
        }

        this.outlineColor.set(outlineColor);
        this.behindPlayer = bBehindPlayer;
        if (this.dirty) {
            return false;
        } else {
            this.dirty = true;
            this.checkFBOs();
            return true;
        }
    }

    public void endFrame(int playerIndex) {
        if (this.dirty) {
            this.playerIndex = playerIndex;
            if (this.thickenHShader == null) {
                this.thickenHShader = ShaderProgram.createShaderProgram("aim_outline_h", false, false, true);
                this.thickenVShader = ShaderProgram.createShaderProgram("aim_outline_v", false, false, true);
                this.blitShader = ShaderProgram.createShaderProgram("aim_outline_blit", false, false, true);
            }

            int x0 = IsoCamera.getScreenLeft(this.playerIndex);
            int y0 = IsoCamera.getScreenTop(this.playerIndex);
            int w = IsoCamera.getScreenWidth(this.playerIndex);
            int h = IsoCamera.getScreenHeight(this.playerIndex);
            Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
            PROJECTION.setOrtho2D(0.0F, w, h, 0.0F);
            Core.getInstance().projectionMatrixStack.push(PROJECTION);
            Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
            MODELVIEW.identity();
            Core.getInstance().modelViewMatrixStack.push(MODELVIEW);
            float resolutionX = this.fboA.getWidth();
            float resolutionY = this.fboA.getHeight();
            float zoom = SpriteRenderer.instance.getPlayerZoomLevel();
            float radius = PZMath.lerp(0.5F, 0.2F, zoom / 2.5F);
            GL11.glBlendFunc(770, 771);
            this.fboB.startDrawing(true, true);
            GL11.glViewport(x0, y0, w, h);
            this.thickenHShader.Start();
            this.thickenHShader.setVector2("u_resolution", resolutionX, resolutionY);
            this.thickenHShader.setValue("u_radius", radius);
            this.thickenHShader.setVector4("u_color", this.outlineColor.r, this.outlineColor.g, this.outlineColor.b, this.outlineColor.a);
            this.renderTexture(this.fboA.getTexture(), x0, y0, w, h, this.thickenHShader);
            this.thickenHShader.End();
            this.fboB.endDrawing();
            this.fboC.startDrawing(true, true);
            GL11.glViewport(x0, y0, w, h);
            this.thickenVShader.Start();
            this.thickenVShader.setVector2("u_resolution", resolutionX, resolutionY);
            this.thickenVShader.setValue("u_radius", radius);
            this.thickenVShader.setVector4("u_color", this.outlineColor.r, this.outlineColor.g, this.outlineColor.b, this.outlineColor.a);
            this.renderTexture(this.fboB.getTexture(), x0, y0, w, h, this.thickenVShader);
            this.thickenVShader.End();
            this.fboC.endDrawing();
            if (this.playerRenderData != null && this.behindPlayer) {
                float camX = CharacterModelCamera.instance.x;
                float camY = CharacterModelCamera.instance.y;
                float camZ = CharacterModelCamera.instance.z;
                boolean bInVehicle = CharacterModelCamera.instance.inVehicle;
                float useAngle = CharacterModelCamera.instance.useAngle;
                boolean bUseWorldIso = CharacterModelCamera.instance.useWorldIso;
                boolean bDepthMask = CharacterModelCamera.instance.depthMask;
                ModelCamera camera = ModelCamera.instance;
                CharacterModelCamera.instance.x = this.playerRenderData.x;
                CharacterModelCamera.instance.y = this.playerRenderData.y;
                CharacterModelCamera.instance.z = this.playerRenderData.z;
                CharacterModelCamera.instance.inVehicle = this.playerRenderData.inVehicle;
                CharacterModelCamera.instance.useAngle = this.playerRenderData.animPlayerAngle;
                CharacterModelCamera.instance.useWorldIso = true;
                CharacterModelCamera.instance.depthMask = false;
                ModelCamera.instance = CharacterModelCamera.instance;
                GL11.glViewport(x0, y0, w, h);
                this.playerRenderData.performRenderCharacterOutline(true, this.outlineColor, this.behindPlayer);
                CharacterModelCamera.instance.x = camX;
                CharacterModelCamera.instance.y = camY;
                CharacterModelCamera.instance.z = camZ;
                CharacterModelCamera.instance.inVehicle = bInVehicle;
                CharacterModelCamera.instance.useAngle = useAngle;
                CharacterModelCamera.instance.useWorldIso = bUseWorldIso;
                CharacterModelCamera.instance.depthMask = bDepthMask;
                ModelCamera.instance = camera;
            }

            GL11.glBlendFunc(770, 771);
            GL11.glViewport(x0, y0, w, h);
            this.blitShader.Start();
            this.blitShader.setSamplerUnit("texture", 0);
            this.blitShader.setSamplerUnit("mask", 1);
            GL13.glActiveTexture(33985);
            GL11.glBindTexture(3553, this.fboA.getTexture().getID());
            GL13.glActiveTexture(33984);
            this.renderTexture(this.fboC.getTexture(), x0, y0, w, h, this.blitShader);
            this.blitShader.End();
            Core.getInstance().projectionMatrixStack.pop();
            Core.getInstance().modelViewMatrixStack.pop();
            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        }
    }

    private void renderTexture(ITexture texture, int x0, int y0, int w, int h, ShaderProgram shaderProgram) {
        VBORenderer vbor = VBORenderer.getInstance();
        vbor.startRun(vbor.formatPositionColorUv);
        vbor.setMode(7);
        vbor.setTextureID(((Texture)texture).getTextureId());
        vbor.setShaderProgram(shaderProgram);
        float u0 = (float)x0 / texture.getWidthHW();
        float v0 = (float)(y0 + h) / texture.getHeightHW();
        float u1 = (float)(x0 + w) / texture.getWidthHW();
        float v1 = (float)y0 / texture.getHeightHW();
        float z = 0.0F;
        int var14 = 0;
        int var13 = 0;
        vbor.addQuad(var13, var14, u0, v0, var13 + w, var14 + h, u1, v1, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
        vbor.endRun();
        vbor.flush();
    }

    public void renderDebug() {
    }

    public static final class Drawer extends TextureDraw.GenericDrawer {
        boolean startFrame;
        int playerIndex;

        @Override
        public void render() {
            if (this.startFrame) {
                ModelOutlines.instance.startFrame(this.playerIndex);
            } else {
                ModelOutlines.instance.endFrame(this.playerIndex);
            }
        }

        @Override
        public void postRender() {
            ModelOutlines.instance.drawerPool.release(this);
        }
    }
}
