// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import zombie.Lua.LuaEventManager;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugType;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoPhysicsObject;

public class IsoFallingClothing extends IsoPhysicsObject {
    public static final float DEFAULT_VELOCITY = 0.2F;
    public static final float DEFAULT_Z_OFFSET = 0.4F;
    private InventoryItem clothing;
    private int dropTimer;
    public boolean addWorldItem = true;
    public float targetX;
    public float targetY;
    public float targetZ;

    @Override
    public String getObjectName() {
        return "FallingClothing";
    }

    public IsoFallingClothing(IsoCell cell) {
        super(cell);
    }

    public IsoFallingClothing(IsoCell cell, float x, float y, float z, float xvel, float yvel, InventoryItem clothing) {
        super(cell);
        this.clothing = clothing;
        this.dropTimer = 60;
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
        this.offsetX = 0.0F;
        this.offsetY = 0.0F;
        this.terminalVelocity = -0.02F;
        Texture tex = this.sprite.LoadFrameExplicit(clothing.getTex().getName());
        if (tex != null) {
            this.sprite.animate = false;
            int scale = Core.tileScale;
            this.sprite.def.scaleAspect(tex.getWidthOrig(), tex.getHeightOrig(), 16 * scale, 16 * scale);
        }

        this.speedMod = 4.5F;
    }

    @Override
    public void collideGround() {
        this.drop();
    }

    @Override
    public void collideWall() {
        this.drop();
    }

    @Override
    public void update() {
        super.update();
        if (this.targetX != 0.0F) {
            float a = 1.0F - Math.min(1.0F, this.dropTimer / 60.0F);
            this.setNextX(PZMath.lerp(this.getNextX(), this.targetX, a));
            this.setNextY(PZMath.lerp(this.getNextY(), this.targetY, a));
            this.setZ(PZMath.lerp(this.getZ(), this.targetZ, a));
        }

        this.dropTimer--;
        if (this.dropTimer <= 0) {
            this.drop();
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo info, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        float flipAngle = (60 - this.dropTimer) / 60.0F * 360.0F;
        ItemModelRenderer.RenderStatus status = WorldItemModelDrawer.renderMain(
            this.clothing, this.getCurrentSquare(), this.getRenderSquare(), this.getX(), this.getY(), this.getZ(), flipAngle
        );
        if (status != ItemModelRenderer.RenderStatus.Loading && status != ItemModelRenderer.RenderStatus.Ready) {
            super.render(x, y, z, info, bDoAttached, bWallLightingPass, shader);
        }
    }

    void drop() {
        if (this.targetX != 0.0F) {
            this.setX(this.targetX);
            this.setY(this.targetY);
            this.setZ(this.targetZ);
        }

        float x = this.getX();
        float y = this.getY();
        float z = this.getZ();
        DebugType.General.println("IsoFallingClothing added x=" + x + " y=" + y);
        float apparentZ = this.getCurrentSquare().getApparentZ(x % 1.0F, y % 1.0F);
        IsoGridSquare square = this.getCell().getGridSquare((double)x, (double)y, (double)apparentZ);
        if (square != null && this.clothing != null) {
            if (this.addWorldItem) {
                square.AddWorldInventoryItem(this.clothing, x % 1.0F, y % 1.0F, apparentZ - square.getZ(), false);
            }

            this.clothing = null;
            this.setDestroyed(true);
            this.getCurrentSquare().getMovingObjects().remove(this);
            this.getCell().Remove(this);
            LuaEventManager.triggerEvent("OnContainerUpdate", square);
        }
    }
}
