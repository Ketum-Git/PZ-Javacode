// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.obj;

import java.util.ArrayList;
import zombie.erosion.ErosionMain;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoTree;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.list.PZArrayList;

public final class ErosionObj {
    private final ErosionObjSprites sprites;
    public String name;
    public int stages;
    public boolean hasSnow;
    public boolean hasFlower;
    public boolean hasChildSprite;
    public float bloomStart;
    public float bloomEnd;
    public boolean noSeasonBase;
    public int cycleTime;

    public ErosionObj(ErosionObjSprites sprites, int cycleTime, float bloomstart, float bloomend, boolean noSeasonBase) {
        this.sprites = sprites;
        this.name = sprites.name;
        this.stages = sprites.stages;
        this.hasSnow = sprites.hasSnow;
        this.hasFlower = sprites.hasFlower;
        this.hasChildSprite = sprites.hasChildSprite;
        this.bloomStart = bloomstart;
        this.bloomEnd = bloomend;
        this.noSeasonBase = noSeasonBase;
        this.cycleTime = cycleTime;
    }

    public IsoObject getObject(IsoGridSquare square, boolean remove) {
        PZArrayList<IsoObject> objs = square.getObjects();

        for (int i = objs.size() - 1; i >= 0; i--) {
            IsoObject obj = objs.get(i);
            if (this.name.equals(obj.getName())) {
                if (remove) {
                    objs.remove(i);
                }

                obj.doNotSync = true;
                return obj;
            }
        }

        return null;
    }

    public IsoObject createObject(IsoGridSquare square, int stage, boolean tree, int season) {
        String spriteName = this.sprites.getBase(stage, this.noSeasonBase ? 0 : season);
        if (spriteName == null) {
            spriteName = "";
        }

        IsoObject obj;
        if (tree) {
            obj = IsoTree.getNew();
            obj.sprite = IsoSpriteManager.instance.namedMap.get(spriteName);
            obj.square = square;
            obj.sx = 0.0F;
            ((IsoTree)obj).initTree();
        } else {
            obj = IsoObject.getNew(square, spriteName, this.name, false);
        }

        obj.setName(this.name);
        obj.doNotSync = true;
        return obj;
    }

    public boolean placeObject(IsoGridSquare square, int stage, boolean tree, int season, boolean bloom) {
        IsoObject obj = this.createObject(square, stage, tree, season);
        if (obj != null && this.setStageObject(stage, obj, season, bloom)) {
            obj.doNotSync = true;
            if (!tree) {
                square.getObjects().add(obj);
                obj.addToWorld();
            } else {
                square.AddTileObject(obj);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean setStageObject(int stage, IsoObject obj, int season, boolean bloom) {
        obj.doNotSync = true;
        if (stage >= 0 && stage < this.stages && obj != null) {
            String spriteName = this.sprites.getBase(stage, this.noSeasonBase ? 0 : season);
            if (spriteName == null) {
                obj.setSprite(this.getSprite(""));
                if (obj.attachedAnimSprite != null) {
                    obj.attachedAnimSprite.clear();
                }

                return true;
            } else {
                IsoSprite sprite = this.getSprite(spriteName);
                obj.setSprite(sprite);
                if (this.hasChildSprite || this.hasFlower) {
                    if (obj.attachedAnimSprite == null) {
                        obj.attachedAnimSprite = new ArrayList<>();
                    }

                    obj.attachedAnimSprite.clear();
                    if (this.hasChildSprite && season != 0) {
                        spriteName = this.sprites.getChildSprite(stage, season);
                        if (spriteName != null) {
                            sprite = this.getSprite(spriteName);
                            obj.attachedAnimSprite.add(sprite.newInstance());
                        }
                    }

                    if (this.hasFlower && bloom) {
                        spriteName = this.sprites.getFlower(stage);
                        if (spriteName != null) {
                            sprite = this.getSprite(spriteName);
                            obj.attachedAnimSprite.add(sprite.newInstance());
                        }
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public boolean setStage(IsoGridSquare square, int stage, int season, boolean bloom) {
        IsoObject obj = this.getObject(square, false);
        return obj != null ? this.setStageObject(stage, obj, season, bloom) : false;
    }

    public IsoObject removeObject(IsoGridSquare square) {
        return this.getObject(square, true);
    }

    private IsoSprite getSprite(String name) {
        return ErosionMain.getInstance().getSpriteManager().getSprite(name);
    }

    public ErosionObjSprites.Entry getEntry(String sprite) {
        return this.sprites.getEntry(sprite);
    }
}
