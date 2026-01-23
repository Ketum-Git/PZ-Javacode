// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import java.util.function.Consumer;
import zombie.core.PerformanceSettings;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.iso.fboRenderChunk.FBORenderLevels;

public class DiamondShaper implements Consumer<TextureDraw> {
    public static final DiamondShaper instance = new DiamondShaper();

    public void accept(TextureDraw ddraw) {
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.meshCutDown.getValue()) {
            float DX = 0.5F;
            float DY = 0.5F;
            float DU = 0.0F;
            float DV = 0.0F;
            if (PerformanceSettings.fboRenderChunk) {
                int textureScale = FBORenderLevels.getTextureScale(IsoCamera.frameState.zoom);
                DX = textureScale;
                DY = textureScale;
                DU = DX;
                DV = DY;
            }

            float x0 = ddraw.x0 - DX;
            float y0 = ddraw.y0 - DY;
            float x1 = ddraw.x1 + DX;
            float y1 = ddraw.y1 - DY;
            float y2 = ddraw.y2 + DY;
            float y3 = ddraw.y3 + DY;
            float width = x1 - x0;
            float height = y2 - y1;
            float xHalf = x0 + width * 0.5F;
            float yHalf = y1 + height * 0.5F;
            float onePixelU = 1.0F / ddraw.tex.getWidthHW();
            float onePixelV = 1.0F / ddraw.tex.getHeightHW();
            float u0 = ddraw.u0 - onePixelU * DU;
            float v0 = ddraw.v0 - onePixelV * DV;
            float u1 = ddraw.u1 + onePixelU * DU;
            float v1 = ddraw.v1 - onePixelV * DV;
            float v2 = ddraw.v2 + onePixelV * DV;
            float v3 = ddraw.v3 + onePixelV * DV;
            float uWidth = u1 - u0;
            float vHeight = v2 - v0;
            float uHalf = u0 + uWidth * 0.5F;
            float vHalf = v1 + vHeight * 0.5F;
            ddraw.x0 = xHalf;
            ddraw.y0 = y0;
            ddraw.u0 = uHalf;
            ddraw.v0 = v0;
            ddraw.x1 = x1;
            ddraw.y1 = yHalf;
            ddraw.u1 = u1;
            ddraw.v1 = vHalf;
            ddraw.x2 = xHalf;
            ddraw.y2 = y3;
            ddraw.u2 = uHalf;
            ddraw.v2 = v3;
            ddraw.x3 = x0;
            ddraw.y3 = yHalf;
            ddraw.u3 = u0;
            ddraw.v3 = vHalf;
            if (ddraw.tex1 != null) {
                onePixelU = 1.0F / ddraw.tex1.getWidthHW();
                onePixelV = 1.0F / ddraw.tex1.getHeightHW();
                u0 = ddraw.tex1U0 - onePixelU * DU;
                v0 = ddraw.tex1V0 - onePixelV * DV;
                u1 = ddraw.tex1U1 + onePixelU * DU;
                v1 = ddraw.tex1V1 - onePixelV * DV;
                v2 = ddraw.tex1V2 + onePixelV * DV;
                v3 = ddraw.tex1V3 + onePixelV * DV;
                uWidth = u1 - u0;
                vHeight = v2 - v0;
                uHalf = u0 + uWidth * 0.5F;
                vHalf = v1 + vHeight * 0.5F;
                ddraw.tex1U0 = uHalf;
                ddraw.tex1V0 = v0;
                ddraw.tex1U1 = u1;
                ddraw.tex1V1 = vHalf;
                ddraw.tex1U2 = uHalf;
                ddraw.tex1V2 = v3;
                ddraw.tex1U3 = u0;
                ddraw.tex1V3 = vHalf;
            }

            if (ddraw.tex2 != null) {
                onePixelU = ddraw.tex2U0;
                onePixelV = ddraw.tex2V0;
                u0 = ddraw.tex2U1;
                v0 = ddraw.tex2V1;
                u1 = ddraw.tex2V2;
                v1 = ddraw.tex2V3;
                v2 = u0 - onePixelU;
                v3 = u1 - onePixelV;
                uWidth = onePixelU + v2 * 0.5F;
                vHeight = v0 + v3 * 0.5F;
                ddraw.tex2U0 = uWidth;
                ddraw.tex2V0 = onePixelV;
                ddraw.tex2U1 = u0;
                ddraw.tex2V1 = vHeight;
                ddraw.tex2U2 = uWidth;
                ddraw.tex2V2 = v1;
                ddraw.tex2U3 = onePixelU;
                ddraw.tex2V3 = vHeight;
            }
        }
    }
}
