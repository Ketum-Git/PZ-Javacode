// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.obj;

import zombie.erosion.ErosionMain;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;

public final class ErosionObjOverlaySprites {
    public String name;
    public int stages;
    private final ErosionObjOverlaySprites.Stage[] sprites;

    public ErosionObjOverlaySprites(int _stages, String _name) {
        this.name = _name;
        this.stages = _stages;
        this.sprites = new ErosionObjOverlaySprites.Stage[this.stages];

        for (int i = 0; i < this.stages; i++) {
            this.sprites[i] = new ErosionObjOverlaySprites.Stage();
        }
    }

    public IsoSprite getSprite(int _stage, int _season) {
        return this.sprites[_stage].seasons[_season].getSprite();
    }

    public IsoSpriteInstance getSpriteInstance(int _stage, int _season) {
        return this.sprites[_stage].seasons[_season].getInstance();
    }

    public void setSprite(int _stage, String _sprite, int _season) {
        this.sprites[_stage].seasons[_season] = new ErosionObjOverlaySprites.Sprite(_sprite);
    }

    private static final class Sprite {
        private final String sprite;

        public Sprite(String _sprite) {
            this.sprite = _sprite;
        }

        public IsoSprite getSprite() {
            return this.sprite != null ? ErosionMain.getInstance().getSpriteManager().getSprite(this.sprite) : null;
        }

        public IsoSpriteInstance getInstance() {
            return this.sprite != null ? ErosionMain.getInstance().getSpriteManager().getSprite(this.sprite).newInstance() : null;
        }
    }

    private static class Stage {
        public ErosionObjOverlaySprites.Sprite[] seasons = new ErosionObjOverlaySprites.Sprite[6];
    }
}
