// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.core.opengl.Shader;
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
    public void load(ByteBuffer bb, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(bb, WorldVersion, IS_DEBUG_SAVE);
    }

    @Override
    public void save(ByteBuffer bb, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(bb, IS_DEBUG_SAVE);
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        super.render(x, y, z, col, bDoChild, bWallLightingPass, shader);
    }

    @Override
    public void renderObjectPicker(float x, float y, float z, ColorInfo lightInfo) {
    }
}
