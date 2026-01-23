// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import zombie.GameTime;
import zombie.core.opengl.Shader;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCell;
import zombie.iso.IsoMovingObject;
import zombie.iso.sprite.IsoSpriteInstance;

public class IsoZombieHead extends IsoMovingObject {
    public float tintb = 1.0F;
    public float tintg = 1.0F;
    public float tintr = 1.0F;
    public float time;

    public IsoZombieHead(IsoCell cell) {
        super(cell);
    }

    @Override
    public boolean Serialize() {
        return false;
    }

    @Override
    public String getObjectName() {
        return "ZombieHead";
    }

    @Override
    public void update() {
        super.update();
        this.time = this.time + GameTime.instance.getMultipliedSecondsSinceLastUpdate();
        this.sx = this.sy = 0.0F;
    }

    @Override
    public void render(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        this.setTargetAlpha(1.0F);
        super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
    }

    public IsoZombieHead(IsoZombieHead.GibletType type, IsoCell cell, float x, float y, float z) {
        super(cell);
        this.solid = false;
        this.shootable = false;
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setNextX(x);
        this.setNextY(y);
        this.setAlpha(0.5F);
        this.def = IsoSpriteInstance.get(this.sprite);
        this.def.alpha = 1.0F;
        this.sprite.def.alpha = 1.0F;
        this.offsetX = -26.0F;
        this.offsetY = -242.0F;
        switch (type) {
            case A:
                this.sprite.LoadFramesNoDirPageDirect("media/gibs/Giblet", "00", 3);
                break;
            case B:
                this.sprite.LoadFramesNoDirPageDirect("media/gibs/Giblet", "01", 3);
        }
    }

    public static enum GibletType {
        A,
        B,
        Eye;
    }
}
