// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import zombie.GameTime;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoPhysicsObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.sprite.IsoSpriteInstance;

public class IsoBloodDrop extends IsoPhysicsObject {
    public float tintb = 1.0F;
    public float tintg = 1.0F;
    public float tintr = 1.0F;
    public float time;
    float sx;
    float sy;
    float lsx;
    float lsy;
    static Vector2 temp = new Vector2();

    public IsoBloodDrop(IsoCell cell) {
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
    public void collideGround() {
        float lx = this.getX() - PZMath.fastfloor(this.getX());
        float ly = this.getY() - PZMath.fastfloor(this.getY());
        IsoGridSquare sq = IsoWorld.instance
            .currentCell
            .getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
        if (sq != null) {
            IsoObject floor = sq.getFloor();
            floor.addChild(this);
            this.setCollidable(false);
            IsoWorld.instance.currentCell.getRemoveList().add(this);
        }
    }

    @Override
    public void collideWall() {
        IsoGridSquare sq = IsoWorld.instance
            .currentCell
            .getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
        if (sq != null) {
            IsoObject floor = null;
            if (this.isCollidedN()) {
                floor = sq.getWall(true);
            } else if (this.isCollidedS()) {
                sq = IsoWorld.instance
                    .currentCell
                    .getGridSquare(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()) + 1, PZMath.fastfloor(this.getZ()));
                if (sq != null) {
                    floor = sq.getWall(true);
                }
            } else if (this.isCollidedW()) {
                floor = sq.getWall(false);
            } else if (this.isCollidedE()) {
                sq = IsoWorld.instance
                    .currentCell
                    .getGridSquare(PZMath.fastfloor(this.getX()) + 1, PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()));
                if (sq != null) {
                    floor = sq.getWall(false);
                }
            }

            if (floor != null) {
                floor.addChild(this);
                this.setCollidable(false);
                IsoWorld.instance.currentCell.getRemoveList().add(this);
            }
        }
    }

    @Override
    public void update() {
        super.update();
        this.time = this.time + GameTime.instance.getMultipliedSecondsSinceLastUpdate();
        if (this.velX == 0.0F && this.velY == 0.0F && this.getZ() == PZMath.fastfloor(this.getZ())) {
            this.setCollidable(false);
            IsoWorld.instance.currentCell.getRemoveList().add(this);
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        this.setTargetAlpha(0.3F);
        this.sprite.render(this, x, y, z, this.dir, this.offsetX, this.offsetY, info, true);
    }

    public IsoBloodDrop(IsoCell cell, float x, float y, float z, float xvel, float yvel) {
        super(cell);
        this.velX = xvel * 2.0F;
        this.velY = yvel * 2.0F;
        this.terminalVelocity = -0.1F;
        this.velZ = this.velZ + (Rand.Next(10000) / 10000.0F - 0.5F) * 0.05F;
        float res = Rand.Next(9000) / 10000.0F;
        res += 0.1F;
        this.velX *= res;
        this.velY *= res;
        this.velZ += res * 0.05F;
        if (Rand.Next(7) == 0) {
            this.velX *= 2.0F;
            this.velY *= 2.0F;
        }

        this.velX *= 0.8F;
        this.velY *= 0.8F;
        temp.x = this.velX;
        temp.y = this.velY;
        temp.rotate((Rand.Next(1000) / 1000.0F - 0.5F) * 0.07F);
        if (Rand.Next(3) == 0) {
            temp.rotate((Rand.Next(1000) / 1000.0F - 0.5F) * 0.1F);
        }

        if (Rand.Next(5) == 0) {
            temp.rotate((Rand.Next(1000) / 1000.0F - 0.5F) * 0.2F);
        }

        if (Rand.Next(8) == 0) {
            temp.rotate((Rand.Next(1000) / 1000.0F - 0.5F) * 0.3F);
        }

        if (Rand.Next(10) == 0) {
            temp.rotate((Rand.Next(1000) / 1000.0F - 0.5F) * 0.4F);
        }

        this.velX = temp.x;
        this.velY = temp.y;
        this.setX(x);
        this.setY(y);
        this.setZ(z);
        this.setNextX(x);
        this.setNextY(y);
        this.setAlpha(0.5F);
        this.def = IsoSpriteInstance.get(this.sprite);
        this.def.alpha = 0.4F;
        this.sprite.def.alpha = 0.4F;
        this.offsetX = -26.0F;
        this.offsetY = -242.0F;
        this.offsetX += 8.0F;
        this.offsetY += 9.0F;
        this.sprite.LoadFramesNoDirPageSimple("BloodSplat");
    }
}
