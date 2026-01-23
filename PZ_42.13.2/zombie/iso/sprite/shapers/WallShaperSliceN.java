// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;

public class WallShaperSliceN extends WallShaper {
    public static final WallShaperSliceN instance = new WallShaperSliceN();

    @Override
    public void accept(TextureDraw texd) {
        super.accept(texd);
        float SLICE = 5.0F;
        float FRAC = 5.0F / texd.tex.getWidthHW();
        texd.x1 = texd.x0 + 5.0F;
        texd.x2 = texd.x3 + 5.0F;
        texd.u1 = texd.u0 + FRAC;
        texd.u2 = texd.u3 + FRAC;
        if (texd.tex1 != null) {
            FRAC = 5.0F / texd.tex1.getWidthHW();
            texd.tex1U1 = texd.tex1U0 + FRAC;
            texd.tex1U2 = texd.tex1U3 + FRAC;
        }

        if (texd.tex2 != null) {
            FRAC = 5.0F / texd.tex2.getWidthHW();
            texd.tex2U1 = texd.tex2U0 + FRAC;
            texd.tex2U2 = texd.tex2U3 + FRAC;
        }

        WallPaddingShaper.instance.accept(texd);
    }
}
