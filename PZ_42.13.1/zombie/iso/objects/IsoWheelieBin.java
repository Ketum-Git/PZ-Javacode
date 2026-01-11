// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoPushableObject;

@UsedFromLua
public class IsoWheelieBin extends IsoPushableObject {
    float velx;
    float vely;

    @Override
    public String getObjectName() {
        return "WheelieBin";
    }

    public IsoWheelieBin(IsoCell cell) {
        super(cell);
        this.container = new ItemContainer("wheeliebin", this.square, this);
        this.collidable = true;
        this.solid = true;
        this.shootable = false;
        this.width = 0.3F;
        this.dir = IsoDirections.E;
        this.setAlphaAndTarget(0.0F);
        this.offsetX = -26.0F;
        this.offsetY = -248.0F;
        this.outlineOnMouseover = true;
        this.sprite.LoadFramesPageSimple("TileObjectsExt_7", "TileObjectsExt_5", "TileObjectsExt_6", "TileObjectsExt_8");
    }

    public IsoWheelieBin(IsoCell cell, int x, int y, int z) {
        super(cell, x, y, z);
        this.setX(x + 0.5F);
        this.setY(y + 0.5F);
        this.setZ(z);
        this.setNextX(this.getX());
        this.setNextY(this.getY());
        this.offsetX = -26.0F;
        this.offsetY = -248.0F;
        this.weight = 6.0F;
        this.sprite.LoadFramesPageSimple("TileObjectsExt_7", "TileObjectsExt_5", "TileObjectsExt_6", "TileObjectsExt_8");
        this.square = this.getCell().getGridSquare(x, y, z);
        this.current = this.getCell().getGridSquare(x, y, z);
        this.container = new ItemContainer("wheeliebin", this.square, this);
        this.collidable = true;
        this.solid = true;
        this.shootable = false;
        this.width = 0.3F;
        this.dir = IsoDirections.E;
        this.setAlphaAndTarget(0.0F);
        this.outlineOnMouseover = true;
    }

    @Override
    public void update() {
        this.velx = this.getX() - this.getLastX();
        this.vely = this.getY() - this.getLastY();
        float capacityDelta = 1.0F - this.container.getContentsWeight() / 500.0F;
        if (capacityDelta < 0.0F) {
            capacityDelta = 0.0F;
        }

        if (capacityDelta < 0.7F) {
            capacityDelta *= capacityDelta;
        }

        if (IsoPlayer.getInstance() != null && IsoPlayer.getInstance().getDragObject() != this) {
            if (this.velx != 0.0F && this.vely == 0.0F && (this.dir == IsoDirections.E || this.dir == IsoDirections.W)) {
                this.setNextX(this.getNextX() + this.velx * 0.65F * capacityDelta);
            }

            if (this.vely != 0.0F && this.velx == 0.0F && (this.dir == IsoDirections.N || this.dir == IsoDirections.S)) {
                this.setNextY(this.getNextY() + this.vely * 0.65F * capacityDelta);
            }
        }

        super.update();
    }

    @Override
    public float getWeight(float x, float y) {
        float capacityDelta = this.container.getContentsWeight() / 500.0F;
        if (capacityDelta < 0.0F) {
            capacityDelta = 0.0F;
        }

        if (capacityDelta > 1.0F) {
            return this.getWeight() * 8.0F;
        } else {
            float weight = this.getWeight() * capacityDelta + 1.5F;
            if (this.dir != IsoDirections.W && (this.dir != IsoDirections.E || y != 0.0F)) {
                return this.dir != IsoDirections.N && (this.dir != IsoDirections.S || x != 0.0F) ? weight * 3.0F : weight / 2.0F;
            } else {
                return weight / 2.0F;
            }
        }
    }
}
