// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;

public class WallShaperSliceW extends WallShaper {
    public static final WallShaperSliceW instance = new WallShaperSliceW();

    @Override
    public void accept(TextureDraw texd) {
        super.accept(texd);
        float SLICE = 7.0F;
        float FRAC = 7.0F / texd.tex.getWidthHW();
        texd.x0 = texd.x1 - 7.0F;
        texd.x3 = texd.x2 - 7.0F;
        texd.u0 = texd.u1 - FRAC;
        texd.u3 = texd.u2 - FRAC;
        if (texd.tex1 != null) {
            FRAC = 7.0F / texd.tex1.getWidthHW();
            texd.tex1U0 = texd.tex1U1 - FRAC;
            texd.tex1U3 = texd.tex1U2 - FRAC;
        }

        if (texd.tex2 != null) {
            FRAC = 7.0F / texd.tex2.getWidthHW();
            texd.tex2U0 = texd.tex2U1 - FRAC;
            texd.tex2U3 = texd.tex2U2 - FRAC;
        }

        WallPaddingShaper.instance.accept(texd);
    }
}
