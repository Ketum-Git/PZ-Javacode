// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;

public class WallShaperSliceN extends WallShaper {
    public static final WallShaperSliceN instance = new WallShaperSliceN();

    @Override
    public void accept(TextureDraw texd) {
        super.accept(texd);
        float slice = 5.0F;
        float frac = 5.0F / texd.tex.getWidthHW();
        texd.x1 = texd.x0 + 5.0F;
        texd.x2 = texd.x3 + 5.0F;
        texd.u1 = texd.u0 + frac;
        texd.u2 = texd.u3 + frac;
        if (texd.tex1 != null) {
            frac = 5.0F / texd.tex1.getWidthHW();
            texd.tex1U1 = texd.tex1U0 + frac;
            texd.tex1U2 = texd.tex1U3 + frac;
        }

        if (texd.tex2 != null) {
            frac = 5.0F / texd.tex2.getWidthHW();
            texd.tex2U1 = texd.tex2U0 + frac;
            texd.tex2U2 = texd.tex2U3 + frac;
        }

        WallPaddingShaper.instance.accept(texd);
    }
}
