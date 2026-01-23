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
    public int cycleTime = 1;

    public ErosionObj(ErosionObjSprites _sprites, int _cycleTime, float _bloomstart, float _bloomend, boolean _noSeasonBase) {
        this.sprites = _sprites;
        this.name = _sprites.name;
        this.stages = _sprites.stages;
        this.hasSnow = _sprites.hasSnow;
        this.hasFlower = _sprites.hasFlower;
        this.hasChildSprite = _sprites.hasChildSprite;
        this.bloomStart = _bloomstart;
        this.bloomEnd = _bloomend;
        this.noSeasonBase = _noSeasonBase;
        this.cycleTime = _cycleTime;
    }

    public IsoObject getObject(IsoGridSquare _sq, boolean _bRemove) {
        PZArrayList<IsoObject> objs = _sq.getObjects();

        for (int i = objs.size() - 1; i >= 0; i--) {
            IsoObject obj = objs.get(i);
            if (this.name.equals(obj.getName())) {
                if (_bRemove) {
                    objs.remove(i);
                }

                obj.doNotSync = true;
                return obj;
            }
        }

        return null;
    }

    public IsoObject createObject(IsoGridSquare _sq, int _stage, boolean _bTree, int _season) {
        String spriteName = this.sprites.getBase(_stage, this.noSeasonBase ? 0 : _season);
        if (spriteName == null) {
            spriteName = "";
        }

        IsoObject obj;
        if (_bTree) {
            obj = IsoTree.getNew();
            obj.sprite = IsoSpriteManager.instance.namedMap.get(spriteName);
            obj.square = _sq;
            obj.sx = 0.0F;
            ((IsoTree)obj).initTree();
        } else {
            obj = IsoObject.getNew(_sq, spriteName, this.name, false);
        }

        obj.setName(this.name);
        obj.doNotSync = true;
        return obj;
    }

    public boolean placeObject(IsoGridSquare _sq, int _stage, boolean _bTree, int _season, boolean _bloom) {
        IsoObject obj = this.createObject(_sq, _stage, _bTree, _season);
        if (obj != null && this.setStageObject(_stage, obj, _season, _bloom)) {
            obj.doNotSync = true;
            if (!_bTree) {
                _sq.getObjects().add(obj);
                obj.addToWorld();
            } else {
                _sq.AddTileObject(obj);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean setStageObject(int _stage, IsoObject _object, int _season, boolean _bloom) {
        _object.doNotSync = true;
        if (_stage >= 0 && _stage < this.stages && _object != null) {
            String spriteName = this.sprites.getBase(_stage, this.noSeasonBase ? 0 : _season);
            if (spriteName == null) {
                _object.setSprite(this.getSprite(""));
                if (_object.attachedAnimSprite != null) {
                    _object.attachedAnimSprite.clear();
                }

                return true;
            } else {
                IsoSprite sprite = this.getSprite(spriteName);
                _object.setSprite(sprite);
                if (this.hasChildSprite || this.hasFlower) {
                    if (_object.attachedAnimSprite == null) {
                        _object.attachedAnimSprite = new ArrayList<>();
                    }

                    _object.attachedAnimSprite.clear();
                    if (this.hasChildSprite && _season != 0) {
                        spriteName = this.sprites.getChildSprite(_stage, _season);
                        if (spriteName != null) {
                            sprite = this.getSprite(spriteName);
                            _object.attachedAnimSprite.add(sprite.newInstance());
                        }
                    }

                    if (this.hasFlower && _bloom) {
                        spriteName = this.sprites.getFlower(_stage);
                        if (spriteName != null) {
                            sprite = this.getSprite(spriteName);
                            _object.attachedAnimSprite.add(sprite.newInstance());
                        }
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public boolean setStage(IsoGridSquare _sq, int _stage, int _season, boolean _bloom) {
        IsoObject obj = this.getObject(_sq, false);
        return obj != null ? this.setStageObject(_stage, obj, _season, _bloom) : false;
    }

    public IsoObject removeObject(IsoGridSquare _sq) {
        return this.getObject(_sq, true);
    }

    private IsoSprite getSprite(String name) {
        return ErosionMain.getInstance().getSpriteManager().getSprite(name);
    }

    public ErosionObjSprites.Entry getEntry(String _sprite) {
        return this.sprites.getEntry(_sprite);
    }
}
