// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite;

import java.util.function.Consumer;
import org.lwjgl.opengl.GL11;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.Shader;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;

public final class IsoReticle {
    private static IsoReticle instance;
    private boolean defaultColor = true;
    private boolean hasValidTarget;
    private float targetChance;
    private float targetAimPenalty;
    private float currentCrosshairOffset = 60.0F;
    private float targetCrosshairOffset = 60.0F;
    private float crosshairMinOffset = 5.0F;
    private float crosshairMaxOffset = 80.0F;
    private IsoReticle.IsoReticleShader isoReticleShader;

    public static IsoReticle getInstance() {
        if (instance == null) {
            instance = new IsoReticle();
        }

        return instance;
    }

    private IsoReticle() {
        RenderThread.invokeOnRenderContext(this::createShader);
        if (this.isoReticleShader != null) {
            this.loadTextures();
        }
    }

    private void loadTextures() {
        for (int textureIndex = 0; textureIndex < 18; textureIndex++) {
            String formatted = "media/ui/Reticle/aimCircle" + String.format("%02d", textureIndex) + ".png";
            this.isoReticleShader.aimTexture[textureIndex] = Texture.getSharedTexture(formatted);
        }

        for (int textureIndex = 0; textureIndex < 7; textureIndex++) {
            String formatted = "media/ui/Reticle/targetReticle" + String.format("%02d", textureIndex) + ".png";
            this.isoReticleShader.targetTexture[textureIndex] = Texture.getSharedTexture(formatted);
        }

        for (int textureIndex = 0; textureIndex < 3; textureIndex++) {
            for (int textureSubIndex = 0; textureSubIndex < 4; textureSubIndex++) {
                String formatted = "media/ui/Reticle/crosshair" + String.format("%d%d", textureIndex, textureSubIndex) + ".png";
                this.isoReticleShader.crosshairTextures[textureIndex][textureSubIndex] = Texture.getSharedTexture(formatted);
            }
        }
    }

    private void createShader() {
        this.isoReticleShader = new IsoReticle.IsoReticleShader();
    }

    public void setAimColor(ColorInfo colorInfo) {
        this.isoReticleShader.aimColorInfo.set(colorInfo);
    }

    public void setReticleColor(ColorInfo colorInfo) {
        this.isoReticleShader.reticleColorInfo.set(colorInfo);
    }

    public void setChance(int chance) {
        this.targetChance = PZMath.clamp((float)chance, 0.0F, 100.0F);
    }

    public void setAimPenalty(int aimPenalty) {
        this.targetAimPenalty = PZMath.clamp((float)aimPenalty, 0.0F, 100.0F);
    }

    public void hasTarget(boolean hasValidTarget) {
        this.hasValidTarget = hasValidTarget;
    }

    public void render(int playerIndex) {
        if (Core.getInstance().displayCursor) {
            if (Core.getInstance().getOffscreenBuffer() != null) {
                IsoPlayer player = IsoPlayer.players[playerIndex];
                if (player != null && !player.isDead() && player.isAiming() && player.playerIndex == 0 && player.joypadBind == -1) {
                    if (!GameTime.isGamePaused()) {
                        if (this.isoReticleShader != null && this.isoReticleShader.isCompiled()) {
                            float reticalZoom = Core.getInstance().getOptionReticleCameraZoom() ? 1.0F / Core.getInstance().getZoom(playerIndex) : 1.0F;
                            int aimTextureIndex = Core.getInstance().getOptionAimTextureIndex();
                            int width = 16;
                            int height = 16;
                            if (this.isoReticleShader.aimTexture.length > aimTextureIndex && this.isoReticleShader.aimTexture[aimTextureIndex] != null) {
                                width = (int)(this.isoReticleShader.aimTexture[aimTextureIndex].getWidth() / 16.0F * reticalZoom);
                                height = (int)(this.isoReticleShader.aimTexture[aimTextureIndex].getHeight() / 16.0F * reticalZoom);
                            }

                            this.isoReticleShader.screenX = (int)(Mouse.getXA() - width / 2.0F);
                            this.isoReticleShader.screenY = (int)(Mouse.getYA() - height / 2.0F);
                            this.isoReticleShader.setWidth(width);
                            this.isoReticleShader.setHeight(height);
                            int screenX = IsoCamera.getScreenLeft(playerIndex);
                            int screenY = IsoCamera.getScreenTop(playerIndex);
                            int screenW = IsoCamera.getScreenWidth(playerIndex);
                            int screenH = IsoCamera.getScreenHeight(playerIndex);
                            this.crosshairMaxOffset = screenW * (Core.getInstance().getOptionMaxCrosshairOffset() / 100.0F);
                            switch (Core.getInstance().getOptionReticleMode()) {
                                case 0:
                                default:
                                    float chanceDelta = 1.0F - this.targetChance / 100.0F;
                                    this.targetCrosshairOffset = this.crosshairMinOffset + (this.crosshairMaxOffset - this.crosshairMinOffset) * chanceDelta;
                                    break;
                                case 1:
                                    float aimPenaltyDelta = this.targetAimPenalty / 20.0F;
                                    this.targetCrosshairOffset = this.crosshairMinOffset
                                        + (this.crosshairMaxOffset - this.crosshairMinOffset) * aimPenaltyDelta;
                            }

                            this.currentCrosshairOffset = PZMath.lerp(this.currentCrosshairOffset, this.targetCrosshairOffset, 0.1F);
                            IndieGL.glBlendFunc(770, 771);
                            int crosshairTextureIndex = Core.getInstance().getOptionCrosshairTextureIndex();
                            SpriteRenderer.instance
                                .renderClamped(
                                    this.isoReticleShader.crosshairTextures[crosshairTextureIndex][0],
                                    this.isoReticleShader.screenX - (int)this.currentCrosshairOffset,
                                    this.isoReticleShader.screenY,
                                    width,
                                    height,
                                    screenX,
                                    screenY,
                                    screenW,
                                    screenH,
                                    1.0F,
                                    1.0F,
                                    1.0F,
                                    this.isoReticleShader.alpha,
                                    null
                                );
                            SpriteRenderer.instance
                                .renderClamped(
                                    this.isoReticleShader.crosshairTextures[crosshairTextureIndex][2],
                                    this.isoReticleShader.screenX + (int)this.currentCrosshairOffset,
                                    this.isoReticleShader.screenY,
                                    width,
                                    height,
                                    screenX,
                                    screenY,
                                    screenW,
                                    screenH,
                                    1.0F,
                                    1.0F,
                                    1.0F,
                                    this.isoReticleShader.alpha,
                                    null
                                );
                            SpriteRenderer.instance
                                .renderClamped(
                                    this.isoReticleShader.crosshairTextures[crosshairTextureIndex][1],
                                    this.isoReticleShader.screenX,
                                    this.isoReticleShader.screenY - (int)this.currentCrosshairOffset,
                                    width,
                                    height,
                                    screenX,
                                    screenY,
                                    screenW,
                                    screenH,
                                    1.0F,
                                    1.0F,
                                    1.0F,
                                    this.isoReticleShader.alpha,
                                    null
                                );
                            SpriteRenderer.instance
                                .renderClamped(
                                    this.isoReticleShader.crosshairTextures[crosshairTextureIndex][3],
                                    this.isoReticleShader.screenX,
                                    this.isoReticleShader.screenY + (int)this.currentCrosshairOffset,
                                    width,
                                    height,
                                    screenX,
                                    screenY,
                                    screenW,
                                    screenH,
                                    1.0F,
                                    1.0F,
                                    1.0F,
                                    this.isoReticleShader.alpha,
                                    null
                                );
                            if (Core.getInstance().getOptionShowAimTexture()) {
                                SpriteRenderer.instance.StartShader(this.isoReticleShader.getID(), playerIndex);
                                SpriteRenderer.instance
                                    .renderClamped(
                                        this.isoReticleShader.aimTexture[Core.getInstance().getOptionAimTextureIndex()],
                                        this.isoReticleShader.screenX,
                                        this.isoReticleShader.screenY,
                                        width,
                                        height,
                                        screenX,
                                        screenY,
                                        screenW,
                                        screenH,
                                        this.isoReticleShader.aimColorInfo.r,
                                        this.isoReticleShader.aimColorInfo.g,
                                        this.isoReticleShader.aimColorInfo.b,
                                        this.isoReticleShader.alpha,
                                        this.isoReticleShader
                                    );
                                SpriteRenderer.instance.EndShader();
                            }

                            if (Core.getInstance().getOptionShowValidTargetReticleTexture() && this.hasValidTarget) {
                                SpriteRenderer.instance
                                    .renderClamped(
                                        this.isoReticleShader.targetTexture[Core.getInstance().getOptionValidTargetReticleTextureIndex()],
                                        this.isoReticleShader.screenX,
                                        this.isoReticleShader.screenY,
                                        width,
                                        height,
                                        screenX,
                                        screenY,
                                        screenW,
                                        screenH,
                                        this.isoReticleShader.reticleColorInfo.r,
                                        this.isoReticleShader.reticleColorInfo.g,
                                        this.isoReticleShader.reticleColorInfo.b,
                                        this.isoReticleShader.alpha,
                                        null
                                    );
                            }

                            if (Core.getInstance().getOptionShowReticleTexture()) {
                                SpriteRenderer.instance
                                    .renderClamped(
                                        this.isoReticleShader.targetTexture[Core.getInstance().getOptionReticleTextureIndex()],
                                        this.isoReticleShader.screenX,
                                        this.isoReticleShader.screenY,
                                        width,
                                        height,
                                        screenX,
                                        screenY,
                                        screenW,
                                        screenH,
                                        this.isoReticleShader.reticleColorInfo.r,
                                        this.isoReticleShader.reticleColorInfo.g,
                                        this.isoReticleShader.reticleColorInfo.b,
                                        this.isoReticleShader.alpha,
                                        null
                                    );
                            }

                            if (this.defaultColor) {
                                this.isoReticleShader.aimColorInfo.set(0.5F, 0.5F, 0.5F, this.isoReticleShader.alpha);
                                this.defaultColor = false;
                            }

                            this.targetChance = 0.0F;
                            this.targetAimPenalty = 100.0F;
                        }
                    }
                }
            }
        }
    }

    private static class IsoReticleShader extends Shader implements Consumer<TextureDraw> {
        private final int maxAimTextures = 18;
        private final int maxTargetTextures = 7;
        private final int maxCrosshairTextures = 3;
        private float alpha = 1.0F;
        private final Texture[] aimTexture = new Texture[18];
        private final Texture[] targetTexture = new Texture[7];
        private final Texture[][] crosshairTextures = new Texture[3][4];
        private Texture textureWorld;
        private int screenX;
        private int screenY;
        private final ColorInfo aimColorInfo = new ColorInfo();
        private final ColorInfo reticleColorInfo = new ColorInfo();

        IsoReticleShader() {
            super("isoreticle");
        }

        @Override
        public void startMainThread(TextureDraw texd, int playerIndex) {
            this.alpha = Core.getInstance().getIsoCursorAlpha();
            this.textureWorld = Core.getInstance().offscreenBuffer.getTexture(playerIndex);
        }

        @Override
        public void startRenderThread(TextureDraw texd) {
            int aimTextureIndex = Core.getInstance().getOptionAimTextureIndex();
            if (this.aimTexture.length > aimTextureIndex && this.aimTexture[aimTextureIndex] != null) {
                this.getProgram().setValue("aimTexture", this.aimTexture[aimTextureIndex], 0);
            }

            this.getProgram().setValue("red", this.aimColorInfo.r);
            this.getProgram().setValue("green", this.aimColorInfo.g);
            this.getProgram().setValue("blue", this.aimColorInfo.b);
            this.getProgram().setValue("alpha", this.alpha);
            SpriteRenderer.ringBuffer.shaderChangedTexture1();
            GL11.glEnable(3042);
        }

        public void accept(TextureDraw textureDraw) {
            int playerIndex = 0;
            int dx1 = (int)textureDraw.x0 - this.screenX;
            int dy1 = (int)textureDraw.y0 - this.screenY;
            int dx2 = this.screenX + this.getWidth() - (int)textureDraw.x2;
            int dy2 = this.screenY + this.getHeight() - (int)textureDraw.y2;
            this.screenX += dx1;
            this.screenY += dy1;
            this.setWidth(this.getWidth() - (dx1 + dx2));
            this.setHeight(this.getHeight() - (dy1 + dy2));
            float worldWidth = this.textureWorld.getWidthHW();
            float worldHeight = this.textureWorld.getHeightHW();
            float screenY = IsoCamera.getScreenTop(0) + IsoCamera.getScreenHeight(0) - (this.screenY + this.getHeight());
            textureDraw.tex1 = this.textureWorld;
            textureDraw.tex1U0 = this.screenX / worldWidth;
            textureDraw.tex1V3 = screenY / worldHeight;
            textureDraw.tex1U1 = (this.screenX + this.getWidth()) / worldWidth;
            textureDraw.tex1V2 = screenY / worldHeight;
            textureDraw.tex1U2 = (this.screenX + this.getWidth()) / worldWidth;
            textureDraw.tex1V1 = (screenY + this.getHeight()) / worldHeight;
            textureDraw.tex1U3 = this.screenX / worldWidth;
            textureDraw.tex1V0 = (screenY + this.getHeight()) / worldHeight;
        }
    }
}
