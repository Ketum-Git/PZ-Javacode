// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;

public class FloorShaperDiamond extends FloorShaper {
    public static final FloorShaperDiamond instance = new FloorShaperDiamond();

    @Override
    public void accept(TextureDraw ddraw) {
        super.accept(ddraw);
        DiamondShaper.instance.accept(ddraw);
    }
}
