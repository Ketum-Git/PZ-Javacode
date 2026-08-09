// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;

public class WallShaperW extends WallShaper {
    public static final WallShaperW instance = new WallShaperW();

    @Override
    public void accept(TextureDraw texd) {
        super.accept(texd);
        texd.x1 = texd.x0 * 0.5F + texd.x1 * 0.5F;
        texd.x2 = texd.x2 * 0.5F + texd.x3 * 0.5F;
        texd.u1 = texd.u0 * 0.5F + texd.u1 * 0.5F;
        texd.u2 = texd.u2 * 0.5F + texd.u3 * 0.5F;
        if (texd.tex1 != null) {
            texd.tex1U1 = texd.tex1U0 * 0.5F + texd.tex1U1 * 0.5F;
            texd.tex1U2 = texd.tex1U2 * 0.5F + texd.tex1U3 * 0.5F;
        }

        if (texd.tex2 != null) {
            texd.tex2U1 = texd.tex2U0 * 0.5F + texd.tex2U1 * 0.5F;
            texd.tex2U2 = texd.tex2U2 * 0.5F + texd.tex2U3 * 0.5F;
        }

        WallPaddingShaper.instance.accept(texd);
    }
}
