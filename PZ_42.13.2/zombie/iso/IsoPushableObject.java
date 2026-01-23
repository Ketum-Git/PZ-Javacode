// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.inventory.ItemContainer;
import zombie.iso.objects.IsoWheelieBin;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;

@UsedFromLua
public class IsoPushableObject extends IsoMovingObject {
    public int carryCapacity = 100;
    public float emptyWeight = 4.5F;
    public ArrayList<IsoPushableObject> connectList;
    public float ox;
    public float oy;

    public IsoPushableObject(IsoCell cell) {
        super(cell);
        this.getCell().getPushableObjectList().add(this);
    }

    public IsoPushableObject(IsoCell cell, int x, int y, int z) {
        super(cell, true);
        this.getCell().getPushableObjectList().add(this);
    }

    public IsoPushableObject(IsoCell cell, IsoGridSquare square, IsoSprite spr) {
        super(cell, square, spr, true);
        this.setX(square.getX() + 0.5F);
        this.setY(square.getY() + 0.5F);
        this.setZ(square.getZ());
        this.ox = this.getX();
        this.oy = this.getY();
        this.setNextX(this.getX());
        this.setNextY(this.getNextY());
        this.offsetX = 6 * Core.tileScale;
        this.offsetY = -30 * Core.tileScale;
        this.setWeight(6.0F);
        this.square = square;
        this.setCurrent(square);
        this.getCurrentSquare().getMovingObjects().add(this);
        this.collidable = true;
        this.solid = true;
        this.shootable = false;
        this.width = 0.5F;
        this.setAlphaAndTarget(0.0F);
        this.getCell().getPushableObjectList().add(this);
    }

    @Override
    public String getObjectName() {
        return "Pushable";
    }

    @Override
    public void update() {
        if (this.connectList != null) {
            Iterator<IsoPushableObject> it = this.connectList.iterator();
            float highestConnectedAlpha = 0.0F;

            while (it.hasNext()) {
                IsoPushableObject pushableObject = it.next();
                float pushableObjectAlpha = pushableObject.getAlpha();
                if (pushableObjectAlpha > highestConnectedAlpha) {
                    highestConnectedAlpha = pushableObjectAlpha;
                }
            }

            this.setAlphaAndTarget(highestConnectedAlpha);
        }

        super.update();
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        if (!(this instanceof IsoWheelieBin)) {
            this.sprite = IsoSpriteManager.instance.getSprite(input.getInt());
        }

        if (input.get() == 1) {
            this.container = new ItemContainer();
            this.container.load(input, WorldVersion);
        }
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        if (!(this instanceof IsoWheelieBin)) {
            output.putInt(this.sprite.id);
        }

        if (this.container != null) {
            output.put((byte)1);
            this.container.save(output);
        } else {
            output.put((byte)0);
        }
    }

    @Override
    public float getWeight(float x, float y) {
        if (this.container == null) {
            return this.emptyWeight;
        } else {
            float capacityDelta = this.container.getContentsWeight() / this.carryCapacity;
            if (capacityDelta < 0.0F) {
                capacityDelta = 0.0F;
            }

            return capacityDelta > 1.0F ? this.getWeight() * 8.0F : this.getWeight() * capacityDelta + this.emptyWeight;
        }
    }

    @Override
    public boolean Serialize() {
        return true;
    }

    @Override
    public void DoCollideNorS() {
        if (this.connectList == null) {
            super.DoCollideNorS();
        } else {
            for (IsoPushableObject p : this.connectList) {
                if (p != this) {
                    if (p.ox < this.ox) {
                        p.setNextX(this.getNextX() - 1.0F);
                        p.setX(this.getX() - 1.0F);
                    } else if (p.ox > this.ox) {
                        p.setNextX(this.getNextX() + 1.0F);
                        p.setX(this.getX() + 1.0F);
                    } else {
                        p.setNextX(this.getNextX());
                        p.setX(this.getX());
                    }

                    if (p.oy < this.oy) {
                        p.setNextY(this.getNextY() - 1.0F);
                        p.setY(this.getY() - 1.0F);
                    } else if (p.oy > this.oy) {
                        p.setNextY(this.getNextY() + 1.0F);
                        p.setY(this.getY() + 1.0F);
                    } else {
                        p.setNextY(this.getNextY());
                        p.setY(this.getY());
                    }

                    p.setImpulsex(this.getImpulsex());
                    p.setImpulsey(this.getImpulsey());
                }
            }
        }
    }

    @Override
    public void DoCollideWorE() {
        if (this.connectList == null) {
            super.DoCollideWorE();
        } else {
            for (IsoPushableObject p : this.connectList) {
                if (p != this) {
                    if (p.ox < this.ox) {
                        p.setNextX(this.getNextX() - 1.0F);
                        p.setX(this.getX() - 1.0F);
                    } else if (p.ox > this.ox) {
                        p.setNextX(this.getNextX() + 1.0F);
                        p.setX(this.getX() + 1.0F);
                    } else {
                        p.setNextX(this.getNextX());
                        p.setX(this.getX());
                    }

                    if (p.oy < this.oy) {
                        p.setNextY(this.getNextY() - 1.0F);
                        p.setY(this.getY() - 1.0F);
                    } else if (p.oy > this.oy) {
                        p.setNextY(this.getNextY() + 1.0F);
                        p.setY(this.getY() + 1.0F);
                    } else {
                        p.setNextY(this.getNextY());
                        p.setY(this.getY());
                    }

                    p.setImpulsex(this.getImpulsex());
                    p.setImpulsey(this.getImpulsey());
                }
            }
        }
    }
}
