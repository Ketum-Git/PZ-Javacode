// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.obj;

import java.util.ArrayList;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;

public final class ErosionObjOverlay {
    private final ErosionObjOverlaySprites sprites;
    public String name;
    public int stages;
    public boolean applyAlpha;
    public int cycleTime;

    public ErosionObjOverlay(ErosionObjOverlaySprites _sprites, int _cycleTime, boolean _applyAlpha) {
        this.sprites = _sprites;
        this.name = _sprites.name;
        this.stages = _sprites.stages;
        this.applyAlpha = _applyAlpha;
        this.cycleTime = _cycleTime;
    }

    public int setOverlay(IsoObject _obj, int _curID, int _stage, int _season, float _alpha) {
        if (_stage >= 0 && _stage < this.stages && _obj != null) {
            if (_curID >= 0) {
                this.removeOverlay(_obj, _curID);
            }

            IsoSprite overlay = this.sprites.getSprite(_stage, _season);
            IsoSpriteInstance overlayInst = overlay.newInstance();
            if (_obj.attachedAnimSprite == null) {
                _obj.attachedAnimSprite = new ArrayList<>();
            }

            _obj.attachedAnimSprite.add(overlayInst);
            return overlayInst.getID();
        } else {
            return -1;
        }
    }

    public boolean removeOverlay(IsoObject _obj, int _id) {
        if (_obj == null) {
            return false;
        } else {
            ArrayList<IsoSpriteInstance> sprList = _obj.attachedAnimSprite;
            if (sprList != null && !sprList.isEmpty()) {
                for (int j = 0; j < _obj.attachedAnimSprite.size(); j++) {
                    if (_obj.attachedAnimSprite.get(j).parentSprite.id == _id) {
                        _obj.attachedAnimSprite.remove(j--);
                    }
                }

                for (int i = sprList.size() - 1; i >= 0; i--) {
                    if (sprList.get(i).getID() == _id) {
                        sprList.remove(i);
                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        }
    }
}
