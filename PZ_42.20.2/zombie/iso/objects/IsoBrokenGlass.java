// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCell;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSpriteManager;

@UsedFromLua
public class IsoBrokenGlass extends IsoObject {
    public IsoBrokenGlass(IsoCell cell) {
        super(cell);
        int randN = Rand.Next(4);
        this.sprite = IsoSpriteManager.instance.getSprite("brokenglass_1_" + randN);
    }

    @Override
    public String getObjectName() {
        return "IsoBrokenGlass";
    }

    @Override
    public void renderObjectPicker(float x, float y, float z, ColorInfo lightInfo) {
    }
}
