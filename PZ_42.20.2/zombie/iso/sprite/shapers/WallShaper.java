// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import java.util.function.Consumer;
import zombie.core.Color;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;

public class WallShaper implements Consumer<TextureDraw> {
    public final int[] col = new int[4];
    protected int colTint;

    public void setTintColor(int tintABGR) {
        this.colTint = tintABGR;
    }

    public void accept(TextureDraw texd) {
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lighting.getValue()) {
            texd.col0 = Color.blendBGR(texd.col0, this.col[0]);
            texd.col1 = Color.blendBGR(texd.col1, this.col[1]);
            texd.col2 = Color.blendBGR(texd.col2, this.col[2]);
            texd.col3 = Color.blendBGR(texd.col3, this.col[3]);
        }

        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
            float alpha = Color.getAlphaChannelFromABGR(texd.col0);
            texd.col0 = Color.colorToABGR(1.0F, 1.0F, 1.0F, alpha);
            texd.col1 = texd.col0;
            texd.col2 = texd.col0;
            texd.col3 = texd.col0;
        }

        if (this.colTint != 0) {
            texd.col0 = Color.tintABGR(texd.col0, this.colTint);
            texd.col1 = Color.tintABGR(texd.col1, this.colTint);
            texd.col2 = Color.tintABGR(texd.col2, this.colTint);
            texd.col3 = Color.tintABGR(texd.col3, this.colTint);
        }
    }
}
