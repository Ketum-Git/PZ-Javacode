// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SceneShaderStore;
import zombie.core.SpriteRenderer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.tileDepth.CutawayAttachedModifier;
import zombie.tileDepth.TileDepthModifier;

public final class IsoWallBloodSplat {
    private static final ColorInfo info = new ColorInfo();
    public float worldAge;
    public IsoSprite sprite;

    public IsoWallBloodSplat() {
    }

    public IsoWallBloodSplat(float worldAge, IsoSprite sprite) {
        this.worldAge = worldAge;
        this.sprite = sprite;
    }

    public void render(float x, float y, float z, ColorInfo objectColor, Consumer<TextureDraw> texdModifier) {
        if (this.sprite != null) {
            if (!this.sprite.hasNoTextures()) {
                int SCL = Core.tileScale;
                int offsetX = 32 * SCL;
                int offsetY = 96 * SCL;
                if (IsoSprite.globalOffsetX == -1.0F) {
                    IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
                    IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
                }

                float goX = IsoSprite.globalOffsetX;
                float goY = IsoSprite.globalOffsetY;
                if (FBORenderChunkManager.instance.isCaching()) {
                    goX = FBORenderChunkManager.instance.getXOffset();
                    goY = FBORenderChunkManager.instance.getYOffset();
                    x -= FBORenderChunkManager.instance.renderChunk.chunk.wx * 8;
                    y -= FBORenderChunkManager.instance.renderChunk.chunk.wy * 8;
                }

                float sx = IsoUtils.XToScreen(x, y, z, 0);
                float sy = IsoUtils.YToScreen(x, y, z, 0);
                sx -= offsetX;
                sy -= offsetY;
                sx += goX;
                sy += goY;
                if (!PerformanceSettings.fboRenderChunk) {
                    if (sx >= IsoCamera.frameState.offscreenWidth || sx + 64 * SCL <= 0.0F) {
                        return;
                    }

                    if (sy >= IsoCamera.frameState.offscreenHeight || sy + 128 * SCL <= 0.0F) {
                        return;
                    }
                }

                info.r = 0.7F * objectColor.r;
                info.g = 0.9F * objectColor.g;
                info.b = 0.9F * objectColor.b;
                info.a = 0.4F;
                float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
                float deltaAge = worldAge - this.worldAge;
                if (deltaAge >= 0.0F && deltaAge < 72.0F) {
                    float f = 1.0F - deltaAge / 72.0F;
                    info.r *= 0.2F + f * 0.8F;
                    info.g *= 0.2F + f * 0.8F;
                    info.b *= 0.2F + f * 0.8F;
                    info.a *= 0.25F + f * 0.75F;
                } else {
                    info.r *= 0.2F;
                    info.g *= 0.2F;
                    info.b *= 0.2F;
                    info.a *= 0.25F;
                }

                info.a = Math.max(info.a, 0.15F);
                if (texdModifier == CutawayAttachedModifier.instance) {
                    if (PerformanceSettings.fboRenderChunk) {
                        IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
                    }

                    this.sprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, info, false, texdModifier);
                    if (PerformanceSettings.fboRenderChunk) {
                        IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
                    }
                } else {
                    if (PerformanceSettings.fboRenderChunk) {
                        SpriteRenderer.instance.StartShader(SceneShaderStore.defaultShaderId, IsoCamera.frameState.playerIndex);
                        IndieGL.enableDepthTest();
                        IndieGL.glDepthMask(false);
                        IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
                    }

                    this.sprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, info, false, TileDepthModifier.instance);
                    if (PerformanceSettings.fboRenderChunk) {
                        IndieGL.glBlendFuncSeparate(1, 771, 773, 1);
                    }
                }
            }
        }
    }

    public void save(ByteBuffer output) {
        output.putFloat(this.worldAge);
        output.putInt(this.sprite.id);
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.worldAge = input.getFloat();
        int spriteID = input.getInt();
        this.sprite = IsoSprite.getSprite(IsoSpriteManager.instance, spriteID);
    }
}
