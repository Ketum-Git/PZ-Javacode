// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import java.util.function.Consumer;
import zombie.core.Color;
import zombie.core.PerformanceSettings;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;

public class FloorShaper implements Consumer<TextureDraw> {
    protected final int[] col = new int[4];
    protected int colTint;
    protected boolean isShore;
    protected final float[] waterDepth = new float[4];

    public void setVertColors(int col0, int col1, int col2, int col3) {
        this.col[0] = col0;
        this.col[1] = col1;
        this.col[2] = col2;
        this.col[3] = col3;
    }

    public void setAlpha4(float alpha) {
        int byteA = (int)(alpha * 255.0F) & 0xFF;
        this.col[0] = this.col[0] & 16777215 | byteA << 24;
        this.col[1] = this.col[1] & 16777215 | byteA << 24;
        this.col[2] = this.col[2] & 16777215 | byteA << 24;
        this.col[3] = this.col[3] & 16777215 | byteA << 24;
    }

    public void setShore(boolean isShore) {
        this.isShore = isShore;
    }

    public void setWaterDepth(float val0, float val1, float val2, float val3) {
        this.waterDepth[0] = val0;
        this.waterDepth[1] = val1;
        this.waterDepth[2] = val2;
        this.waterDepth[3] = val3;
    }

    public void setTintColor(int tintABGR) {
        this.colTint = tintABGR;
    }

    public void accept(TextureDraw ddraw) {
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lighting.getValue()) {
            ddraw.col0 = Color.blendBGR(ddraw.col0, this.col[0]);
            ddraw.col1 = Color.blendBGR(ddraw.col1, this.col[1]);
            ddraw.col2 = Color.blendBGR(ddraw.col2, this.col[2]);
            ddraw.col3 = Color.blendBGR(ddraw.col3, this.col[3]);
        }

        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            ddraw.col0 = -1;
            ddraw.col1 = -1;
            ddraw.col2 = -1;
            ddraw.col3 = -1;
        }

        if (this.isShore && DebugOptions.instance.terrain.renderTiles.isoGridSquare.shoreFade.getValue()) {
            ddraw.col0 = Color.setAlphaChannelToABGR(ddraw.col0, 1.0F - this.waterDepth[0]);
            ddraw.col1 = Color.setAlphaChannelToABGR(ddraw.col1, 1.0F - this.waterDepth[1]);
            ddraw.col2 = Color.setAlphaChannelToABGR(ddraw.col2, 1.0F - this.waterDepth[2]);
            ddraw.col3 = Color.setAlphaChannelToABGR(ddraw.col3, 1.0F - this.waterDepth[3]);
        }

        if (this.colTint != 0) {
            ddraw.col0 = Color.tintABGR(ddraw.col0, this.colTint);
            ddraw.col1 = Color.tintABGR(ddraw.col1, this.colTint);
            ddraw.col2 = Color.tintABGR(ddraw.col2, this.colTint);
            ddraw.col3 = Color.tintABGR(ddraw.col3, this.colTint);
        }

        if (!PerformanceSettings.fboRenderChunk) {
            SpritePadding.applyIsoPadding(ddraw, this.getIsoPaddingSettings());
        }
    }

    private SpritePadding.IsoPaddingSettings getIsoPaddingSettings() {
        return SpritePaddingSettings.getSettings().isoPadding;
    }
}
