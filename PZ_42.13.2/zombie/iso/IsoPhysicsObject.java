// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.network.GameServer;

public class IsoPhysicsObject extends IsoMovingObject {
    public float velX;
    public float velY;
    public float velZ;
    public float terminalVelocity = -0.05F;
    protected float speedMod = 1.0F;

    public IsoPhysicsObject(IsoCell cell) {
        super(cell);
        this.solid = false;
        this.shootable = false;
    }

    public void collideGround() {
    }

    public void collideWall() {
    }

    @Override
    public void update() {
        IsoGridSquare sq = this.getCurrentSquare();
        super.update();
        if (this.isCollidedThisFrame()) {
            if (this.isCollidedN() || this.isCollidedS()) {
                this.velY = -this.velY;
                this.velY *= 0.5F;
                this.collideWall();
            }

            if (this.isCollidedE() || this.isCollidedW()) {
                this.velX = -this.velX;
                this.velX *= 0.5F;
                this.collideWall();
            }
        }

        int fps = GameServer.server ? 10 : PerformanceSettings.getLockFPS();
        float fpsMod = 30.0F / fps;
        this.speedMod = 1.0F;
        float remove = 0.1F * this.speedMod * fpsMod;
        remove = 1.0F - remove;
        this.velX *= remove;
        this.velY *= remove;
        this.velZ -= 0.005F * fpsMod;
        if (this.velZ < this.terminalVelocity) {
            this.velZ = this.terminalVelocity;
        }

        this.setNextX(this.getNextX() + this.velX * this.speedMod * 0.3F * fpsMod);
        this.setNextY(this.getNextY() + this.velY * this.speedMod * 0.3F * fpsMod);
        float lastZ = this.getZ();
        this.setZ(this.getZ() + this.velZ * 0.4F * fpsMod);
        if (this.getZ() < 0.0F) {
            this.setZ(0.0F);
            this.velZ = -this.velZ * 0.5F;
            this.collideGround();
        }

        if (this.getCurrentSquare() != null
            && PZMath.fastfloor(this.getZ()) < PZMath.fastfloor(lastZ)
            && (sq != null && sq.TreatAsSolidFloor() || this.getCurrentSquare().TreatAsSolidFloor())) {
            this.setZ(PZMath.fastfloor(lastZ));
            this.velZ = -this.velZ * 0.5F;
            this.collideGround();
        }

        if (Math.abs(this.velX) < 1.0E-4F) {
            this.velX = 0.0F;
        }

        if (Math.abs(this.velY) < 1.0E-4F) {
            this.velY = 0.0F;
        }

        if (this.velX + this.velY == 0.0F) {
            this.sprite.animate = false;
        }

        this.sx = this.sy = 0.0F;
    }

    @Override
    public float getGlobalMovementMod(boolean bDoNoises) {
        return 1.0F;
    }
}
