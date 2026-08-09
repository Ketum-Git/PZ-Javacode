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

    public ErosionObjOverlay(ErosionObjOverlaySprites sprites, int cycleTime, boolean applyAlpha) {
        this.sprites = sprites;
        this.name = sprites.name;
        this.stages = sprites.stages;
        this.applyAlpha = applyAlpha;
        this.cycleTime = cycleTime;
    }

    public int setOverlay(IsoObject obj, int curId, int stage, int season, float alpha) {
        if (stage >= 0 && stage < this.stages && obj != null) {
            if (curId >= 0) {
                this.removeOverlay(obj, curId);
            }

            IsoSprite overlay = this.sprites.getSprite(stage, season);
            IsoSpriteInstance overlayInst = overlay.newInstance();
            if (obj.attachedAnimSprite == null) {
                obj.attachedAnimSprite = new ArrayList<>();
            }

            obj.attachedAnimSprite.add(overlayInst);
            return overlayInst.getID();
        } else {
            return -1;
        }
    }

    public boolean removeOverlay(IsoObject obj, int id) {
        if (obj == null) {
            return false;
        } else {
            ArrayList<IsoSpriteInstance> sprList = obj.attachedAnimSprite;
            if (sprList != null && !sprList.isEmpty()) {
                for (int j = 0; j < obj.attachedAnimSprite.size(); j++) {
                    if (obj.attachedAnimSprite.get(j).parentSprite.id == id) {
                        obj.attachedAnimSprite.remove(j--);
                    }
                }

                for (int i = sprList.size() - 1; i >= 0; i--) {
                    if (sprList.get(i).getID() == id) {
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
