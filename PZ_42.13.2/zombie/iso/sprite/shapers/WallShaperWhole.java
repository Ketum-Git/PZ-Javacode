// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;

public class WallShaperWhole extends WallShaper {
    public static final WallShaperWhole instance = new WallShaperWhole();

    @Override
    public void accept(TextureDraw texd) {
        super.accept(texd);
        WallPaddingShaper.instance.accept(texd);
    }
}
