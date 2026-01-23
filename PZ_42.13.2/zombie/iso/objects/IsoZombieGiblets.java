// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import zombie.GameTime;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCell;
import zombie.iso.IsoPhysicsObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSpriteInstance;

@UsedFromLua
public class IsoZombieGiblets extends IsoPhysicsObject {
    public float tintb = 1.0F;
    public float tintg = 1.0F;
    public float tintr = 1.0F;
    public float time;
    boolean invis;

    public IsoZombieGiblets(IsoCell cell) {
        super(cell);
    }

    @Override
    public boolean Serialize() {
        return false;
    }

    @Override
    public String getObjectName() {
        return "ZombieGiblets";
    }

    @Override
    public void update() {
        if (Rand.Next(Rand.AdjustForFramerate(12)) == 0
            && this.getZ() > PZMath.fastfloor(this.getZ())
            && this.getCurrentSquare() != null
            && this.getCurrentSquare().getChunk() != null) {
            this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), PZMath.fastfloor(this.getZ()), Rand.Next(8));
        }

        if (Core.lastStand
            && Rand.Next(Rand.AdjustForFramerate(15)) == 0
            && this.getZ() > PZMath.fastfloor(this.getZ())
            && this.getCurrentSquare() != null
            && this.getCurrentSquare().getChunk() != null) {
            this.getCurrentSquare().getChunk().addBloodSplat(this.getX(), this.getY(), PZMath.fastfloor(this.getZ()), Rand.Next(8));
        }

        super.update();
        this.time = this.time + GameTime.instance.getMultipliedSecondsSinceLastUpdate();
        if (this.velX == 0.0F && this.velY == 0.0F && this.getZ() == PZMath.fastfloor(this.getZ())) {
            this.setCollidable(false);
            IsoWorld.instance.currentCell.getRemoveList().add(this);
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        if (!this.invis) {
            float or = info.r;
            float og = info.g;
            float ob = info.b;
            info.r = 0.5F;
            info.g = 0.5F;
            info.b = 0.5F;
            float alpha = 1.0F - PZMath.clamp(this.time, 0.0F, 1.0F);
            this.def.targetAlpha = alpha;
            this.sprite.def.targetAlpha = alpha;
            this.setTargetAlpha(alpha);
            IndieGL.glBlendFunc(770, 771);
            super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
            if (Core.debug) {
            }

            info.r = or;
            info.g = og;
            info.b = ob;
        }
    }

    public IsoZombieGiblets(IsoZombieGiblets.GibletType type, IsoCell cell, float x, float y, float z, float xvel, float yvel) {
        super(cell);
        this.velX = xvel;
        this.velY = yvel;
        float randX = Rand.Next(4000) / 10000.0F;
        float randY = Rand.Next(4000) / 10000.0F;
        randX -= 0.2F;
        randY -= 0.2F;
        this.velX += randX;
        this.velY += randY;
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setNextX(x);
        this.setNextY(y);
        this.setAlpha(0.2F);
        this.def = IsoSpriteInstance.get(this.sprite);
        this.def.alpha = 0.2F;
        this.sprite.def.alpha = 0.4F;
        this.offsetX = 0.0F;
        this.offsetY = 0.0F;
        if (Rand.Next(3) != 0) {
            this.def.alpha = 0.0F;
            this.sprite.def.alpha = 0.0F;
            this.invis = true;
        }

        switch (type) {
            case A:
                this.sprite.setFromCache("Giblet", "00", 3);
                break;
            case B:
                this.sprite.setFromCache("Giblet", "01", 3);
                break;
            case Eye:
                this.sprite.setFromCache("Eyeball", "00", 1);
        }
    }

    public static enum GibletType {
        A,
        B,
        Eye;
    }
}
